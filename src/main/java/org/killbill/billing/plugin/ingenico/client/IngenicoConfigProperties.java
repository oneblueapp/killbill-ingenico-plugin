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

package org.killbill.billing.plugin.ingenico.client;

import java.util.Properties;

public class IngenicoConfigProperties {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.ingenico.";

    private static final String DEFAULT_AUTHORIZATION_TYPE = "V1HMAC";
    private static final String DEFAULT_CONNECTION_TIMEOUT = "30000";
    private static final String DEFAULT_SOCKET_TIMEOUT = "60000";
    private static final String DEFAULT_MAX_CONNECTIONS = "10";
    private static final String DEFAULT_INTEGRATOR = "Ingenico";
    private static final String DEFAULT_ENDPOINT_HOST = "api-sandbox.globalcollect.com";

    //private final Map<String, String> merchantAccountMap = new ConcurrentHashMap<String, String>();

    private final String endpointHost;
    private final String authorizationType;
    private final String connectTimeout;
    private final String socketTimeout;
    private final String maxConnections;
    private final String integrator;
    private final String apiKey;
    private final String apiSecret;
    private final String merchantId;

    public IngenicoConfigProperties(final Properties properties) {
        this.authorizationType = properties.getProperty(PROPERTY_PREFIX + "authorizationType", DEFAULT_AUTHORIZATION_TYPE);
        this.connectTimeout = properties.getProperty(PROPERTY_PREFIX + "connectTimeout", DEFAULT_CONNECTION_TIMEOUT);
        this.socketTimeout = properties.getProperty(PROPERTY_PREFIX + "socketTimeout", DEFAULT_SOCKET_TIMEOUT);
        this.maxConnections = properties.getProperty(PROPERTY_PREFIX + "maxConnections", DEFAULT_MAX_CONNECTIONS);
        this.integrator = properties.getProperty(PROPERTY_PREFIX + "integrator", DEFAULT_INTEGRATOR);
        this.endpointHost = properties.getProperty(PROPERTY_PREFIX + "endpoint.host", DEFAULT_ENDPOINT_HOST);
        this.apiKey = properties.getProperty(PROPERTY_PREFIX + "apiKey", "placeholder");
        this.apiSecret = properties.getProperty(PROPERTY_PREFIX + "apiSecret", "placeholder");
        this.merchantId = properties.getProperty(PROPERTY_PREFIX + "merchantId");

//        this.merchantAccounts = properties.getProperty(PROPERTY_PREFIX + "merchantAccount");
//        merchantAccountMap.clear();
//        if (merchantAccounts != null && merchantAccounts.contains(ENTRY_DELIMITER)) {
//            for (final String account : merchantAccounts.split("\\" + ENTRY_DELIMITER)) {
//                final String countryIsoCode = account.split(KEY_VALUE_DELIMITER)[0];
//                final String merchantAccount = account.split(KEY_VALUE_DELIMITER)[1];
//                merchantAccountMap.put(countryIsoCode, merchantAccount);
//            }
//        }
    }

//    public String getMerchantAccount(final String countryIsoCode) {
//        if (countryIsoCode == null || merchantAccountMap.isEmpty()) {
//            return merchantAccounts;
//        }
//
//        return merchantAccountMap.get(adjustCountryCode(countryIsoCode));
//    }

    public String getAuthorizationType() {
        return authorizationType;
    }

    public String getConnectTimeout() {
        return connectTimeout;
    }

    public String getSocketTimeout() {
        return socketTimeout;
    }

    public String getMaxConnections() { return maxConnections; }

    public String getIntegrator() {
        return integrator;
    }

    public String getEndpointHost() {
        return endpointHost;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.put("connect.api.authorizationType", getAuthorizationType());
        properties.put("connect.api.connectTimeout", getConnectTimeout());
        properties.put("connect.api.socketTimeout", getSocketTimeout());
        properties.put("connect.api.maxConnections", getMaxConnections());
        properties.put("connect.api.integrator", getIntegrator());
        properties.put("connect.api.endpoint.host", getEndpointHost());
        return properties;
    }
}
