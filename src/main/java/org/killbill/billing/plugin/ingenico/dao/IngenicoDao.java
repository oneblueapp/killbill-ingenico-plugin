package org.killbill.billing.plugin.ingenico.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;
import org.killbill.billing.plugin.ingenico.api.IngenicoPaymentPluginApi;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoPaymentMethods;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoResponses;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoResponsesRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

import static org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoPaymentMethods.INGENICO_PAYMENT_METHODS;
import static org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoResponses.INGENICO_RESPONSES;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class IngenicoDao extends PluginPaymentDao<IngenicoResponsesRecord, IngenicoResponses, IngenicoPaymentMethodsRecord, IngenicoPaymentMethods> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Joiner JOINER = Joiner.on(",");

    public IngenicoDao(final DataSource dataSource) throws SQLException {
        super(INGENICO_RESPONSES, INGENICO_PAYMENT_METHODS, dataSource);
    }

    // Payment methods

    public void setPaymentMethodToken(final String kbPaymentMethodId, final String token, final String kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<IngenicoResponsesRecord>() {
                    @Override
                    public IngenicoResponsesRecord withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .update(INGENICO_PAYMENT_METHODS)
                                .set(INGENICO_PAYMENT_METHODS.TOKEN, token)
                                .where(INGENICO_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId))
                                .and(INGENICO_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId))
                                .and(INGENICO_PAYMENT_METHODS.IS_DELETED.equal(FALSE))
                                .execute();
                        return null;
                    }
                });
    }

    public void addResponse(final UUID kbAccountId,
                            final UUID kbPaymentId,
                            final UUID kbPaymentTransactionId,
                            final TransactionType transactionType,
                            final BigDecimal amount,
                            final Currency currency,
                            final PurchaseResult result,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .insertInto(INGENICO_RESPONSES,
                                        INGENICO_RESPONSES.KB_ACCOUNT_ID,
                                        INGENICO_RESPONSES.KB_PAYMENT_ID,
                                        INGENICO_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                        INGENICO_RESPONSES.TRANSACTION_TYPE,
                                        INGENICO_RESPONSES.AMOUNT,
                                        INGENICO_RESPONSES.CURRENCY,
                                        INGENICO_RESPONSES.PG_MERCHANT_ID,
                                        INGENICO_RESPONSES.PG_TRANSACTION_ID,
                                        INGENICO_RESPONSES.PG_STATUS,
                                        INGENICO_RESPONSES.PG_TRANSACTION_METHOD,
                                            INGENICO_RESPONSES.REFERENCE,
                                        INGENICO_RESPONSES.PG_MERCHANT_REFERENCE,
                                        INGENICO_RESPONSES.PG_AUTHORIZATION_CODE,
                                        INGENICO_RESPONSES.PG_PRODUCT_ID,
                                        INGENICO_RESPONSES.PG_ERROR_CODE,
                                        INGENICO_RESPONSES.PG_ERROR_MESSAGE,
                                        INGENICO_RESPONSES.PG_FRAUD_AVS_RESULT,
                                        INGENICO_RESPONSES.PG_FRAUD_CVV_RESULT,
                                        INGENICO_RESPONSES.PG_FRAUD_RESULT,
                                        INGENICO_RESPONSES.CREATED_DATE,
                                        INGENICO_RESPONSES.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentId.toString(),
                                        kbPaymentTransactionId.toString(),
                                        transactionType.toString(),
                                        amount,
                                        currency.toString(),
                                        result.getMerchantId(),
                                        result.getPgTransactionId(),
                                        result.getPgStatus(),
                                        result.getPgTransactionMethod(),
                                        result.getReference(),
                                        result.getPgMerchantReference(),
                                        result.getPgAuthorizationCode(),
                                        result.getPgProductiId(),
                                        result.getPgErrorCode(),
                                        result.getPgErrorMessage(),
                                        result.getPgFraudAvsResult(),
                                        result.getPgFraudCvvResult(),
                                        result.getPgFraudResult(),
                                        toTimestamp(utcNow),
                                        kbTenantId.toString())
                                .execute();
                        return null;
                    }
                });
    }

    public void addResponse(final UUID kbAccountId,
                            final UUID kbPaymentId,
                            final UUID kbPaymentTransactionId,
                            final TransactionType transactionType,
                            @Nullable final BigDecimal amount,
                            @Nullable final Currency currency,
                            final PaymentModificationResponse result,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {

//        execute(dataSource.getConnection(),
//                new WithConnectionCallback<Void>() {
//                    @Override
//                    public Void withConnection(final Connection conn) throws SQLException {
//                        DSL.using(conn, dialect, settings)
//                           .insertInto(INGENICO_RESPONSES,
//                                       INGENICO_RESPONSES.KB_ACCOUNT_ID,
//                                       INGENICO_RESPONSES.KB_PAYMENT_ID,
//                                       INGENICO_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
//                                       INGENICO_RESPONSES.TRANSACTION_TYPE,
//                                       INGENICO_RESPONSES.AMOUNT,
//                                       INGENICO_RESPONSES.CURRENCY,
//                                       INGENICO_RESPONSES.PG_MERCHANT_ID,
//                                       INGENICO_RESPONSES.PG_TRANSACTION_ID,
//                                       INGENICO_RESPONSES.PG_STATUS,
//                                       INGENICO_RESPONSES.PG_TRANSACTION_METHOD,
//                                       INGENICO_RESPONSES.PG_REFERENCE,
//                                       INGENICO_RESPONSES.PG_AUTHORIZATION_CODE,
//                                       INGENICO_RESPONSES.PG_PRODUCT_ID,
//                                       INGENICO_RESPONSES.PG_ERROR_CODE,
//                                       INGENICO_RESPONSES.PG_ERROR_MESSAGE,
//                                       INGENICO_RESPONSES.PG_FRAUD_AVS_RESULT,
//                                       INGENICO_RESPONSES.PG_FRAUD_CVV_RESULT,
//                                       INGENICO_RESPONSES.PG_FRAUD_RESULT,
//                                       INGENICO_RESPONSES.CREATED_DATE,
//                                       INGENICO_RESPONSES.KB_TENANT_ID)
//                           .values(kbAccountId.toString(),
//                                   kbPaymentId.toString(),
//                                   kbPaymentTransactionId.toString(),
//                                   transactionType.toString(),
//                                   amount,
//                                   currency,
//                                   result.getResponse(),
//                                   result.getPspReference(),
//                                   null,
//                                   null,
//                                   null,
//                                   null,
//                                   null,
//                                   null,
//                                   null,
//                                   dccAmountValue == null ? null : new BigDecimal(dccAmountValue),
//                                   getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result),
//                                   getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result),
//                                   getProperty(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result),
//                                   getProperty(AdyenPaymentPluginApi.PROPERTY_MD, result),
//                                   getProperty(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result),
//                                   additionalData,
//                                   toTimestamp(utcNow),
//                                   kbTenantId.toString())
//                           .execute();
//                        return null;
//                    }
//                });
    }

}
