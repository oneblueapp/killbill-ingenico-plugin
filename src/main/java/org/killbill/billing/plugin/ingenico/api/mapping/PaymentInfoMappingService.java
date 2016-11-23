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

package org.killbill.billing.plugin.ingenico.api.mapping;

import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.ingenico.client.model.PaymentInfo;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;
import org.killbill.clock.Clock;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.*;

public abstract class PaymentInfoMappingService {

    public static PaymentInfo toPaymentInfo(final Clock clock, final AccountData account, final Iterable<PluginProperty> properties) {
        return toPaymentInfo(clock, account, null, properties);
    }

    public static PaymentInfo toPaymentInfo(final Clock clock, @Nullable final AccountData account, @Nullable final IngenicoPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        final PaymentInfo paymentInfo;


        if (paymentMethodsRecord != null && paymentMethodsRecord.getToken() != null) {
            paymentInfo = RecurringMappingService.toPaymentInfo(paymentMethodsRecord, properties);
        } else {
            paymentInfo = CardMappingService.toPaymentInfo(paymentMethodsRecord, properties);
        }

        setBillingAddress(account, paymentInfo, paymentMethodsRecord, properties);
        setSelectedBrand(paymentInfo, properties);

        return paymentInfo;
    }

    private static void setBillingAddress(@Nullable final AccountData account, final PaymentInfo paymentInfo, @Nullable final IngenicoPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        String street = PluginProperties.getValue(PROPERTY_ADDRESS1, paymentMethodsRecord == null ? null : paymentMethodsRecord.getAddress1(), properties);
        if (street == null && account != null) {
            street = account.getAddress1();
        }
        paymentInfo.setStreet(street);

        String houseNumberOrName = PluginProperties.getValue(PROPERTY_ADDRESS2, paymentMethodsRecord == null ? null : paymentMethodsRecord.getAddress2(), properties);
        if (houseNumberOrName == null && account != null) {
            houseNumberOrName = account.getAddress2();
        }
        paymentInfo.setHouseNumberOrName(houseNumberOrName);

        String city = PluginProperties.getValue(PROPERTY_CITY, paymentMethodsRecord == null ? null : paymentMethodsRecord.getCity(), properties);
        if (city == null && account != null) {
            city = account.getCity();
        }
        paymentInfo.setCity(city);

        String postalCode = PluginProperties.getValue(PROPERTY_ZIP, paymentMethodsRecord == null ? null : paymentMethodsRecord.getZip(), properties);
        if (postalCode == null && account != null) {
            postalCode = account.getPostalCode();
        }
        paymentInfo.setPostalCode(postalCode);

        String stateOrProvince = PluginProperties.getValue(PROPERTY_STATE, paymentMethodsRecord == null ? null : paymentMethodsRecord.getState(), properties);
        if (stateOrProvince == null && account != null) {
            stateOrProvince = account.getStateOrProvince();
        }
        paymentInfo.setStateOrProvince(stateOrProvince);

        String country = PluginProperties.getValue(PROPERTY_COUNTRY, paymentMethodsRecord == null ? null : paymentMethodsRecord.getCountry(), properties);
        if (country == null && account != null) {
            country = account.getCountry();
        }
        paymentInfo.setCountry(country);
    }

    private static void setSelectedBrand(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String brand = PluginProperties.findPluginPropertyValue(PROPERTY_CC_TYPE, properties);
        final Integer paymentProductId = PaymentProductMappingService.toPaymentProductId(brand);
        paymentInfo.setPaymentProductId(paymentProductId);
    }

    private static String decode(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return value;
        }
    }

}
