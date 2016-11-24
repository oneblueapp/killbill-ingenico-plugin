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

package org.killbill.billing.plugin.ingenico.client.payment.converter;

import org.killbill.billing.plugin.ingenico.client.model.PaymentInfo;

import com.ingenico.connect.gateway.sdk.java.domain.definitions.Address;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Customer;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Order;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.CustomerToken;

public class PaymentInfoConverter<T extends PaymentInfo> {

    private Order order;

    /**
     * @param paymentInfo to convert
     * @return {@code true} if this converter is capable of handling the payment info
     */
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return true;
    }

    /**
     * Convert a PaymentInfo Object into an Ingenico CreatePaymentRequest
     */
    public CreatePaymentRequest convertPaymentInfoToPaymentRequest(final T paymentInfo) {
        final CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        order = new Order();

        setInstallments(paymentInfo, paymentRequest);
        setOrderBillingAddress(paymentInfo, paymentRequest);
        paymentRequest.setOrder(order);

        return paymentRequest;
    }

    /**
     * Convert a PaymentInfo Object into an Ingenico CreateTokenRequest
     */
    public CreateTokenRequest convertPaymentInfoToCreateTokenRequest(final T paymentInfo) {
        final CreateTokenRequest createTokenRequest = new CreateTokenRequest();

        return createTokenRequest;
    }

    private void setInstallments(final PaymentInfo paymentInfo, final CreatePaymentRequest paymentRequest) {
//        if (paymentInfo.getInstallments() != null) {
//            final Installments installments = new Installments();
//            installments.setValue(paymentInfo.getInstallments().shortValue());
//            paymentRequest.setInstallments(installments);
//        }
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

    private void setOrderBillingAddress(PaymentInfo paymentInfo, final CreatePaymentRequest paymentRequest) {
        Address address = createBillingAddress(paymentInfo);
        Customer customer = order.getCustomer() != null ? order.getCustomer() : new Customer();
        customer.setBillingAddress(address);
        order.setCustomer(customer);
        paymentRequest.setOrder(order);
    }
}
