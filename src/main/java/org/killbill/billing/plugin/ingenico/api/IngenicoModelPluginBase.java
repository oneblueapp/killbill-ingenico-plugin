package org.killbill.billing.plugin.ingenico.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by otaviosoares on 15/11/16.
 */
public class IngenicoModelPluginBase {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected static List<PluginProperty> buildPluginProperties(@Nullable final String additionalData) {
        if (additionalData == null) {
            return ImmutableList.<PluginProperty>of();
        }

        final Map additionalDataMap;
        try {
            additionalDataMap = objectMapper.readValue(additionalData, Map.class);
        } catch (final IOException e) {
            return ImmutableList.<PluginProperty>of();
        }

        return PluginProperties.buildPluginProperties(additionalDataMap);
    }
}
