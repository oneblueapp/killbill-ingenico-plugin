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

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus;
import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallResult;

import com.google.common.base.Optional;

public class PaymentModificationResponse<T> {

    private final IngenicoCallErrorStatus ingenicoCallErrorStatus;
    private final Map<Object, Object> additionalData;
    private final String paymentId;
    private final String status;

    public PaymentModificationResponse(final String status, final String paymentId, final Map<Object, Object> additionalData) {
        this(paymentId, status, null, additionalData);
    }

    public PaymentModificationResponse(final String paymentId, final IngenicoCallResult<T> ingenicoCallResult, final Map<Object, Object> additionalData) {
        this(paymentId, null, ingenicoCallResult.getResponseStatus().orNull(), additionalData);
    }

    private PaymentModificationResponse(final String paymentId,
                                        final String status,
                                        @Nullable final IngenicoCallErrorStatus adyenCallErrorStatus,
                                        final Map<Object, Object> additionalData) {
        this.paymentId = paymentId;
        this.status = status;
        this.ingenicoCallErrorStatus = adyenCallErrorStatus;
        this.additionalData = additionalData;
    }

    /**
     * True if we received a well formed soap status from adyen.
     */
    public boolean isTechnicallySuccessful() {
        return !getIngenicoCallErrorStatus().isPresent();
    }

    public Map<Object, Object> getAdditionalData() {
        return additionalData;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }

    public Optional<IngenicoCallErrorStatus> getIngenicoCallErrorStatus() {
        return Optional.fromNullable(ingenicoCallErrorStatus);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentModificationResponse{");
        sb.append("ingenicoCallErrorStatus=").append(ingenicoCallErrorStatus);
        sb.append(", paymentId='").append(paymentId).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", additionalData={");
        // Make sure to escape values, as they may contain spaces
        final Iterator<Object> iterator = additionalData.keySet().iterator();
        if (iterator.hasNext()) {
            final Object key = iterator.next();
            sb.append(key).append("='").append(additionalData.get(key)).append("'");
        }
        while (iterator.hasNext()) {
            final Object key = iterator.next();
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

        final PaymentModificationResponse that = (PaymentModificationResponse) o;

        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
            return false;
        }
        if (paymentId != null ? !paymentId.equals(that.paymentId) : that.paymentId != null) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }
        return !(ingenicoCallErrorStatus != null ? !ingenicoCallErrorStatus.equals(that.ingenicoCallErrorStatus) : that.ingenicoCallErrorStatus != null);

    }

    @Override
    public int hashCode() {
        int result = additionalData != null ? additionalData.hashCode() : 0;
        result = 31 * result + (paymentId != null ? paymentId.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (ingenicoCallErrorStatus != null ? ingenicoCallErrorStatus.hashCode() : 0);
        return result;
    }
}
