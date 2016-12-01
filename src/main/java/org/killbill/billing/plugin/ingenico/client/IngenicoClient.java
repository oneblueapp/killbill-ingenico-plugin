package org.killbill.billing.plugin.ingenico.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.PaymentInfo;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.ingenico.client.payment.builder.IngenicoRequestFactory;
import org.killbill.billing.plugin.ingenico.client.payment.service.BaseIngenicoPaymentServiceProviderPort;
import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus;
import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallResult;
import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoPaymentRequestSender;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.ingenico.connect.gateway.sdk.java.domain.payment.ApprovePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CancelPaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.PaymentApprovalResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.PaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Payment;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PaymentOutput;
import com.ingenico.connect.gateway.sdk.java.domain.refund.RefundRequest;
import com.ingenico.connect.gateway.sdk.java.domain.refund.RefundResponse;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenResponse;

import static org.killbill.billing.plugin.ingenico.client.model.PurchaseResult.EXCEPTION_CLASS;
import static org.killbill.billing.plugin.ingenico.client.model.PurchaseResult.EXCEPTION_MESSAGE;
import static org.killbill.billing.plugin.ingenico.client.model.PurchaseResult.INGENICO_CALL_ERROR_STATUS;
import static org.killbill.billing.plugin.ingenico.client.model.PurchaseResult.UNKNOWN;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class IngenicoClient extends BaseIngenicoPaymentServiceProviderPort implements Closeable {

    private static final java.lang.String HMAC_ALGORITHM = "";
    private IngenicoRequestFactory ingenicoRequestFactory;
    private IngenicoPaymentRequestSender ingenicoPaymentRequestSender;

    public IngenicoClient(final IngenicoRequestFactory ingenicoRequestFactory, IngenicoPaymentRequestSender ingenicoPaymentRequestSender) {
        this.ingenicoRequestFactory = ingenicoRequestFactory;
        this.ingenicoPaymentRequestSender = ingenicoPaymentRequestSender;
    }

    @Override
    public void close() throws IOException {
        ingenicoPaymentRequestSender.close();
    }

    public PurchaseResult create(TransactionType transactionType, PaymentData<Card> paymentData, UserData userData, final SplitSettlementData splitSettlementData) {
        return authorisePurchaseOrCredit(transactionType, paymentData, userData, splitSettlementData);
    }

    private PurchaseResult authorisePurchaseOrCredit(TransactionType transactionType, final PaymentData<Card> paymentData, final UserData userData, final SplitSettlementData splitSettlementData) {CreatePaymentRequest body = ingenicoRequestFactory.createPaymentRequest(paymentData, userData, splitSettlementData);
        final IngenicoCallResult<CreatePaymentResponse> ingenicoCallResult = ingenicoPaymentRequestSender.create(body);

        if (!ingenicoCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase(transactionType.toString(), userData, paymentData, ingenicoCallResult);
        }

        final CreatePaymentResponse result = ingenicoCallResult.getResult().get();
        Payment paymentResponse = result.getPayment();

        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(paymentResponse.getStatus(), transactionType);

        PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();

        final Map<String, String> additionalData = new HashMap<String, String>();

        return new PurchaseResult(
                paymentServiceProviderResult,
                paymentResponse.getId(),
                paymentResponse.getStatus(),
                paymentOutput.getReferences().getPaymentReference(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getAuthorisationCode(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getAvsResult(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getCvvResult(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getFraudServiceResult(),
                paymentData.getPaymentTransactionExternalKey(),
                additionalData);
    }

    private PurchaseResult handleTechnicalFailureAtPurchase(final String transactionType, final UserData userData, final PaymentData paymentData, final IngenicoCallResult<CreatePaymentResponse> ingenicoCall) {
        logTransactionError(transactionType, userData, paymentData, ingenicoCall);
        return new PurchaseResult(paymentData.getPaymentTransactionExternalKey(), ingenicoCall);
    }

    public PaymentModificationResponse capture(final PaymentData paymentData, final String paymentId, final SplitSettlementData splitSettlementData) {
        ApprovePaymentRequest body = ingenicoRequestFactory.approvePaymentRequest(paymentData, paymentId, splitSettlementData);
        final IngenicoCallResult<PaymentApprovalResponse> ingenicoCallResult = ingenicoPaymentRequestSender.approve(paymentId, body);

        if (!ingenicoCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtApprove(paymentId, paymentData, ingenicoCallResult);
        }

        PaymentApprovalResponse result = ingenicoCallResult.getResult().get();
        Payment paymentResponse = result.getPayment();

        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(paymentResponse.getStatus(), null);

        return new PaymentModificationResponse<PaymentApprovalResponse>(paymentServiceProviderResult, paymentResponse.getStatus(), paymentId);
    }

    private PaymentModificationResponse handleTechnicalFailureAtApprove(final String paymentId, final PaymentData paymentData, final IngenicoCallResult<PaymentApprovalResponse> ingenicoCall) {
        logTransactionError("capture", paymentId, paymentData, ingenicoCall);
        return new PaymentModificationResponse(paymentId, ingenicoCall, getModificationAdditionalErrorData(ingenicoCall));
    }

    public PaymentModificationResponse cancel(final String paymentId, final SplitSettlementData splitSettlementData) {
        final IngenicoCallResult<CancelPaymentResponse> ingenicoCallResult = ingenicoPaymentRequestSender.cancel(paymentId);
        if (!ingenicoCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtCancel(paymentId, ingenicoCallResult);
        }
        return null;
    }

    private PaymentModificationResponse handleTechnicalFailureAtCancel(final String paymentId, final IngenicoCallResult<CancelPaymentResponse> ingenicoCall) {
        logTransactionError("cancel", paymentId, null, ingenicoCall);
        return new PaymentModificationResponse(paymentId, ingenicoCall, getModificationAdditionalErrorData(ingenicoCall));
    }

    public PaymentModificationResponse refund(final PaymentData paymentData, final String paymentId, final SplitSettlementData splitSettlementData) {
        final RefundRequest body = null;
        final IngenicoCallResult<RefundResponse> ingenicoCallResult = ingenicoPaymentRequestSender.refund(paymentId, body);
        if (!ingenicoCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtRefund(paymentId, paymentData, ingenicoCallResult);
        }
        return null;
    }

    private PaymentModificationResponse handleTechnicalFailureAtRefund(final String paymentId, final PaymentData paymentData, final IngenicoCallResult<RefundResponse> ingenicoCall) {
        logTransactionError("refund", paymentId, null, ingenicoCall);
        return new PaymentModificationResponse(paymentId, ingenicoCall, getModificationAdditionalErrorData(ingenicoCall));
    }

    private Map<Object,Object> getModificationAdditionalErrorData(final IngenicoCallResult<?> ingenicoCall) {
        final Map<Object, Object> additionalDataMap = new HashMap<Object, Object>();
        final Optional<IngenicoCallErrorStatus> responseStatus = ingenicoCall.getResponseStatus();
        additionalDataMap.putAll(ImmutableMap.<Object, Object>of(INGENICO_CALL_ERROR_STATUS, responseStatus.isPresent() ? responseStatus.get() : "",
                                                                 EXCEPTION_CLASS, ingenicoCall.getExceptionClass().or(UNKNOWN),
                                                                 EXCEPTION_MESSAGE, ingenicoCall.getExceptionMessage().or(UNKNOWN)));

        return additionalDataMap;
    }

    public String tokenizeCreditCard(PaymentInfo paymentInfo, UserData userData) {
        CreateTokenRequest body = ingenicoRequestFactory.createTokenRequest(paymentInfo, userData);
        final IngenicoCallResult<CreateTokenResponse> ingenicoCallResult = ingenicoPaymentRequestSender.createToken(body);
        if (!ingenicoCallResult.receivedWellFormedResponse()) {
            return null;
        }

        return ingenicoCallResult.getResult().get().getToken();
    }

    public PaymentModificationResponse getPaymentInfo(final String paymentId, TransactionType transactionType) {
        final IngenicoCallResult<PaymentResponse> ingenicoCallResult = ingenicoPaymentRequestSender.get(paymentId);
        if (!ingenicoCallResult.receivedWellFormedResponse()) {
            return null;
        }

        PaymentResponse result = ingenicoCallResult.getResult().get();
        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getStatus(), transactionType);

        return new PaymentModificationResponse(paymentServiceProviderResult, result.getStatus(), result.getId());
    }
}
