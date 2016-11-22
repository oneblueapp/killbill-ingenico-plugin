/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.ingenico.api.IngenicoPaymentPluginApi;
import org.killbill.billing.plugin.ingenico.client.IngenicoClient;
import org.killbill.billing.plugin.ingenico.dao.IngenicoDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.osgi.framework.BundleContext;

public class IngenicoActivator extends KillbillActivatorBase {

    //
    // Ideally that string should match the pluginName on the filesystem, but there is no enforcement
    //
    public static final String PLUGIN_NAME = "killbill-ingenico";

    private OSGIKillbillEventDispatcher.OSGIKillbillEventHandler killbillEventHandler;
    private IngenicoConfigurationHandler ingenicoConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final Clock clock = new DefaultClock();
        final IngenicoDao dao = new IngenicoDao(dataSource.getDataSource());
        ingenicoConfigurationHandler = new IngenicoConfigurationHandler(PLUGIN_NAME, killbillAPI, logService);

        // Register an event listener (optional)
//        killbillEventHandler = new IngenicoListener(logService, killbillAPI);
//        registerEventHandlerWhenPluginStart(killbillEventHandler);

        final IngenicoClient globalIngenicoClient = ingenicoConfigurationHandler.createConfigurable(configProperties.getProperties());
        ingenicoConfigurationHandler.setDefaultConfigurable(globalIngenicoClient);

        final PaymentPluginApi paymentPluginApi = new IngenicoPaymentPluginApi(ingenicoConfigurationHandler, killbillAPI, configProperties, logService, clock, dao);
        registerPaymentPluginApi(context, paymentPluginApi);

        final IngenicoServlet analyticsServlet = new IngenicoServlet(logService);
        registerServlet(context, analyticsServlet);

        registerHandlers();
    }

    public void registerHandlers() {
        final PluginConfigurationEventHandler handler = new PluginConfigurationEventHandler(ingenicoConfigurationHandler);
        dispatcher.registerEventHandlers(handler);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        // Do additional work on shutdown (optional)
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }
}
