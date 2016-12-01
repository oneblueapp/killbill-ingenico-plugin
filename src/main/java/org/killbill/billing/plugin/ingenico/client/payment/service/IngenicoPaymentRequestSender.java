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

package org.killbill.billing.plugin.ingenico.client.payment.service;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

import org.killbill.billing.plugin.ingenico.client.IngenicoClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.ingenico.connect.gateway.sdk.java.ApiException;
import com.ingenico.connect.gateway.sdk.java.AuthorizationException;
import com.ingenico.connect.gateway.sdk.java.DeclinedPaymentException;
import com.ingenico.connect.gateway.sdk.java.DeclinedPayoutException;
import com.ingenico.connect.gateway.sdk.java.DeclinedRefundException;
import com.ingenico.connect.gateway.sdk.java.GlobalCollectException;
import com.ingenico.connect.gateway.sdk.java.IdempotenceException;
import com.ingenico.connect.gateway.sdk.java.ReferenceException;
import com.ingenico.connect.gateway.sdk.java.ValidationException;
import com.ingenico.connect.gateway.sdk.java.domain.errors.definitions.APIError;
import com.ingenico.connect.gateway.sdk.java.domain.payment.ApprovePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CancelPaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.PaymentApprovalResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.PaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.CreatePaymentResult;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Payment;
import com.ingenico.connect.gateway.sdk.java.domain.payout.definitions.PayoutResult;
import com.ingenico.connect.gateway.sdk.java.domain.refund.RefundRequest;
import com.ingenico.connect.gateway.sdk.java.domain.refund.RefundResponse;
import com.ingenico.connect.gateway.sdk.java.domain.refund.definitions.RefundResult;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenResponse;
import com.ingenico.connect.gateway.sdk.java.merchant.MerchantClient;

import static org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus.REQUEST_NOT_SEND;
import static org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus.RESPONSE_ABOUT_INVALID_REQUEST;
import static org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus.RESPONSE_INVALID;
import static org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus.RESPONSE_NOT_RECEIVED;
import static org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus.UNKNOWN_FAILURE;

