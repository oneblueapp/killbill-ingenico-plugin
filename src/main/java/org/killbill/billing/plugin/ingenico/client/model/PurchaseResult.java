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

package org.killbill.billing.plugin.ingenico.client.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.ingenico.connect.gateway.sdk.java.domain.errors.definitions.APIError;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentResponse;

import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus;
import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallResult;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PurchaseResult {

    public static final String INGENICO_CALL_ERROR_STATUS = "ingenicoCallErrorStatus";
    public static final String EXCEPTION_CLASS = "exceptionClass";
    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String UNKNOWN = "";

    private final Optional<PaymentServiceProviderResult> result;
    private final String paymentId;
    private final String status;
    private final String paymentReference;
    private final String authorisationCode;
    private final String paymentTransactionExternalKey;
    private final String avsResult;
    private final String cvvResult;
    private final String fraudServiceResult;
    private final IngenicoCallErrorStatus ingenicoCallErrorStatus;
    private final Map<String, String> additionalData;
    private final String errorCode;
    private final String errorMessage;

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String paymentId,
                          final String status,
                          final String paymentReference,
                          final String authorisationCode,
                          final String avsResult,
                          final String cvvResult,
                          final String fraudServiceResult,
                          final String paymentTransactionExternalKey,
                          final Map<String, String> additionalData) {
        this(Optional.of(result), paymentId, status, paymentReference, authorisationCode, null, null, avsResult, cvvResult, fraudServiceResult, paymentTransactionExternalKey, null, additionalData);
    }

    public PurchaseResult(final String paymentTransactionExternalKey,
                          final IngenicoCallResult<CreatePaymentResponse> ingenicoCallResult) {

        this(Optional.<PaymentServiceProviderResult>absent(),
             ingenicoCallResult.getPaymentId(),
             ingenicoCallResult.getStatus(),
             null,
             null,
             ingenicoCallResult.getErrors().isPresent() &&  ingenicoCallResult.getErrors().get().size() > 0 ?  ingenicoCallResult.getErrors().get().get(0).getCode() : null,
             ingenicoCallResult.getErrors().isPresent() &&  ingenicoCallResult.getErrors().get().size() > 0 ?  ingenicoCallResult.getErrors().get().get(0).getMessage() : null,
             null,
             null,
             null,
             paymentTransactionExternalKey,
             ingenicoCallResult.getResponseStatus().isPresent() ? ingenicoCallResult.getResponseStatus().get() : null,
             ImmutableMap.<String, String>of(INGENICO_CALL_ERROR_STATUS, ingenicoCallResult.getResponseStatus().isPresent() ? ingenicoCallResult.getResponseStatus().get().name() : UNKNOWN,
                                             EXCEPTION_CLASS, ingenicoCallResult.getExceptionClass().isPresent() ? ingenicoCallResult.getExceptionClass().get() : UNKNOWN,
                                             EXCEPTION_MESSAGE, ingenicoCallResult.getExceptionMessage().isPresent() ? ingenicoCallResult.getExceptionMessage().get() : UNKNOWN));
    }


    public PurchaseResult(final Optional<PaymentServiceProviderResult> result,
                          final String paymentId,
                          final String status,
                          final String paymentReference,
                          final String authorisationCode,
                          final String errorCode,
                          final String errorMessage,
                          final String avsResult,
                          final String cvvResult,
                          final String fraudServiceResult,
                          final String paymentTransactionExternalKey,
                          @Nullable final IngenicoCallErrorStatus ingenicoCallErrorStatus,
                          final Map<String, String> additionalData) {
        this.ingenicoCallErrorStatus = ingenicoCallErrorStatus;
        this.result = result;
        this.paymentId = paymentId;
        this.status = status;
        this.paymentReference = paymentReference;
        this.authorisationCode = authorisationCode;
        this.avsResult = avsResult;
        this.cvvResult = cvvResult;
        this.fraudServiceResult = fraudServiceResult;
        this.paymentTransactionExternalKey = paymentTransactionExternalKey;
        this.additionalData = additionalData;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Optional<IngenicoCallErrorStatus> getIngenicoCallErrorStatus() {
        return Optional.fromNullable(ingenicoCallErrorStatus);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PurchaseResult{");
        sb.append("result='").append(result.isPresent() ? result.get() : null).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", errorCode='").append(errorCode).append('\'');
        sb.append(", errorMessage='").append(errorMessage).append('\'');
        sb.append(", paymentId='").append(paymentId).append('\'');
        sb.append(", paymentReference='").append(paymentReference).append('\'');
        sb.append(", authorisationCode='").append(authorisationCode).append('\'');
        sb.append(", avsResult='").append(avsResult).append('\'');
        sb.append(", cvvResult='").append(cvvResult).append('\'');
        sb.append(", fraudServiceResult='").append(fraudServiceResult).append('\'');
        sb.append(", paymentTransactionExternalKey='").append(paymentTransactionExternalKey).append('\'');
        sb.append(", ingenicoCallErrorStatus=").append(ingenicoCallErrorStatus);
        sb.append(", additionalData={");
        // Make sure to escape values, as they may contain spaces (e.g. avsResult='4 AVS not supported for this card type')
        final Iterator<String> iterator = additionalData.keySet().iterator();
        if (iterator.hasNext()) {
            final String key = iterator.next();
            sb.append(key).append("='").append(additionalData.get(key)).append("'");
        }
        while (iterator.hasNext()) {
            final String key = iterator.next();
            sb.append(", ")
              .append(key)
              .append("='")
              .append(additionalData.get(key))
              .append("'");
        }
        sb.append("}}");
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PurchaseResult that = (PurchaseResult) o;

        if (result != null ? !result.equals(that.result) : that.result != null) {
            return false;
        }
        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }
        if (paymentId != null ? !paymentId.equals(that.paymentId) : that.paymentId != null) {
            return false;
        }
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) {
            return false;
        }
        if (errorCode != null ? !errorCode.equals(that.errorCode) : that.errorCode != null) {
            return false;
        }
        if (paymentReference != null ? !paymentReference.equals(that.paymentReference) : that.paymentReference != null) {
            return false;
        }
        if (authorisationCode != null ? !authorisationCode.equals(that.authorisationCode) : that.authorisationCode != null) {
            return false;
        }
        if (avsResult != null ? !avsResult.equals(that.avsResult) : that.avsResult != null) {
            return false;
        }
        if (cvvResult != null ? !cvvResult.equals(that.cvvResult) : that.cvvResult != null) {
            return false;
        }
        if (fraudServiceResult != null ? !fraudServiceResult.equals(that.fraudServiceResult) : that.fraudServiceResult != null) {
            return false;
        }
        if (paymentTransactionExternalKey != null ? !paymentTransactionExternalKey.equals(that.paymentTransactionExternalKey) : that.paymentTransactionExternalKey != null) {
            return false;
        }
        if (ingenicoCallErrorStatus != null ? !ingenicoCallErrorStatus.equals(that.ingenicoCallErrorStatus) : that.ingenicoCallErrorStatus != null) {
            return false;
        }
        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result1 = result != null ? result.hashCode() : 0;
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (paymentId != null ? paymentId.hashCode() : 0);
        result1 = 31 * result1 + (errorMessage != null ? errorMessage.hashCode() : 0);
        result1 = 31 * result1 + (errorCode != null ? errorCode.hashCode() : 0);
        result1 = 31 * result1 + (paymentReference != null ? paymentReference.hashCode() : 0);
        result1 = 31 * result1 + (authorisationCode != null ? authorisationCode.hashCode() : 0);
        result1 = 31 * result1 + (avsResult != null ? avsResult.hashCode() : 0);
        result1 = 31 * result1 + (cvvResult != null ? cvvResult.hashCode() : 0);
        result1 = 31 * result1 + (fraudServiceResult != null ? fraudServiceResult.hashCode() : 0);
        result1 = 31 * result1 + (paymentTransactionExternalKey != null ? paymentTransactionExternalKey.hashCode() : 0);
        result1 = 31 * result1 + (ingenicoCallErrorStatus != null ? ingenicoCallErrorStatus.hashCode() : 0);
        result1 = 31 * result1 + (additionalData != null ? additionalData.hashCode() : 0);
        return result1;
    }


    public Optional<PaymentServiceProviderResult> getResult() {
        return result;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getAuthorizationCode() {
        return authorisationCode;
    }

    public String getFraudAvsResult() {
        return avsResult;
    }

    public String getFraudCvvResult() {
        return cvvResult;
    }

    public String getFraudResult() {
        return fraudServiceResult;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPaymentTransactionExternalKey() {
        return paymentTransactionExternalKey;
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }
}
