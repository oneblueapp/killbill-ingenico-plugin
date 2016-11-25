/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.ingenico.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.*;
import org.killbill.billing.payment.plugin.api.*;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.plugin.ingenico.api.mapping.PaymentInfoMappingService;
import org.killbill.billing.plugin.ingenico.client.IngenicoClient;
import org.killbill.billing.plugin.ingenico.client.model.*;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.ingenico.core.IngenicoConfigurationHandler;
import org.killbill.billing.plugin.ingenico.dao.IngenicoDao;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoPaymentMethods;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoResponses;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.clock.Clock;
import org.osgi.service.log.LogService;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.killbill.billing.plugin.ingenico.api.mapping.UserDataMappingService.toUserData;

//
// A 'real' payment plugin would of course implement this interface.
//
public class IngenicoPaymentPluginApi extends PluginPaymentPluginApi<IngenicoResponsesRecord, IngenicoResponses, IngenicoPaymentMethodsRecord, IngenicoPaymentMethods> {

    private final IngenicoConfigurationHandler ingenicoConfigurationHandler;
    private final OSGIKillbillLogService logService;

    // Credit cards
    public static final String PROPERTY_CC_ISSUER_COUNTRY = "issuerCountry";

    // User data
    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_IP = "ip";
    public static final String PROPERTY_CUSTOMER_LOCALE = "customerLocale";
    public static final String PROPERTY_EMAIL = "email";

    private final IngenicoDao dao;

