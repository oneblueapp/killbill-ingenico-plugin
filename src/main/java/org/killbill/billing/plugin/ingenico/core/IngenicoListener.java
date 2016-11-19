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

package org.killbill.billing.plugin.ingenico.core;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.service.log.LogService;

public class IngenicoListener implements OSGIKillbillEventHandler {

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;

    public IngenicoListener(final OSGIKillbillLogService logService, final OSGIKillbillAPI killbillAPI) {
        this.logService = logService;
        this.osgiKillbillAPI = killbillAPI;
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {

        logService.log(LogService.LOG_INFO, "Received event " + killbillEvent.getEventType() +
                " for object id " + killbillEvent.getObjectId() +
                " of type " + killbillEvent.getObjectType());

        switch (killbillEvent.getEventType()) {
            //
            // Handle ACCOUNT_CREATION and ACCOUNT_CHANGE (only) for demo purpose and just print the account
            //
            case ACCOUNT_CREATION:
            case ACCOUNT_CHANGE:
                try {
                    final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), new PluginTenantContext(killbillEvent.getTenantId()));
                    logService.log(LogService.LOG_INFO, "Account information: " + account);
                } catch (final AccountApiException e) {
                    logService.log(LogService.LOG_WARNING, "Unable to find account", e);
                }
                break;

            // Nothing
            default:
                break;

        }
    }
}
