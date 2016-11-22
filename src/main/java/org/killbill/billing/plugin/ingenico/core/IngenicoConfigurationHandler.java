package org.killbill.billing.plugin.ingenico.core;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;
import org.killbill.billing.plugin.ingenico.client.IngenicoClient;
import org.killbill.billing.plugin.ingenico.client.IngenicoConfigProperties;
import java.util.Properties;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class IngenicoConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<IngenicoClient> {
    public IngenicoConfigurationHandler(String pluginName, OSGIKillbillAPI osgiKillbillAPI, OSGIKillbillLogService osgiKillbillLogService) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
    }

    @Override
    protected IngenicoClient createConfigurable(final Properties properties) {
        final IngenicoConfigProperties ingenicoConfigProperties = new IngenicoConfigProperties(properties);
        return new IngenicoClient(ingenicoConfigProperties);
    }
}