    public IngenicoPaymentPluginApi(final IngenicoConfigurationHandler ingenicoConfigurationHandler,
                                    final OSGIKillbillAPI killbillAPI,
                                    final OSGIConfigPropertiesService osgiConfigPropertiesService,
                                    final OSGIKillbillLogService logService,
                                    final Clock clock,
                                    final IngenicoDao dao) {
        super(killbillAPI, osgiConfigPropertiesService, logService, clock, dao);
        this.ingenicoConfigurationHandler = ingenicoConfigurationHandler;
        this.logService = logService;
        this.dao = dao;
    }



    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeInitialTransaction(TransactionType.AUTHORIZE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.CAPTURE,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(final PaymentData paymentData, final String paymentId, final SplitSettlementData splitSettlementData) {
                                                  final IngenicoClient ingenicoClient = ingenicoConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return ingenicoClient.capture(paymentData, paymentId, splitSettlementData);
                                              }
                                          },
                                          kbAccountId,
                                          kbPaymentId,
                                          kbTransactionId,
                                          kbPaymentMethodId,
                                          amount,
                                          currency,
                                          properties,
                                          context);
    }

    private IngenicoPaymentMethodsRecord getIngenicoPaymentMethodsRecord(UUID kbPaymentMethodId, TenantContext context) {
        IngenicoPaymentMethodsRecord paymentMethodsRecord = null;

        if (kbPaymentMethodId != null) {
            try {
                paymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
            } catch (final SQLException e) {
                logService.log(LogService.LOG_WARNING, "Failed to retrieve payment method " + kbPaymentMethodId, e);
            }
        }

        return MoreObjects.firstNonNull(paymentMethodsRecord, emptyRecord(kbPaymentMethodId));
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeInitialTransaction(TransactionType.PURCHASE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.VOID,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData) {
                                                  final IngenicoClient ingenicoClient = ingenicoConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return ingenicoClient.cancel(paymentData, pspReference, splitSettlementData);
                                              }
                                          },
                                          kbAccountId,
                                          kbPaymentId,
                                          kbTransactionId,
                                          kbPaymentMethodId,
                                          null,
                                          null,
                                          properties,
                                          context);
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeInitialTransaction(TransactionType.CREDIT, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.REFUND,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData) {
                                                  final IngenicoClient ingenicoClient = ingenicoConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return ingenicoClient.refund(paymentData, pspReference, splitSettlementData);
                                              }
                                          },
                                          kbAccountId,
                                          kbPaymentId,
                                          kbTransactionId,
                                          kbPaymentMethodId,
                                          amount,
                                          currency,
                                          properties,
                                          context);
    }

    @Override
    protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(IngenicoResponsesRecord ingenicoResponsesRecord) {
        return new IngenicoPaymentTransactionInfoPlugin(ingenicoResponsesRecord);
    }

    @Override
    protected PaymentMethodPlugin buildPaymentMethodPlugin(IngenicoPaymentMethodsRecord paymentMethodsRecord) {
        return new IngenicoPaymentMethodPlugin(paymentMethodsRecord);
    }

    @Override
    protected PaymentMethodInfoPlugin buildPaymentMethodInfoPlugin(IngenicoPaymentMethodsRecord paymentMethodsRecord) {
        return new IngenicoPaymentMethodInfoPlugin(paymentMethodsRecord);
    }

    @Override
    protected String getPaymentMethodId(IngenicoPaymentMethodsRecord paymentMethodsRecord) {
        return paymentMethodsRecord.getKbPaymentMethodId();
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final Map<String, String> safePropertiesMap = new HashMap<String, String>(PluginProperties.toStringMap(paymentMethodProps.getProperties(), properties));
        final IngenicoClient client = ingenicoConfigurationHandler.getConfigurable(context.getTenantId());

        final Account account = getAccount(kbAccountId, context);
        final PaymentInfo paymentInfo = buildPaymentInfo(account, paymentMethodProps.getProperties(), context);

        final UserData userData = toUserData(account, properties);
        final String token = client.tokenizeCreditCard(paymentInfo, userData);
        if (token == null) {
            throw new PaymentPluginApiException("Token not created", "Unable to create token");
        }
        safePropertiesMap.put(PROPERTY_TOKEN, token);

        // Delete sensitive data
        safePropertiesMap.remove(PROPERTY_CC_NUMBER);
        safePropertiesMap.remove(PROPERTY_CC_VERIFICATION_VALUE);
        //safePropertiesMap.remove(PROPERTY_ACCOUNT_NUMBER);
        final PluginPaymentMethodPlugin safePaymentMethodProps = new PluginPaymentMethodPlugin(kbPaymentMethodId, token, setDefault, ImmutableList.<PluginProperty>of());

        final Iterable<PluginProperty> safeProperties = PluginProperties.buildPluginProperties(safePropertiesMap);
        super.addPaymentMethod(kbAccountId, kbPaymentMethodId, safePaymentMethodProps, setDefault, safeProperties, context);
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    //    @Override
//    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
//        return null;
//    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    private abstract static class TransactionExecutor<T> {

        public T execute(final PaymentData paymentData, final UserData userData, final SplitSettlementData splitSettlementData) {
            throw new UnsupportedOperationException();
        }

        public T execute(final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData) {
            throw new UnsupportedOperationException();
        }
    }

    private PaymentTransactionInfoPlugin executeInitialTransaction(final TransactionType transactionType,
                                                                   final UUID kbAccountId,
                                                                   final UUID kbPaymentId,
                                                                   final UUID kbTransactionId,
                                                                   final UUID kbPaymentMethodId,
                                                                   final BigDecimal amount,
                                                                   final Currency currency,
                                                                   final Iterable<PluginProperty> properties,
                                                                   final CallContext context) throws PaymentPluginApiException {
        return executeInitialTransaction(transactionType,
                                         new TransactionExecutor<PurchaseResult>() {
                                             @Override
                                             public PurchaseResult execute(final PaymentData paymentData, final UserData userData, final SplitSettlementData splitSettlementData) {
                                                 final IngenicoClient ingenicoClient = ingenicoConfigurationHandler.getConfigurable(context.getTenantId());

                                                 return ingenicoClient.create(paymentData, userData, splitSettlementData);
                                             }
                                         },
                                         kbAccountId,
                                         kbPaymentId,
                                         kbTransactionId,
                                         kbPaymentMethodId,
                                         amount,
                                         currency,
                                         properties,
                                         context);
    }

    private PaymentTransactionInfoPlugin executeInitialTransaction(final TransactionType transactionType,
                                                                   final TransactionExecutor<PurchaseResult> transactionExecutor,
                                                                   final UUID kbAccountId,
                                                                   final UUID kbPaymentId,
                                                                   final UUID kbTransactionId,
                                                                   final UUID kbPaymentMethodId,
                                                                   final BigDecimal amount,
                                                                   final Currency currency,
                                                                   final Iterable<PluginProperty> properties,
                                                                   final TenantContext context) throws PaymentPluginApiException {
        final Account account = getAccount(kbAccountId, context);

        final IngenicoPaymentMethodsRecord nonNullPaymentMethodsRecord = getIngenicoPaymentMethodsRecord(kbPaymentMethodId, context);
        // Pull extra properties from the payment method (such as the customerId)
        final Iterable<PluginProperty> additionalPropertiesFromRecord = buildPaymentMethodPlugin(nonNullPaymentMethodsRecord).getProperties();
        //noinspection unchecked
        final Iterable<PluginProperty> mergedProperties = PluginProperties.merge(additionalPropertiesFromRecord, properties);
        final PaymentData paymentData = buildPaymentData(account, kbPaymentId, kbTransactionId, nonNullPaymentMethodsRecord, amount, currency, mergedProperties, context);
        final UserData userData = toUserData(account, mergedProperties);
        final SplitSettlementData splitSettlementData = null;
        final DateTime utcNow = clock.getUTCNow();

        final PurchaseResult response = transactionExecutor.execute(paymentData, userData, splitSettlementData);
        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, response, utcNow, context.getTenantId());
            return new IngenicoPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + response.toString(), e);
        }
    }

    private PaymentTransactionInfoPlugin executeFollowUpTransaction(final TransactionType transactionType,
                                                                    final TransactionExecutor<PaymentModificationResponse> transactionExecutor,
                                                                    final UUID kbAccountId,
                                                                    final UUID kbPaymentId,
                                                                    final UUID kbTransactionId,
                                                                    final UUID kbPaymentMethodId,
                                                                    @Nullable final BigDecimal amount,
                                                                    @Nullable final Currency currency,
                                                                    final Iterable<PluginProperty> properties,
                                                                    final TenantContext context) throws PaymentPluginApiException {
        final Account account = getAccount(kbAccountId, context);

        final String paymentId;
        try {
            final IngenicoResponsesRecord previousResponse = dao.getSuccessfulAuthorizationResponse(kbPaymentId, context.getTenantId());
            if (previousResponse == null) {
                throw new PaymentPluginApiException(null, "Unable to retrieve previous payment response for kbTransactionId " + kbTransactionId);
            }
            paymentId = previousResponse.getIngenicoPaymentId();
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve previous payment response for kbTransactionId " + kbTransactionId, e);
        }

        final IngenicoPaymentMethodsRecord nonNullPaymentMethodsRecord = getIngenicoPaymentMethodsRecord(kbPaymentMethodId, context);
        final PaymentData paymentData = buildPaymentData(account, kbPaymentId, kbTransactionId, nonNullPaymentMethodsRecord, amount, currency, properties, context);
        final SplitSettlementData splitSettlementData = null;
        final DateTime utcNow = clock.getUTCNow();

        final PaymentModificationResponse response = transactionExecutor.execute(paymentData, paymentId, splitSettlementData);
        final Optional<PaymentServiceProviderResult> paymentServiceProviderResult;
        if (response.isTechnicallySuccessful()) {
            paymentServiceProviderResult = Optional.of(PaymentServiceProviderResult.RECEIVED);
        } else {
            paymentServiceProviderResult = Optional.<PaymentServiceProviderResult>absent();
        }

        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, response, utcNow, context.getTenantId());
            return new IngenicoPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, paymentServiceProviderResult, utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + (response.toString()), e);
        }
    }

    private IngenicoPaymentMethodsRecord emptyRecord(@Nullable final UUID kbPaymentMethodId) {
        final IngenicoPaymentMethodsRecord record = new IngenicoPaymentMethodsRecord();
        if (kbPaymentMethodId != null) {
            record.setKbPaymentMethodId(kbPaymentMethodId.toString());
        }
        return record;
    }

    private PaymentData<PaymentInfo> buildPaymentData(final AccountData account, final UUID kbPaymentId, final UUID kbTransactionId, final IngenicoPaymentMethodsRecord paymentMethodsRecord, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        final Payment payment;
        try {
            payment = killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, false, properties, context);
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException(String.format("Unable to retrieve kbPaymentId='%s'", kbPaymentId), e);
        }

        final PaymentTransaction paymentTransaction = Iterables.<PaymentTransaction>find(payment.getTransactions(),
                                                                                         new Predicate<PaymentTransaction>() {
                                                                                             @Override
                                                                                             public boolean apply(final PaymentTransaction input) {
                                                                                                 return kbTransactionId.equals(input.getId());
                                                                                             }
                                                                                         });

        final PaymentInfo paymentInfo = buildPaymentInfo(account, paymentMethodsRecord, properties, context);

        return new PaymentData<PaymentInfo>(amount, currency, paymentTransaction.getExternalKey(), paymentInfo);
    }

    private PaymentInfo buildPaymentInfo(AccountData account, IngenicoPaymentMethodsRecord paymentMethodsRecord, Iterable<PluginProperty> properties, TenantContext context) {
        return PaymentInfoMappingService.toPaymentInfo(clock, account, paymentMethodsRecord, properties);
    }

    private PaymentInfo buildPaymentInfo(AccountData account, Iterable<PluginProperty> properties, TenantContext context) {
        return PaymentInfoMappingService.toPaymentInfo(clock, account, properties);
    }
}
