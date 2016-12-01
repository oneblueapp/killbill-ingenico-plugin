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

package org.killbill.billing.plugin.ingenico.client.payment.service;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.slf4j.Logger;

public abstract class BaseIngenicoPaymentServiceProviderPort {

    protected Logger logger;

    protected void logTransaction(final String transactionType, final UserData userData, final PaymentData paymentData, @Nullable final PurchaseResult result, @Nullable final IngenicoCallResult<?> ingenicoCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendPaymentData(logBuffer, paymentData);
        appendUserData(logBuffer, userData);
        appendPurchaseResult(logBuffer, result);
        if (ingenicoCall != null) {
            appendDuration(logBuffer, ingenicoCall.getDuration());
        }
        logBuffer.append(", error=false");

        //logger.info(logBuffer.toString());
    }

    protected void logTransaction(final String transactionType, final String pspReference, final PaymentData paymentData, final PaymentModificationResponse response, final IngenicoCallResult<?> ingenicoCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendPaymentData(logBuffer, paymentData);
        appendPspReference(logBuffer, pspReference);
        appendModificationResponse(logBuffer, response);
        appendDuration(logBuffer, ingenicoCall.getDuration());
        logBuffer.append(", error=false");

        //logger.info(logBuffer.toString());
    }

    protected void logTransactionError(final String transactionType, final UserData userData, final PaymentData paymentData, final IngenicoCallResult<?> ingenicoCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendPaymentData(logBuffer, paymentData);
        appendUserData(logBuffer, userData);
        appendIngenicoCall(logBuffer, ingenicoCall);
        appendDuration(logBuffer, ingenicoCall.getDuration());
        logBuffer.append(", error=true");

        //logger.warn(logBuffer.toString());
    }

    protected void logTransactionError(final String transactionType, final String paymentId,  final PaymentData paymentData, final IngenicoCallResult<?> ingenicoCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendPaymentData(logBuffer, paymentData);
        appendPspReference(logBuffer, paymentId);
        appendIngenicoCall(logBuffer, ingenicoCall);
        appendDuration(logBuffer, ingenicoCall.getDuration());
        logBuffer.append(", error=true");

        //logger.warn(logBuffer.toString());
    }

    private void appendTransactionType(final StringBuilder buffer, final String transactionType) {
        buffer.append("op='").append(transactionType).append("'");
    }

    private void appendPaymentData(final StringBuilder buffer, final PaymentData paymentData) {
        if (paymentData == null ) {
            return;
        }

        if (paymentData.getAmount() != null) {
            buffer.append(", amount='")
                  .append(paymentData.getAmount())
                  .append("'");
        }
        if (paymentData.getCurrency() != null) {
            buffer.append(", currency='")
                  .append(paymentData.getCurrency())
                  .append("'");
        }
        if (paymentData.getPaymentTransactionExternalKey() != null) {
            buffer.append(", paymentTransactionExternalKey='")
                  .append(paymentData.getPaymentTransactionExternalKey())
                  .append("'");
        }
    }

    private void appendUserData(final StringBuilder buffer, final UserData userData) {
        if (userData != null && userData.getShopperReference() != null) {
            buffer.append(", shopperReference='")
                  .append(userData.getShopperReference())
                  .append("'");
        }
    }


    private void appendPspReference(final StringBuilder buffer, final String pspReference) {
        if (pspReference != null) {
            buffer.append(", pspReference='")
                  .append(pspReference)
                  .append("'");
        }
    }

    private void appendPurchaseResult(final StringBuilder buffer, @Nullable final PurchaseResult result) {
        if (result != null) {
            buffer.append(", ").append(result);
        }
    }

    private void appendModificationResponse(final StringBuilder buffer, final PaymentModificationResponse response) {
        buffer.append(", ").append(response);
    }

    private void appendDuration(final StringBuilder buffer, final long duration) {
        buffer.append(", duration=").append(duration);
    }

    private void appendIngenicoCall(final StringBuilder buffer, final IngenicoCallResult<?> ingenicoCall) {
        buffer.append(", ").append(ingenicoCall);
    }
}
