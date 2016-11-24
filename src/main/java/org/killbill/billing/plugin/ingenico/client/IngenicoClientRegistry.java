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

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.X509TrustManager;

import com.google.common.base.Preconditions;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.CommunicatorConfiguration;
import com.ingenico.connect.gateway.sdk.java.Factory;
import com.ingenico.connect.gateway.sdk.java.merchant.MerchantClient;

public class IngenicoClientRegistry {

    private final Map<String, Object> services = new ConcurrentHashMap<String, Object>();


    protected final IngenicoConfigProperties config;
    private Client client;

    public IngenicoClientRegistry(final IngenicoConfigProperties config) {
        this.config = Preconditions.checkNotNull(config, "config");
    }

    public void close() throws IOException {
        for (final Object service : services.values()) {
            try {
                this.client.close();
                this.client = null;
            } catch (final RuntimeException ignored) {
            }
        }
    }

    public Client getClient() {
        if (this.client != null) return this.client;
        Preconditions.checkNotNull(config.getEndpointHost(), "apiKey");
        Preconditions.checkNotNull(config.getIntegrator(), "apiKey");
        Preconditions.checkNotNull(config.getMerchantId(), "apiKey");
        Preconditions.checkNotNull(config.getApiKey(), "apiKey");
        Preconditions.checkNotNull(config.getApiSecret(), "portName");

        CommunicatorConfiguration configuration = new CommunicatorConfiguration(config.toProperties())
                .withApiKeyId(config.getApiKey())
                .withSecretApiKey(config.getApiSecret());
        this.client = Factory.createClient(configuration);
        return this.client;
    }

    public MerchantClient getMerchantClient() {
        return getClient().merchant(config.getMerchantId());
    }

    private static final class TrustAllX509TrustManager implements X509TrustManager {

        private static final X509Certificate[] acceptedIssuers = {};

        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {}

        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {}

        public X509Certificate[] getAcceptedIssuers() {
            return acceptedIssuers;
        }
    }
}
