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

import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.ingenico.client.IngenicoConfigProperties;
import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.payment.converter.PaymentInfoConverterManagement;

import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;

public class IngenicoRequestFactory {

    private final PaymentInfoConverterManagement paymentInfoConverterManagement;
    private final IngenicoConfigProperties ingenicoConfigProperties;

    public IngenicoRequestFactory(final PaymentInfoConverterManagement paymentInfoConverterManagement,
                                  final IngenicoConfigProperties ingenicoConfigProperties) {
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;
        this.ingenicoConfigProperties = ingenicoConfigProperties;
    }

    public CreatePaymentRequest createPaymentRequest(final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData) {
        final PaymentRequestBuilder paymentRequestBuilder = new PaymentRequestBuilder(paymentData, userData, splitSettlementData, paymentInfoConverterManagement);
        return paymentRequestBuilder.build();
    }

//    public PaymentRequest3D paymentRequest3d(final String merchantAccount, final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData) {
//        final PaymentRequest3DBuilder paymentRequest3DBuilder = new PaymentRequest3DBuilder(merchantAccount, paymentData, userData, splitSettlementData);
//        return paymentRequest3DBuilder.build();
//    }
//
//    public ModificationRequest createModificationRequest(final String merchantAccount, final PaymentData paymentData, final String pspReference, @Nullable final SplitSettlementData splitSettlementData) {
//        final ModificationRequestBuilder modificationRequestBuilder = new ModificationRequestBuilder(merchantAccount, paymentData, pspReference, splitSettlementData);
//        return modificationRequestBuilder.build();
//    }
//
//    public Map<String, String> createHppRequest(final String merchantAccount, final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData) throws SignatureGenerationException {
//        final HPPRequestBuilder builder = new HPPRequestBuilder(merchantAccount,
//                                                                paymentData,
//                                                                userData,
//                                                                splitSettlementData,
//                                                                adyenConfigProperties,
//                                                                signer);
//        return builder.build();
//    }
}
