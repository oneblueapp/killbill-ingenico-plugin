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

package org.killbill.billing.plugin.ingenico.client.payment.converter.impl;

import org.killbill.billing.plugin.ingenico.client.model.PaymentInfo;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.ingenico.client.payment.converter.PaymentInfoConverter;

import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.CardPaymentMethodSpecificInput;

public class RecurringConverter extends PaymentInfoConverter<Recurring> {

    @Override
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return paymentInfo instanceof Recurring;
    }

    @Override
    public CreatePaymentRequest convertPaymentInfoToPaymentRequest(final Recurring paymentInfo) {
        final CreatePaymentRequest paymentRequest = super.convertPaymentInfoToPaymentRequest(paymentInfo);

        setCvcForOneClick(paymentInfo, paymentRequest);

        return paymentRequest;
    }

    private void setCvcForOneClick(final Recurring paymentInfo, final CreatePaymentRequest paymentRequest) {
        final com.ingenico.connect.gateway.sdk.java.domain.definitions.Card card = new com.ingenico.connect.gateway.sdk.java.domain.definitions.Card();
        card.setCvv(paymentInfo.getCvc());

        final CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
//        cardPaymentMethodSpecificInput.setIsRecurring(true);
//        cardPaymentMethodSpecificInput.setRecurringPaymentSequenceIndicator("first|recurring");
//        cardPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getPaymentProductId());
        cardPaymentMethodSpecificInput.setCard(card);
        cardPaymentMethodSpecificInput.setToken(paymentInfo.getRecurringDetailReference());
        paymentRequest.setCardPaymentMethodSpecificInput(cardPaymentMethodSpecificInput);
    }
}
