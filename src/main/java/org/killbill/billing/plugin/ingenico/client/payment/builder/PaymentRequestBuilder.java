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

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.payment.converter.PaymentInfoConverterManagement;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.Address;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.AmountOfMoney;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.AddressPersonal;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.ContactDetails;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Customer;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Order;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderInvoiceData;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderReferences;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PersonalInformation;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PersonalName;

public class PaymentRequestBuilder extends RequestBuilder<CreatePaymentRequest> {

    private final PaymentData paymentData;
    private final UserData userData;
    private final SplitSettlementData splitSettlementData;
    private final Order order;

    public PaymentRequestBuilder(final PaymentData paymentData,
                                 final UserData userData,
                                 @Nullable final SplitSettlementData splitSettlementData,
                                 final PaymentInfoConverterManagement paymentInfoConverterManagement) {
        super(paymentInfoConverterManagement.convertPaymentInfoToPaymentRequest(paymentData.getPaymentInfo()));
        this.paymentData = paymentData;
        this.userData = userData;
        this.splitSettlementData = splitSettlementData;
        this.order = request.getOrder();
    }

    @Override
    public CreatePaymentRequest build() {
        setReferences();
        setAmount();
        setShopperData();
        setShippingAddress();
        setSplitSettlementData();

        request.setOrder(order);
        return request;
    }

    private void setReferences() {
        //OrderInvoiceData invoiceData = new OrderInvoiceData();
        //invoiceData.setInvoiceDate("20140306191500");
        //invoiceData.setInvoiceNumber("000000123");

        OrderReferences references = new OrderReferences();
        //references.setDescriptor("Fast and Furry-ous");
        //references.setInvoiceData(invoiceData);
        //references.setMerchantOrderId(123456L);
        references.setMerchantReference(paymentData.getPaymentTransactionExternalKey());
        order.setReferences(references);
    }

    private void setAmount() {
        if (paymentData.getAmount() == null || paymentData.getCurrency() == null) {
            return;
        }

        final String currency = paymentData.getCurrency().name();
        final AmountOfMoney amount = new AmountOfMoney();
        amount.setAmount(toMinorUnits(paymentData.getAmount(), currency));
        amount.setCurrencyCode(currency);
        order.setAmountOfMoney(amount);
    }

    private void setShopperData() {
        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setEmailAddress(userData.getShopperEmail());
        contactDetails.setEmailMessageType("html");
        contactDetails.setPhoneNumber(userData.getTelephoneNumber());

        PersonalName name = new PersonalName();
        name.setFirstName(userData.getFirstName());
        name.setSurname(userData.getLastName());
        //name.setSurnamePrefix("E.");
        //name.setTitle("Mr.");

        PersonalInformation personalInformation = new PersonalInformation();
        personalInformation.setDateOfBirth(userData.getFormattedDateOfBirth("yyyyMMdd"));
        personalInformation.setGender(userData.getGender());
        personalInformation.setName(name);

        Customer customer = order.getCustomer() != null ? order.getCustomer() : new Customer();
        customer.setContactDetails(contactDetails);
        customer.setLocale(userData.getShopperLocale().toString());
        customer.setMerchantCustomerId(userData.getShopperReference());
        customer.setPersonalInformation(personalInformation);
        customer.setVatNumber(userData.getVatNumber());

        //customer.setCompanyInformation(companyInformation););;
        order.setCustomer(customer);
    }

    private void setShippingAddress() {
        PersonalName personalName = new PersonalName();
        personalName.setFirstName("Road");
        personalName.setSurname("Runner");
        //        shippingName.setTitle("Miss");

        AddressPersonal shippingAddress = new AddressPersonal();
        shippingAddress.setAdditionalInfo("Suite II");
        shippingAddress.setCity("Monument Valley");
        shippingAddress.setCountryCode("US");
        shippingAddress.setHouseNumber("1");
        shippingAddress.setName(personalName);
        shippingAddress.setState("Utah");
        shippingAddress.setStreet("Desertroad");
        shippingAddress.setZip("84536");

        Customer customer = order.getCustomer() != null ? order.getCustomer() : new Customer();
        customer.setShippingAddress(shippingAddress);
        order.setCustomer(customer);
    }

    private void setSplitSettlementData() {
//        if (splitSettlementData != null) {
//            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
//            request.getAdditionalData().getEntry().addAll(entries);
//        }
    }
}
