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
import com.google.common.collect.ImmutableMap;

import static org.killbill.billing.plugin.ingenico.client.model.PurchaseResult.INGENICO_CALL_ERROR_STATUS;

public class PaymentModificationResponse<T> {

    private final IngenicoCallErrorStatus ingenicoCallErrorStatus;
    private final Map<Object, Object> additionalData;
    private final String merchantReference;
    private final String response;

    public PaymentModificationResponse(final String response, final String merchantReference, final Map<Object, Object> additionalData) {
        this(merchantReference, response, null, additionalData);
    }

    public PaymentModificationResponse(final String merchantReference, final IngenicoCallResult<T> ingenicoCallResult, final Map<Object, Object> additionalData) {
        this(merchantReference, null, ingenicoCallResult.getResponseStatus().orNull(), additionalData);
    }

    private PaymentModificationResponse(final String merchantReference,
                                        final String response,
                                        @Nullable final IngenicoCallErrorStatus adyenCallErrorStatus,
                                        final Map<Object, Object> additionalData) {
        this.merchantReference = merchantReference;
        this.response = response;
        this.ingenicoCallErrorStatus = adyenCallErrorStatus;
        this.additionalData = additionalData;
    }

    /**
     * True if we received a well formed soap response from adyen.
     */
    public boolean isTechnicallySuccessful() {
        return !getIngenicoCallErrorStatus().isPresent();
    }

    public Map<Object, Object> getAdditionalData() {
        return additionalData;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public String getResponse() {
        return response;
    }

    public Optional<IngenicoCallErrorStatus> getIngenicoCallErrorStatus() {
        return Optional.fromNullable(ingenicoCallErrorStatus);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentModificationResponse{");
        sb.append("ingenicoCallErrorStatus=").append(ingenicoCallErrorStatus);
        sb.append(", merchantReference='").append(merchantReference).append('\'');
        sb.append(", response='").append(response).append('\'');
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
        if (merchantReference != null ? !merchantReference.equals(that.merchantReference) : that.merchantReference != null) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (response != null ? !response.equals(that.response) : that.response != null) {
            return false;
        }
        return !(ingenicoCallErrorStatus != null ? !ingenicoCallErrorStatus.equals(that.ingenicoCallErrorStatus) : that.ingenicoCallErrorStatus != null);

    }

    @Override
    public int hashCode() {
        int result = additionalData != null ? additionalData.hashCode() : 0;
        result = 31 * result + (merchantReference != null ? merchantReference.hashCode() : 0);
        result = 31 * result + (response != null ? response.hashCode() : 0);
        result = 31 * result + (ingenicoCallErrorStatus != null ? ingenicoCallErrorStatus.hashCode() : 0);
        return result;
    }
}
