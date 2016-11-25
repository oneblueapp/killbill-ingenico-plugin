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

import javax.xml.ws.soap.SOAPFaultException;

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
import com.ingenico.connect.gateway.sdk.java.domain.refund.RefundRequest;
import com.ingenico.connect.gateway.sdk.java.domain.refund.RefundResponse;
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

    public IngenicoCallResult<RefundResponse> refund(final String merchantAccount, final String paymentId, final RefundRequest modificationRequest) {
        return callIngenico(new IngenicoCall<MerchantClient, RefundResponse>() {
            @Override
            public RefundResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().refund(paymentId, modificationRequest);
            }
        });
    }

    public IngenicoCallResult<CancelPaymentResponse> cancel(final String merchantAccount, final  String paymentId) {
        return callIngenico(new IngenicoCall<MerchantClient, CancelPaymentResponse>() {
            @Override
            public CancelPaymentResponse apply(final MerchantClient client) throws ApiException {
                return client.payments().cancel(paymentId);
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
            List<APIError> errors = ((ValidationException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors);
        } else if (rootCause instanceof IllegalArgumentException) {
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause);
        } else if (rootCause instanceof DeclinedPaymentException) {
            List<APIError> errors = ((DeclinedPaymentException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors);
        } else if (rootCause instanceof DeclinedPayoutException) {
            List<APIError> errors = ((DeclinedPayoutException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors);
        } else if (rootCause instanceof DeclinedRefundException) {
            List<APIError> errors = ((DeclinedRefundException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors);
        } else if (rootCause instanceof AuthorizationException) {
            List<APIError> errors = ((AuthorizationException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause, errors);
        } else if (rootCause instanceof ReferenceException) {
            List<APIError> errors = ((ReferenceException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause, errors);
        } else if (rootCause instanceof IdempotenceException) {
            List<APIError> errors = ((IdempotenceException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(REQUEST_NOT_SEND, rootCause, errors);
        } else if (rootCause instanceof GlobalCollectException) {
            List<APIError> errors = ((GlobalCollectException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_INVALID, rootCause, errors);
        } else if (rootCause instanceof ApiException) {
            List<APIError> errors = ((ApiException)rootCause).getErrors();
            return new UnSuccessfulIngenicoCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors);
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
}
