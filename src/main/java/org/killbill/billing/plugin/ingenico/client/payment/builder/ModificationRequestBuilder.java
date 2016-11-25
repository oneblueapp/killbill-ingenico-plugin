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

package org.killbill.billing.plugin.ingenico.client.payment.builder;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;

import com.ingenico.connect.gateway.sdk.java.domain.definitions.AmountOfMoney;
import com.ingenico.connect.gateway.sdk.java.domain.payment.ApprovePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderApprovePayment;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderReferencesApprovePayment;

public class ModificationRequestBuilder extends RequestBuilder<ApprovePaymentRequest> {

    private final PaymentData paymentData;
    private final SplitSettlementData splitSettlementData;

    public ModificationRequestBuilder(final PaymentData paymentData,
                                      @Nullable final SplitSettlementData splitSettlementData) {
        super(new ApprovePaymentRequest());
//        final AnyType2AnyTypeMap map = new AnyType2AnyTypeMap();
//        request.setAdditionalData(map);

        this.paymentData = paymentData;
        this.splitSettlementData = splitSettlementData;
    }

    @Override
    public ApprovePaymentRequest build() {
        setReferences();
        setAmount();
        //setSplitSettlementData();

        return request;
    }

    private void setReferences() {
        final OrderApprovePayment order = new OrderApprovePayment();
        final OrderReferencesApprovePayment references = new OrderReferencesApprovePayment();
        references.setMerchantReference(paymentData.getPaymentTransactionExternalKey());
        order.setReferences(references);

        request.setOrder(order);
    }

    private void setAmount() {
        if (paymentData.getAmount() == null || paymentData.getCurrency() == null) {
            return;
        }

        final String currency = paymentData.getCurrency().name();
        final AmountOfMoney amount = new AmountOfMoney();
        amount.setAmount(toMinorUnits(paymentData.getAmount(), currency));
        amount.setCurrencyCode(currency);
    }

//    private void setSplitSettlementData() {
//        if (splitSettlementData != null) {
//            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
//            request.getAdditionalData().getEntry().addAll(entries);
//        }
//    }
}