public class IngenicoPaymentRequestSender implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(IngenicoPaymentRequestSender.class);

    private final IngenicoClientRegistry ingenicoClientRegistry;
    private List<APIError> errors;
    private String status;

    public IngenicoPaymentRequestSender(final IngenicoClientRegistry ingenicoClientRegistry) {
        this.ingenicoClientRegistry = ingenicoClientRegistry;
    }

    public IngenicoCallResult<CreatePaymentResponse> create(final CreatePaymentRequest request) {
        return callIngenico(new IngenicoCall<MerchantClient, CreatePaymentResponse>() {
            @Override
            public CreatePaymentResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().create(request);
            }
        });
    }

    public IngenicoCallResult<PaymentApprovalResponse> approve(final  String paymentId, final ApprovePaymentRequest modificationRequest) {
        return callIngenico(new IngenicoCall<MerchantClient, PaymentApprovalResponse>() {
            @Override
            public PaymentApprovalResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().approve(paymentId, modificationRequest);
            }
        });
    }

    public IngenicoCallResult<PaymentResponse> get(final String paymentId) {
        return callIngenico(new IngenicoCall<MerchantClient, PaymentResponse>() {
            @Override
            public PaymentResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().get(paymentId);
            }
        });
    }

    public IngenicoCallResult<RefundResponse> refund(final String paymentId, final RefundRequest modificationRequest) {
        return callIngenico(new IngenicoCall<MerchantClient, RefundResponse>() {
            @Override
            public RefundResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().refund(paymentId, modificationRequest);
            }
        });
    }

    public IngenicoCallResult<CancelPaymentResponse> cancel(final  String paymentId) {
        return callIngenico(new IngenicoCall<MerchantClient, CancelPaymentResponse>() {
            @Override
            public CancelPaymentResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().cancel(paymentId);
            }
        });
    }

    public IngenicoCallResult<CreateTokenResponse> createToken(final CreateTokenRequest createTokenRequest) {
        return callIngenico(new IngenicoCall<MerchantClient, CreateTokenResponse>() {
            @Override
            public CreateTokenResponse apply(final MerchantClient client) throws ApiException {
                return client.tokens().create(createTokenRequest);
            }
        });
    }

    private <T> IngenicoCallResult<T> callIngenico(final IngenicoCall<MerchantClient, T> ingenicoCall) {
        final long startTime = System.currentTimeMillis();
        try {
            final MerchantClient client = ingenicoClientRegistry.getMerchantClient();
            final T result = ingenicoCall.apply(client);

            final long duration = System.currentTimeMillis() - startTime;
            return new SuccessfulIngenicoCall<T>(result, duration);
        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.warn("Exception during Ingenico request", e);

            final UnSuccessfulIngenicoCall<T> unsuccessfulResult = mapExceptionToCallResult(e);
            unsuccessfulResult.setDuration(duration);
            return unsuccessfulResult;
        }
    }

    /**
     * Educated guess approach to transform CXF exceptions into error status codes.
     * In the future if we encounter further different cases it makes sense to change this if/else structure to a map with lookup.
     */
    private <T> UnSuccessfulIngenicoCall<T> mapExceptionToCallResult(final Exception e) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(e);
        final String errorMessage = rootCause.getMessage();
        if (rootCause instanceof ConnectException) {
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause);
        } else if (rootCause instanceof SocketTimeoutException) {
            // read timeout
            if (errorMessage.contains("Read timed out")) {
                return new UnSuccessfulIngenicoCall<T>(RESPONSE_NOT_RECEIVED, rootCause);
            } else if (errorMessage.contains("Unexpected end of file from server")) {
                return new UnSuccessfulIngenicoCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof SocketException) {
            if (errorMessage.contains("Unexpected end of file from server")) {
                return new UnSuccessfulIngenicoCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof UnknownHostException) {
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause);
        } else if (rootCause instanceof ValidationException) {
            List<APIError> errors = ((ValidationException) rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors);
        } else if (rootCause instanceof IllegalArgumentException) {
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause);
        } else if (rootCause instanceof DeclinedPaymentException) {
            IngenicoErrors errors = parseDeclinedPayment((DeclinedPaymentException) rootCause);
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors.getErrors(), errors.getPaymentId(), errors.getStatus());
        } else if (rootCause instanceof DeclinedPayoutException) {
            IngenicoErrors errors = parseDeclinedPayout((DeclinedPayoutException) rootCause);
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors.getErrors(), errors.getPaymentId(), errors.getStatus());
        } else if (rootCause instanceof DeclinedRefundException) {
            IngenicoErrors errors = parseDeclinedRefund((DeclinedRefundException) rootCause);
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors.getErrors(), errors.getPaymentId(), errors.getStatus());
        } else if (rootCause instanceof AuthorizationException) {
            List<APIError> errors = ((AuthorizationException) rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause, errors);
        } else if (rootCause instanceof ReferenceException) {
            List<APIError> errors = ((ReferenceException) rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause, errors);
        } else if (rootCause instanceof IdempotenceException) {
            List<APIError> errors = ((IdempotenceException) rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause, errors);
        } else if (rootCause instanceof GlobalCollectException) {
            List<APIError> errors = ((GlobalCollectException) rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_INVALID, rootCause, errors);
        } else if (rootCause instanceof ApiException) {
            IngenicoErrors errors = parseApiError((ApiException) rootCause);
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors.getErrors(), errors.getPaymentId(), errors.getStatus());
        } else if (rootCause instanceof IOException) {
            if (errorMessage.contains("Invalid Http response")) {
                // unparsable data as response
                return new UnSuccessfulIngenicoCall<T>(RESPONSE_INVALID, rootCause);
            } else if (errorMessage.contains("Bogus chunk size")) {
                return new UnSuccessfulIngenicoCall<T>(RESPONSE_INVALID, rootCause);
            }
        }

        return new UnSuccessfulIngenicoCall<T>(UNKNOWN_FAILURE, rootCause);
    }

    @Override
    public void close() throws IOException {
        ingenicoClientRegistry.close();
    }

    private interface IngenicoCall<T, R> {

        R apply(T t) throws ApiException;
    }

    private static class IngenicoErrors {
        final private List<APIError> errors;
        final private String status;
        final private String paymentId;

        public IngenicoErrors(final List<APIError> errors) {
            this(errors, null, null);
        }

        private IngenicoErrors(final List<APIError> errors, final String status, final String paymentId) {
            this.errors = errors;
            this.status = status;
            this.paymentId = paymentId;
        }

        public List<APIError> getErrors() {
            return errors;
        }

        public String getStatus() {
            return status;
        }

        public String getPaymentId() {
            return paymentId;
        }
    }

    private IngenicoErrors parseDeclinedPayment(DeclinedPaymentException e) {
        List<APIError> errors = e.getErrors();
        final CreatePaymentResult paymentResult = e.getCreatePaymentResult();
        Payment payment = paymentResult.getPayment();

        return new IngenicoErrors(errors, payment.getStatus(), payment.getId());
    }

    private IngenicoErrors parseDeclinedPayout(final DeclinedPayoutException e) {
        List<APIError> errors = e.getErrors();
        final PayoutResult paymentResult = e.getPayoutResult();
        return new IngenicoErrors(errors, paymentResult.getStatus(), paymentResult.getId());
    }

    private IngenicoErrors parseDeclinedRefund(final DeclinedRefundException e) {
        List<APIError> errors = e.getErrors();
        final RefundResult paymentResult = e.getRefundResult();
        return new IngenicoErrors(errors, paymentResult.getStatus(), paymentResult.getId());
    }

    private IngenicoErrors parseApiError(final ApiException e) {
        List<APIError> errors = e.getErrors();
        return new IngenicoErrors(errors);
    }
}
