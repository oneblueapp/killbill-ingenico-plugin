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

package org.killbill.billing.plugin.ingenico.api;

import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.api.PluginCallContext;

/**
 * Created by otaviosoares on 30/11/16.
 */
public class IngenicoCallContext extends PluginCallContext {

    private static final String INGENICO_PLUGIN = "Ingenico plugin";

    public IngenicoCallContext(final DateTime utcNow, final UUID kbTenantId) {
        super(INGENICO_PLUGIN, utcNow, kbTenantId);
    }
}
