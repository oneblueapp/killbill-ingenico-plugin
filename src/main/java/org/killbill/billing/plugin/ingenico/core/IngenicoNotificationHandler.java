/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.ingenico.core;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.ingenico.api.IngenicoCallContext;
import org.killbill.billing.plugin.ingenico.api.IngenicoPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.dao.IngenicoDao;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Created by otaviosoares on 30/11/16.
 */
public class IngenicoNotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(IngenicoNotificationHandler.class);

    // Note that AUTHORISATION maps to either AUTHORIZE or PURCHASE
    private static final Map<String, TransactionType> EVENT_CODES_TO_TRANSACTION_TYPE = ImmutableMap.<String, TransactionType>builder().put("CANCELLED", TransactionType.VOID)
                                                                                                                                       .put("REFUNDED", TransactionType.REFUND)
                                                                                                                                       .put("CAPTURED", TransactionType.CAPTURE)
                                                                                                                                       .put("CHARGEBACKED", TransactionType.CHARGEBACK)
                                                                                                                                       .put("REVERSED", TransactionType.CHARGEBACK)
                                                                                                                                       .build();

    private final OSGIKillbillAPI killbillAPI;
    private final IngenicoDao dao;
    private final Clock clock;

    public IngenicoNotificationHandler(final OSGIKillbillAPI killbillAPI, final IngenicoDao dao, final Clock clock) {

        this.killbillAPI = killbillAPI;
        this.dao = dao;
        this.clock = clock;
    }

    public void updatePaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final List<PaymentTransactionInfoPlugin> paymentTransactions, TransactionType transactionType, final IngenicoResponsesRecord ingenicoResponseRecord, final PaymentModificationResponse response, final TenantContext tenantContext) throws SQLException {
        UUID kbTransactionId = UUID.fromString(ingenicoResponseRecord.getKbPaymentTransactionId());
        final DateTime utcNow = clock.getUTCNow();
        final TransactionType expectedTransactionType = EVENT_CODES_TO_TRANSACTION_TYPE.get(ingenicoResponseRecord.getIngenicoStatus());
        final BigDecimal amount = ingenicoResponseRecord.getAmount();
        final Currency currency = Currency.fromCode(ingenicoResponseRecord.getCurrency());
        final CallContext context = new IngenicoCallContext(utcNow, tenantContext.getTenantId());

        IngenicoPaymentTransactionInfoPlugin paymentTransactionInfoPlugin = new IngenicoPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, response.getResult() , utcNow, response);
        final PaymentPluginStatus paymentPluginStatus = paymentTransactionInfoPlugin.getStatus();

        Account account = getAccount(kbAccountId, tenantContext);
        Payment payment = getPayment(kbPaymentId, context);
        PaymentTransaction paymentTransaction = filterForTransaction(payment, kbTransactionId);

        if (PaymentPluginStatus.UNDEFINED.equals(paymentPluginStatus)) {
            return;
        } else if (paymentTransaction != null && TransactionStatus.PENDING.equals(paymentTransaction.getTransactionStatus())) {
            transitionPendingTransaction(account, kbTransactionId, paymentPluginStatus, context);
        } else if (paymentTransaction != null && paymentTransactionInfoPlugin.getStatus() != paymentPluginStatus) {
            fixPaymentTransactionState(payment, paymentTransaction, paymentPluginStatus, ingenicoResponseRecord, context);
        } else if (paymentTransaction == null && expectedTransactionType == TransactionType.CHARGEBACK && PaymentPluginStatus.PROCESSED.equals(paymentPluginStatus)) {
            createChargeback(account, kbPaymentId, paymentTransactionInfoPlugin, context);
        } else if (paymentTransaction == null && expectedTransactionType == TransactionType.CHARGEBACK && PaymentPluginStatus.ERROR.equals(paymentPluginStatus)) {
            // There should only be one chargeback in Kill Bill, see https://github.com/killbill/killbill/issues/477
            final PaymentTransactionInfoPlugin chargeback = filterTransactions(paymentTransactions, TransactionType.CHARGEBACK);
            createChargebackReversal(account, kbPaymentId, chargeback, context);
        }
        else {
            return;
        }

        Iterable<PluginProperty> additionalData = PluginProperties.buildPluginProperties(response.getAdditionalData());
        dao.updateResponse(kbTransactionId, response.getStatus(), response.getResultOrNull(), additionalData, tenantContext.getTenantId());
    }

    private void createChargeback(final Account account, final UUID kbPaymentId, final PaymentTransactionInfoPlugin transaction, final CallContext context) {
        BigDecimal amount = transaction.getAmount();
        Currency currency = transaction.getCurrency();

        // using AUTH CODE - DONT KNOW IF IT's CORRECT
        final String paymentTransactionExternalKey = transaction.getSecondPaymentReferenceId();

        try {
            final Payment chargeback = killbillAPI.getPaymentApi().createChargeback(account,
                                                                                        kbPaymentId,
                                                                                        amount,
                                                                                        currency,
                                                                                        paymentTransactionExternalKey,
                                                                                        context);
        } catch (final PaymentApiException e) {
            // Have Ingenico retry
            throw new RuntimeException("Failed to record chargeback", e);
        }
    }

    private void createChargebackReversal(final Account account, final UUID kbPaymentId, final PaymentTransactionInfoPlugin chargeback, final CallContext context) {
        final String paymentTransactionExternalKey = chargeback.getSecondPaymentReferenceId();

        try {
            final Payment chargebackReversal = killbillAPI.getPaymentApi().createChargebackReversal(account,
                                                                                                        kbPaymentId,
                                                                                                        paymentTransactionExternalKey,
                                                                                                        context);
        } catch (final PaymentApiException e) {
            // Have Ingenico retry
            throw new RuntimeException("Failed to record chargeback reversal", e);
        }
    }

    private void fixPaymentTransactionState(final Payment payment, final PaymentTransaction paymentTransaction, final PaymentPluginStatus paymentPluginStatus, final IngenicoResponsesRecord ingenicoResponseRecord, final CallContext context) {
        final PaymentTransaction updatedPaymentTransaction;
        if (ingenicoResponseRecord == null) {
            updatedPaymentTransaction = paymentTransaction;
        } else {
            //            updatedPaymentTransaction = new IngenicoPaymentTransaction(paymentTransactionInfoPlugin.getGatewayErrorCode(), paymentTransactionInfoPlugin.getGatewayError(), paymentTransaction);
            updatedPaymentTransaction = paymentTransaction;
        }

        final String currentPaymentStateName = String.format("%s_%s", updatedPaymentTransaction.getTransactionType() == TransactionType.AUTHORIZE ? "AUTH" : updatedPaymentTransaction.getTransactionType(), paymentPluginStatus == PaymentPluginStatus.PROCESSED ? "SUCCESS" : "FAILED");

        final TransactionStatus transactionStatus;
        switch (paymentPluginStatus) {
            case PROCESSED:
                transactionStatus = TransactionStatus.SUCCESS;
                break;
            case PENDING:
                transactionStatus = TransactionStatus.PENDING;
                break;
            case ERROR:
                transactionStatus = TransactionStatus.PAYMENT_FAILURE;
                break;
            case CANCELED:
                transactionStatus = TransactionStatus.PLUGIN_FAILURE;
                break;
            default:
                transactionStatus = TransactionStatus.UNKNOWN;
                break;
        }
        logger.warn("Forcing transition paymentTransactionExternalKey='{}', oldPaymentPluginStatus='{}', newPaymentPluginStatus='{}'", updatedPaymentTransaction.getExternalKey(), updatedPaymentTransaction.getPaymentInfoPlugin().getStatus(), paymentPluginStatus);

        try {
            killbillAPI.getAdminPaymentApi().fixPaymentTransactionState(payment, updatedPaymentTransaction, transactionStatus, null, currentPaymentStateName, ImmutableList.<PluginProperty>of(), context);
        } catch (final PaymentApiException e) {
            // Have Ingenico retry
            throw new RuntimeException(String.format("Failed to fix transaction kbPaymentTransactionId='%s'", updatedPaymentTransaction.getId()), e);
        }
    }

    private void transitionPendingTransaction(final Account account, final UUID kbTransactionId, final PaymentPluginStatus paymentPluginStatus, final CallContext context) {
        try {
            final Payment payment = killbillAPI.getPaymentApi().notifyPendingTransactionOfStateChanged(account, kbTransactionId, paymentPluginStatus == PaymentPluginStatus.PROCESSED, context);
        } catch (final PaymentApiException e) {
            // Have Ingenico retry
            throw new RuntimeException(String.format("Failed to transition pending transaction kbPaymentTransactionId='%s'", kbTransactionId), e);
        }
    }

    private Payment getPayment(final UUID kbPaymentId, final TenantContext context) {
        try {
            return killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, false, ImmutableList.<PluginProperty>of(), context);
        } catch (final PaymentApiException e) {
            // Have Ingenico retry
            throw new RuntimeException(String.format("Failed to retrieve kbPaymentId='%s'", kbPaymentId), e);
        }
    }

    private PaymentTransactionInfoPlugin filterTransactions(final List<PaymentTransactionInfoPlugin> transactions, final UUID kbTransactionId) {
        for (final PaymentTransactionInfoPlugin paymentTransaction : transactions) {
            if (paymentTransaction.getKbTransactionPaymentId().equals(kbTransactionId)) {
                return paymentTransaction;
            }
        }
        return null;
    }

    private PaymentTransactionInfoPlugin filterTransactions(final List<PaymentTransactionInfoPlugin> transactions, final TransactionType transactionType) {
        for (final PaymentTransactionInfoPlugin paymentTransaction : transactions) {
            if (paymentTransaction.getTransactionType().equals(transactionType)) {
                return paymentTransaction;
            }
        }
        return null;
    }

    private PaymentTransaction filterForTransaction(final Payment payment, final UUID kbTransactionId) {
        for (final PaymentTransaction paymentTransaction : payment.getTransactions()) {
            if (paymentTransaction.getId().equals(kbTransactionId)) {
                return paymentTransaction;
            }
        }
        return null;
    }

    private PaymentTransaction filterForTransaction(final Payment payment, final TransactionType transactionType) {
        for (final PaymentTransaction paymentTransaction : payment.getTransactions()) {
            if (paymentTransaction.getTransactionType().equals(transactionType)) {
                return paymentTransaction;
            }
        }
        return null;
    }

    private Account getAccount(final UUID kbAccountId, final TenantContext context) {
        try {
            return killbillAPI.getAccountUserApi().getAccountById(kbAccountId, context);
        } catch (final AccountApiException e) {
            // Have Ingenico retry
            throw new RuntimeException(String.format("Failed to retrieve kbAccountId='%s'", kbAccountId), e);
        }
    }
}
