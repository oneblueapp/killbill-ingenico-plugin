package org.killbill.billing.plugin.ingenico.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;
import org.killbill.billing.plugin.ingenico.api.IngenicoPaymentPluginApi;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoPaymentMethods;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.IngenicoResponses;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoResponsesRecord;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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
        final String dccAmountValue = getProperty(IngenicoPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result);
        final String additionalData = getAdditionalData(result);

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
                                        INGENICO_RESPONSES.PSP_RESULT,
                                        INGENICO_RESPONSES.PSP_REFERENCE,
                                        INGENICO_RESPONSES.AUTH_CODE,
                                        INGENICO_RESPONSES.RESULT_CODE,
                                        INGENICO_RESPONSES.REFUSAL_REASON,
                                        INGENICO_RESPONSES.REFERENCE,
                                        INGENICO_RESPONSES.PSP_ERROR_CODES,
                                        INGENICO_RESPONSES.PAYMENT_INTERNAL_REF,
                                        INGENICO_RESPONSES.FORM_URL,
                                        INGENICO_RESPONSES.DCC_AMOUNT,
                                        INGENICO_RESPONSES.DCC_CURRENCY,
                                        INGENICO_RESPONSES.DCC_SIGNATURE,
                                        INGENICO_RESPONSES.ISSUER_URL,
                                        INGENICO_RESPONSES.MD,
                                        INGENICO_RESPONSES.PA_REQUEST,
                                        INGENICO_RESPONSES.ADDITIONAL_DATA,
                                        INGENICO_RESPONSES.CREATED_DATE,
                                        INGENICO_RESPONSES.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentId.toString(),
                                        kbPaymentTransactionId.toString(),
                                        transactionType.toString(),
                                        amount,
                                        currency,
                                        result.getResult().isPresent() ? result.getResult().get().toString() : null,
                                        result.getPspReference(),
                                        result.getAuthCode(),
                                        result.getResultCode(),
                                        result.getReason(),
                                        result.getReference(),
                                        null,
                                        result.getPaymentTransactionExternalKey(),
                                        result.getFormUrl(),
                                        dccAmountValue == null ? null : new BigDecimal(dccAmountValue),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_ISSUER_URL, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_MD, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_PA_REQ, result),
                                        additionalData,
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
        final String dccAmountValue = getProperty(IngenicoPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result);
        final String additionalData = getAdditionalData(result);

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
                                        INGENICO_RESPONSES.PSP_RESULT,
                                        INGENICO_RESPONSES.PSP_REFERENCE,
                                        INGENICO_RESPONSES.AUTH_CODE,
                                        INGENICO_RESPONSES.RESULT_CODE,
                                        INGENICO_RESPONSES.REFUSAL_REASON,
                                        INGENICO_RESPONSES.REFERENCE,
                                        INGENICO_RESPONSES.PSP_ERROR_CODES,
                                        INGENICO_RESPONSES.PAYMENT_INTERNAL_REF,
                                        INGENICO_RESPONSES.FORM_URL,
                                        INGENICO_RESPONSES.DCC_AMOUNT,
                                        INGENICO_RESPONSES.DCC_CURRENCY,
                                        INGENICO_RESPONSES.DCC_SIGNATURE,
                                        INGENICO_RESPONSES.ISSUER_URL,
                                        INGENICO_RESPONSES.MD,
                                        INGENICO_RESPONSES.PA_REQUEST,
                                        INGENICO_RESPONSES.ADDITIONAL_DATA,
                                        INGENICO_RESPONSES.CREATED_DATE,
                                        INGENICO_RESPONSES.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentId.toString(),
                                        kbPaymentTransactionId.toString(),
                                        transactionType.toString(),
                                        amount,
                                        currency,
                                        result.getResponse(),
                                        result.getPspReference(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        dccAmountValue == null ? null : new BigDecimal(dccAmountValue),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_ISSUER_URL, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_MD, result),
                                        getProperty(IngenicoPaymentPluginApi.PROPERTY_PA_REQ, result),
                                        additionalData,
                                        toTimestamp(utcNow),
                                        kbTenantId.toString())
                                .execute();
                        return null;
                    }
                });
    }

    /**
     * Update the PSP reference and additional data of the latest response row for a payment transaction
     *
     * @param kbPaymentTransactionId     Kill Bill payment transaction id
     * @param additionalPluginProperties Latest properties
     * @param kbTenantId                 Kill Bill tenant id
     * @return the latest version of the response row, null if one couldn't be found
     * @throws SQLException For any unexpected SQL error
     */
    public IngenicoResponsesRecord updateResponse(final UUID kbPaymentTransactionId, final Iterable<PluginProperty> additionalPluginProperties, final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalProperties = PluginProperties.toMap(additionalPluginProperties);

        return execute(dataSource.getConnection(),
                new WithConnectionCallback<IngenicoResponsesRecord>() {
                    @Override
                    public IngenicoResponsesRecord withConnection(final Connection conn) throws SQLException {
                        final IngenicoResponsesRecord response = DSL.using(conn, dialect, settings)
                                .selectFrom(INGENICO_RESPONSES)
                                .where(INGENICO_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                .and(INGENICO_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                .orderBy(INGENICO_RESPONSES.RECORD_ID.desc())
                                .limit(1)
                                .fetchOne();

                        if (response == null) {
                            return null;
                        }

                        final Map originalData = new HashMap(fromAdditionalData(response.getAdditionalData()));
                        originalData.putAll(additionalProperties);
                        final String mergedAdditionalData = getAdditionalData(originalData);

                        DSL.using(conn, dialect, settings)
                                .update(INGENICO_RESPONSES)
                                .set(INGENICO_RESPONSES.PSP_REFERENCE, getProperty(IngenicoPaymentPluginApi.PROPERTY_PSP_REFERENCE, additionalProperties))
                                .set(INGENICO_RESPONSES.ADDITIONAL_DATA, mergedAdditionalData)
                                .where(INGENICO_RESPONSES.RECORD_ID.equal(response.getRecordId()))
                                .execute();

                        return DSL.using(conn, dialect, settings)
                                .selectFrom(INGENICO_RESPONSES)
                                .where(INGENICO_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                .and(INGENICO_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                .orderBy(INGENICO_RESPONSES.RECORD_ID.desc())
                                .limit(1)
                                .fetchOne();
                    }
                });
    }

    @Override
    public List<IngenicoResponsesRecord> getResponses(final UUID kbPaymentId, final UUID kbTenantId) throws SQLException {
        final List<IngenicoResponsesRecord> responses = new LinkedList<IngenicoResponsesRecord>();
        for (final IngenicoResponsesRecord adyenResponsesRecord : Lists.<IngenicoResponsesRecord>reverse(super.getResponses(kbPaymentId, kbTenantId))) {
            responses.add(adyenResponsesRecord);

            // Keep only the completion row for 3D-S
            if (TransactionType.AUTHORIZE.toString().equals(adyenResponsesRecord.getTransactionType())) {
                break;
            }
        }
        return Lists.<IngenicoResponsesRecord>reverse(responses);
    }

    // Assumes that the last auth was successful
    @Override
    public IngenicoResponsesRecord getSuccessfulAuthorizationResponse(final UUID kbPaymentId, final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                new WithConnectionCallback<IngenicoResponsesRecord>() {
                    @Override
                    public IngenicoResponsesRecord withConnection(final Connection conn) throws SQLException {
                        return DSL.using(conn, dialect, settings)
                                .selectFrom(responsesTable)
                                .where(DSL.field(responsesTable.getName() + "." + KB_PAYMENT_ID).equal(kbPaymentId.toString()))
                                .and(
                                        DSL.field(responsesTable.getName() + "." + TRANSACTION_TYPE).equal(TransactionType.AUTHORIZE.toString())
                                                .or(DSL.field(responsesTable.getName() + "." + TRANSACTION_TYPE).equal(TransactionType.PURCHASE.toString()))
                                )
                                .and(DSL.field(responsesTable.getName() + "." + KB_TENANT_ID).equal(kbTenantId.toString()))
                                .orderBy(DSL.field(responsesTable.getName() + "." + RECORD_ID).desc())
                                .limit(1)
                                .fetchOne();
                    }
                });
    }

    public IngenicoResponsesRecord getResponse(final String pspReference) throws SQLException {
        return execute(dataSource.getConnection(),
                new WithConnectionCallback<IngenicoResponsesRecord>() {
                    @Override
                    public IngenicoResponsesRecord withConnection(final Connection conn) throws SQLException {
                        return DSL.using(conn, dialect, settings)
                                .selectFrom(INGENICO_RESPONSES)
                                .where(INGENICO_RESPONSES.PSP_REFERENCE.equal(pspReference))
                                .orderBy(INGENICO_RESPONSES.RECORD_ID.desc())
                                // Can have multiple entries for 3D-S
                                .limit(1)
                                .fetchOne();
                    }
                });
    }

    private String getString(@Nullable final Iterable iterable) {
        if (iterable == null || !iterable.iterator().hasNext()) {
            return null;
        } else {
            return JOINER.join(Iterables.transform(iterable, Functions.toStringFunction()));
        }
    }

    private String getProperty(final String key, final PurchaseResult result) {
        return getProperty(key, result.getFormParameter());
    }

    private String getAdditionalData(final PurchaseResult result) throws SQLException {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        if (result.getAdditionalData() != null && !result.getAdditionalData().isEmpty()) {
            additionalDataMap.putAll(result.getAdditionalData());
        }
        if (result.getFormParameter() != null && !result.getFormParameter().isEmpty()) {
            additionalDataMap.putAll(result.getFormParameter());
        }
        if (additionalDataMap.isEmpty()) {
            return null;
        } else {
            return getAdditionalData(additionalDataMap);
        }
    }

    private String getAdditionalData(final PaymentModificationResponse response) throws SQLException {
        return getAdditionalData(response.getAdditionalData());
    }

    private String getAdditionalData(final Map additionalData) throws SQLException {
        if (additionalData == null || additionalData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(additionalData);
        } catch (final JsonProcessingException e) {
            throw new SQLException(e);
        }
    }

    public static Map fromAdditionalData(final String additionalData) {
        if (additionalData == null) {
            return ImmutableMap.of();
        }

        try {
            return objectMapper.readValue(additionalData, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
