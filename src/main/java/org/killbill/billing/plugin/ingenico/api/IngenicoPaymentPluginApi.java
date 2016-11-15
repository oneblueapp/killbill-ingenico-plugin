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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.*;
import org.killbill.billing.payment.plugin.api.*;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;
import org.killbill.billing.plugin.ingenico.IngenicoPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.ingenico.client.IngenicoClient;
import org.killbill.billing.plugin.ingenico.client.model.*;
import org.killbill.billing.plugin.ingenico.core.IngenicoConfigurationHandler;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoPaymentMethods;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoResponses;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.clock.Clock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.service.log.LogService;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
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
    public static final String PROPERTY_CUSTOMER_ID = "customerId";
    public static final String PROPERTY_EMAIL = "email";

    public IngenicoPaymentPluginApi(final IngenicoConfigurationHandler ingenicoConfigurationHandler, OSGIKillbillAPI killbillAPI, OSGIConfigPropertiesService osgiConfigPropertiesService, Clock clock, PluginPaymentDao dao, final OSGIKillbillLogService logService) {
        super(killbillAPI, osgiConfigPropertiesService, logService, clock, dao);
        this.ingenicoConfigurationHandler = ingenicoConfigurationHandler;
        this.logService = logService;
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final IngenicoClient client = ingenicoConfigurationHandler.getConfigurable(context.getTenantId());

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

        final String merchantAccount = getMerchantAccount(paymentData, properties, context);

//        final PurchaseResult response = transactionExecutor.execute(merchantAccount, paymentData, userData, splitSettlementData);
        final PurchaseResult response = client.create(paymentData, userData, splitSettlementData);
        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.AUTHORIZE, amount, currency, response, utcNow, context.getTenantId());
            return new IngenicoPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, TransactionType.AUTHORIZE, amount, currency, utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + response.toString(), e);
        }
        return null;
    }

    private IngenicoPaymentMethodsRecord getIngenicoPaymentMethodsRecord(UUID kbPaymentMethodId, CallContext context) {
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
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(IngenicoResponsesRecord record) {
        return null;
    }

    @Override
    protected PaymentMethodPlugin buildPaymentMethodPlugin(IngenicoPaymentMethodsRecord paymentMethodsRecord) {
        return new IngenicoPaymentMethodPlugin(paymentMethodsRecord);
    }

    @Override
    protected PaymentMethodInfoPlugin buildPaymentMethodInfoPlugin(IngenicoPaymentMethodsRecord record) {
        return null;
    }

    @Override
    protected String getPaymentMethodId(IngenicoPaymentMethodsRecord input) {
        return null;
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
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
            payment = killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, properties, context);
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

        //final PaymentInfo paymentInfo = buildPaymentInfo(account, paymentMethodsRecord, properties, context);

        return new PaymentData<PaymentInfo>(amount, currency, paymentTransaction.getExternalKey());
    }

    private String getMerchantAccount(final PaymentData paymentData, final Iterable<PluginProperty> properties, final TenantContext context) {
        final String countryIsoCode = paymentData.getPaymentInfo().getCountry();
        return getMerchantAccount(countryIsoCode, properties, context);
    }

    private String getMerchantAccount(final String countryCode, final Iterable<PluginProperty> properties, final TenantContext context) {
        final String pluginPropertyMerchantAccount = PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_PROCESSOR_ACCOUNT_ID, properties);
        if (pluginPropertyMerchantAccount != null) {
            return pluginPropertyMerchantAccount;
        }

        // A bit of a hack - it would be nice to be able to isolate AdyenConfigProperties
        final AdyenConfigProperties adyenConfigProperties = adyenHppConfigurationHandler.getConfigurable(context.getTenantId()).getAdyenConfigProperties();
        return adyenConfigProperties.getMerchantAccount(countryCode);
    }
}
