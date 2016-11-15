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

import org.killbill.billing.catalog.api.Currency;

import java.math.BigDecimal;

public class PaymentData<I extends PaymentInfo> {

    private final Long amount;
    private final Currency currency;
    private final String paymentTransactionExternalKey;

    public PaymentData(final BigDecimal amount, final Currency currency, final String paymentTransactionExternalKey) {
        this.amount = amount.setScale(0, BigDecimal.ROUND_UP).longValueExact();
        this.currency = currency;
        this.paymentTransactionExternalKey = paymentTransactionExternalKey;
    }

    public Long getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getPaymentTransactionExternalKey() {
        return paymentTransactionExternalKey;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentData{");
        sb.append("amount=").append(amount);
        sb.append(", currency=").append(currency);
        sb.append(", paymentTransactionExternalKey='").append(paymentTransactionExternalKey).append('\'');
        sb.append('}');
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

        final PaymentData<?> that = (PaymentData<?>) o;

        if (amount != null ? !amount.equals(that.amount) : that.amount != null) {
            return false;
        }
        if (currency != that.currency) {
            return false;
        }
        return paymentTransactionExternalKey != null ? !paymentTransactionExternalKey.equals(that.paymentTransactionExternalKey) : that.paymentTransactionExternalKey != null;
    }

    @Override
    public int hashCode() {
        int result = amount != null ? amount.hashCode() : 0;
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (paymentTransactionExternalKey != null ? paymentTransactionExternalKey.hashCode() : 0);
        return result;
    }
}