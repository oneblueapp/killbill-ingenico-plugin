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
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.ingenico.client.payment.converter.PaymentInfoConverter;

import com.ingenico.connect.gateway.sdk.java.domain.definitions.BankAccountIban;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.SepaDirectDebitPaymentMethodSpecificInput;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.Debtor;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.MandateSepaDirectDebitWithoutCreditor;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenSepaDirectDebitWithoutCreditor;

public class SepaDirectDebitConverter extends PaymentInfoConverter<SepaDirectDebit> {

    @Override
    public boolean supportsPaymentInfo(final PaymentInfo type) {
        return type instanceof SepaDirectDebit;
    }

    @Override
    public CreatePaymentRequest convertPaymentInfoToPaymentRequest(final SepaDirectDebit paymentInfo) {
        final CreatePaymentRequest ingenicoRequest = super.convertPaymentInfoToPaymentRequest(paymentInfo);
        final SepaDirectDebitPaymentMethodSpecificInput sepaDirectDebit = new SepaDirectDebitPaymentMethodSpecificInput();
        //sepaDirectDebit.setToken(paymentInfo.getToken());
        //sepaDirectDebit.setIsRecurring(paymentInfo.getToken());
        //sepaDirectDebit.setRecurringPaymentSequenceIndicator(paymentInfo.getToken());
        //sepaDirectDebit.setDateCollect(paymentInfo.getToken());
        //sepaDirectDebit.setDirectDebitText(paymentInfo.getToken());
        sepaDirectDebit.setPaymentProductId(paymentInfo.getPaymentProductId());
        ingenicoRequest.setSepaDirectDebitPaymentMethodSpecificInput(sepaDirectDebit);

        return ingenicoRequest;
    }

    @Override
    public CreateTokenRequest convertPaymentInfoToCreateTokenRequest(final SepaDirectDebit paymentInfo) {

        MandateSepaDirectDebitWithoutCreditor mandate = new MandateSepaDirectDebitWithoutCreditor();
        final BankAccountIban bankAccountIban = new BankAccountIban();
        bankAccountIban.setIban(paymentInfo.getIban());
        mandate.setBankAccountIban(bankAccountIban);
        //mandateSepaDirectDebitWithoutCreditor.setCustomerContractIdentifier(paymentInfo.get);
        final Debtor debtor = new Debtor();
        debtor.setCity(paymentInfo.getCity());
        debtor.setState(paymentInfo.getStateOrProvince());
        debtor.setStreet(paymentInfo.getStreet());
        debtor.setCountryCode(paymentInfo.getPostalCode());
        debtor.setFirstName(paymentInfo.getSepaAccountHolder());
        debtor.setHouseNumber(paymentInfo.getHouseNumberOrName());
        mandate.setDebtor(debtor);

        //mandateSepaDirectDebitWithoutCreditor.setIsRecurring();
        //mandateSepaDirectDebitWithoutCreditor.setPreNotification();
        //        final MandateApproval mandateApproval = new MandateApproval();
        //        mandateApproval.setMandateSignatureDate();
        //        mandateApproval.setMandateSignaturePlace();
        //        mandateApproval.setMandateSigned();
        //        mandateSepaDirectDebitWithoutCreditor.setMandateApproval(mandateApproval);

        final CreateTokenRequest ingenicoRequest = super.convertPaymentInfoToCreateTokenRequest(paymentInfo);
        final TokenSepaDirectDebitWithoutCreditor sepaDirectDebit = new TokenSepaDirectDebitWithoutCreditor();
        sepaDirectDebit.setMandate(mandate);
        //sepaDirectDebit.setAlias();
        ingenicoRequest.setSepaDirectDebit(sepaDirectDebit);

        return ingenicoRequest;
    }
}
