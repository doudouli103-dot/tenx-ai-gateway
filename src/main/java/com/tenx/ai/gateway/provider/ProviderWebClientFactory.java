package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProviderWebClientFactory {

    private final WebClient.Builder webClientBuilder;
    private final ConcurrentMap<ClientKey, WebClient> clients = new ConcurrentHashMap<ClientKey, WebClient>();

    public ProviderWebClientFactory(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public WebClient client(GatewayProperties.ProviderConfig provider) {
        ClientKey key = new ClientKey(provider.getBaseUrl(), provider.getApiKey());
        return clients.computeIfAbsent(key, ignored -> buildClient(provider));
    }

    private WebClient buildClient(GatewayProperties.ProviderConfig provider) {
        WebClient.Builder builder = webClientBuilder.clone().baseUrl(provider.getBaseUrl());
        if (provider.getApiKey() != null && provider.getApiKey().trim().length() > 0) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey());
        }
        return builder.build();
    }

    private static class ClientKey {
        private final String baseUrl;
        private final String apiKey;

        ClientKey(String baseUrl, String apiKey) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ClientKey)) {
                return false;
            }
            ClientKey that = (ClientKey) other;
            return equalsNullable(baseUrl, that.baseUrl) && equalsNullable(apiKey, that.apiKey);
        }

        @Override
        public int hashCode() {
            int result = baseUrl == null ? 0 : baseUrl.hashCode();
            result = 31 * result + (apiKey == null ? 0 : apiKey.hashCode());
            return result;
        }

        private static boolean equalsNullable(String left, String right) {
            return left == null ? right == null : left.equals(right);
        }
    }
}
