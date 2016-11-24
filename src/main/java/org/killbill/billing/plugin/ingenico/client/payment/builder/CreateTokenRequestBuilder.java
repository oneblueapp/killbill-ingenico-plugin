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
import org.killbill.billing.plugin.ingenico.client.model.PaymentInfo;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.payment.converter.PaymentInfoConverterManagement;

import com.ingenico.connect.gateway.sdk.java.domain.definitions.Address;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.AmountOfMoney;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.CompanyInformation;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.ContactDetailsBase;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.AddressPersonal;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.ContactDetails;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Customer;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Order;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderReferences;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PersonalInformation;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PersonalName;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.ContactDetailsToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.CustomerToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.CustomerTokenWithContactDetails;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.MandateSepaDirectDebitWithoutCreditor;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.PersonalInformationToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.PersonalNameToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenCard;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenSepaDirectDebitWithoutCreditor;

public class CreateTokenRequestBuilder extends RequestBuilder<CreateTokenRequest> {

    private final PaymentInfo paymentInfo;
    private final UserData userData;

    public CreateTokenRequestBuilder(final PaymentInfo paymentInfo,
                                     final UserData userData,
                                     @Nullable final SplitSettlementData splitSettlementData,
                                     final PaymentInfoConverterManagement paymentInfoConverterManagement) {
        super(paymentInfoConverterManagement.convertPaymentInfoToCreateTokenRequest(paymentInfo));
        this.paymentInfo = paymentInfo;
        this.userData = userData;
    }

    @Override
    public CreateTokenRequest build() {
        setShopperData();

        return request;
    }


    private void setShopperData() {
        ContactDetailsBase contactDetailsBase = new ContactDetailsBase();
        contactDetailsBase.setEmailAddress(userData.getShopperEmail());
        contactDetailsBase.setEmailMessageType("html");

        ContactDetails contactDetails = (ContactDetails)contactDetailsBase;
        contactDetails.setPhoneNumber(userData.getTelephoneNumber());

        PersonalNameToken personalNameToken = new PersonalNameToken();
        personalNameToken.setFirstName(userData.getFirstName());
        personalNameToken.setSurname(userData.getLastName());

        CompanyInformation companyInformation = new CompanyInformation();
        companyInformation.setName(userData.getCompanyName());

        PersonalInformationToken personalInformationToken = new PersonalInformationToken();
        personalInformationToken.setName(personalNameToken);

        Address billingAddress = createBillingAddress(paymentInfo);

        if (null != request.getCard()) {
            TokenCard tokenCard = request.getCard();
            CustomerToken customer = new CustomerToken();
            customer.setPersonalInformation(personalInformationToken);
            customer.setVatNumber(userData.getVatNumber());
            customer.setCompanyInformation(companyInformation);
            customer.setMerchantCustomerId(userData.getShopperReference());
            customer.setBillingAddress(billingAddress);
            request.setCard(tokenCard);
        }
        else if (null != request.getSepaDirectDebit()) {
            TokenSepaDirectDebitWithoutCreditor sepaDirectDebit = request.getSepaDirectDebit();
            CustomerTokenWithContactDetails customerTokenWithContactDetails = new CustomerTokenWithContactDetails();
            ContactDetailsToken contactTokenDetails = new ContactDetailsToken();
            customerTokenWithContactDetails.setBillingAddress(billingAddress);
            customerTokenWithContactDetails.setContactDetails((ContactDetailsToken)contactDetailsBase);
            customerTokenWithContactDetails.setPersonalInformation(personalInformationToken);
            customerTokenWithContactDetails.setVatNumber(userData.getVatNumber());
            customerTokenWithContactDetails.setCompanyInformation(companyInformation);
            sepaDirectDebit.setCustomer(customerTokenWithContactDetails);
            request.setSepaDirectDebit(sepaDirectDebit);
        }
    }



    private Address createBillingAddress(final PaymentInfo paymentInfo) {
        final Address address = new Address();
        address.setStreet(paymentInfo.getStreet());
        address.setHouseNumber(paymentInfo.getHouseNumberOrName());
        address.setCity(paymentInfo.getCity());
        address.setZip(paymentInfo.getPostalCode());
        address.setState(paymentInfo.getStateOrProvince());

        final String adjustedCountry = paymentInfo.getCountry();
        address.setCountryCode(adjustedCountry);
        return address;
    }
}
