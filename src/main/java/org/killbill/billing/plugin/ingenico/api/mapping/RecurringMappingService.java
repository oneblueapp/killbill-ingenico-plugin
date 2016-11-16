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

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;

import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE;

public abstract class RecurringMappingService {

    public static Recurring toPaymentInfo(final IngenicoPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        final Recurring recurring = new Recurring();

        recurring.setRecurringDetailReference(paymentMethodsRecord.getToken());

        final String ccVerificationValue = PluginProperties.getValue(PROPERTY_CC_VERIFICATION_VALUE, paymentMethodsRecord.getCcVerificationValue(), properties);
        recurring.setCvc(ccVerificationValue);

        return recurring;
    }
}
