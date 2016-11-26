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
import javax.annotation.Nullable;

import org.killbill.billing.plugin.util.KillBillMoney;

public abstract class RequestBuilder<R> {

    protected R request;

    protected RequestBuilder(final R request) {
        this.request = request;
    }

    public R build() {
        return request;
    }

    protected Long toMinorUnits(@Nullable final BigDecimal amountBD, @Nullable final String currencyIsoCode) {
        if (amountBD == null || currencyIsoCode == null) {
            return null;
        }
        return KillBillMoney.toMinorUnits(currencyIsoCode, amountBD);
    }

    protected Long toLong(@Nullable final BigDecimal amountBD) {
        return amountBD.setScale(0,BigDecimal.ROUND_HALF_UP).longValueExact();
    }
}
