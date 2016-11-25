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

import java.util.List;

import com.google.common.base.Optional;
import com.ingenico.connect.gateway.sdk.java.domain.errors.definitions.APIError;

import static com.google.common.base.Preconditions.checkNotNull;

public interface IngenicoCallResult<T> {

    Optional<T> getResult();

    long getDuration();

    String getPaymentId();

    String getStatus();

    Optional<IngenicoCallErrorStatus> getResponseStatus();

    Optional<String> getExceptionClass();

    Optional<String> getExceptionMessage();

    Optional<List<APIError>> getErrors();

    boolean receivedWellFormedResponse();

}

abstract class IngenicoCallBase<T> implements IngenicoCallResult<T>  {
    private final String paymentId;
    private final String status;

    public IngenicoCallBase(String paymentId, String status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    @Override
    public Optional<T> getResult() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDuration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPaymentId() {
        return paymentId;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Optional<IngenicoCallErrorStatus> getResponseStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> getExceptionClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<List<APIError>> getErrors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean receivedWellFormedResponse() {
        throw new UnsupportedOperationException();
    }
}

class SuccessfulIngenicoCall<T> extends IngenicoCallBase {

    private final T result;

    private final long duration;

    public SuccessfulIngenicoCall(final T result, long duration) {
        this(result, null, null, duration);
    }

    public SuccessfulIngenicoCall(final T result, String paymentId, String status, long duration) {
        super(paymentId, status);
        this.result = checkNotNull(result, "result");
        this.duration = duration;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.of(result);
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public Optional<IngenicoCallErrorStatus> getResponseStatus() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getExceptionClass() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.absent();
    }

    @Override
    public Optional<List<APIError>> getErrors() {
        return null;
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SuccessfulIngenicoCall{");
        sb.append("result=").append(result);
        sb.append(" }");
        return sb.toString();
    }
}

class UnSuccessfulIngenicoCall<T> extends IngenicoCallBase {

    private final IngenicoCallErrorStatus responseStatus;
    private final String exceptionClass;
    private final String exceptionMessage;
    private final List<APIError> errors;
    private long duration;

    UnSuccessfulIngenicoCall(final IngenicoCallErrorStatus responseStatus, final Throwable rootCause) {
        this(responseStatus, rootCause, null, null, null);
    }

    UnSuccessfulIngenicoCall(final IngenicoCallErrorStatus responseStatus, final Throwable rootCause, List<APIError> errors) {
        this(responseStatus, rootCause, errors, null, null);
    }

    UnSuccessfulIngenicoCall(final IngenicoCallErrorStatus responseStatus, final Throwable rootCause, List<APIError> errors, String paymentId, String status) {
        super(paymentId, status);
        this.responseStatus = responseStatus;
        this.exceptionClass = rootCause.getClass().getCanonicalName();
        this.exceptionMessage = rootCause.getMessage();
        this.errors = errors;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.absent();
    }

    @Override
    public long getDuration() {
        return duration;
    }

    public void setDuration(final long duration) {
        this.duration = duration;
    }

    @Override
    public Optional<IngenicoCallErrorStatus> getResponseStatus() {
        return Optional.of(responseStatus);
    }

    @Override
    public Optional<String> getExceptionClass() {
        return Optional.of(exceptionClass);
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.of(exceptionMessage);
    }

    @Override
    public Optional<List<APIError>> getErrors() {
        return Optional.of(errors);
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UnSuccessfulIngenicoCall{");
        sb.append("responseStatus=").append(responseStatus);
        sb.append(", exceptionMessage='").append(exceptionMessage).append('\'');
        sb.append(", exceptionClass='").append(exceptionClass).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
