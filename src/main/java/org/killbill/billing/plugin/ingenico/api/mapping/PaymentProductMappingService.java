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

import java.util.HashMap;

/**
 * Created by otaviosoares on 21/11/16.
 */
public class PaymentProductMappingService {

    final private static HashMap<String, Integer> MAP = new HashMap<String, Integer>() {
        {
            put("VISA", 1);
            put("AMERICAN_EXPRESS", 2);
            put("MASTERCARD", 3);
            put("VISA_DEBIG", 114);
            put("MAESTRO", 117);
            put("MASTERCARD_DEBIT", 119);
            put("VISA_ELECTRON", 122);
            put("JCB", 125);
            put("DISCOVER", 128);
            put("CARTE_BANCAIRE", 130);
            put("DINERS_CLUB", 132);
            put("CABAL", 135);
            put("NARANJA", 136);
            put("NEVADA", 137);
            put("ITALCRED", 139);
            put("ARGENCARD", 140);
            put("CONSUMAX", 141);
            put("MAS", 142);
            put("PYME_NACION", 144);
            put("NATIVA", 145);
            put("AURA", 146);
            put("ELO", 147);
            put("HIPERCARD", 148);
            put("TARJETA_SHOPPING", 149);
        }
    };

    public static Integer toPaymentProductId(final String brand) {
        return MAP.get(brand);
    }
}
