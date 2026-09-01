package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.provider.ProviderWebClientFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

public class ProviderWebClientFactoryTest {

    @Test
    public void reusesClientForSameProviderConfiguration() {
        ProviderWebClientFactory factory = new ProviderWebClientFactory(WebClient.builder());

        GatewayProperties.ProviderConfig provider = provider("http://127.0.0.1:4000", "key-a");

        Assertions.assertSame(factory.client(provider), factory.client(provider));
    }

    @Test
    public void createsDifferentClientsForDifferentProviderConfigurations() {
        ProviderWebClientFactory factory = new ProviderWebClientFactory(WebClient.builder());

        WebClient first = factory.client(provider("http://127.0.0.1:4000", "key-a"));
        WebClient second = factory.client(provider("http://127.0.0.1:4010", "key-a"));
        WebClient third = factory.client(provider("http://127.0.0.1:4000", "key-b"));

        Assertions.assertNotSame(first, second);
        Assertions.assertNotSame(first, third);
    }

    private GatewayProperties.ProviderConfig provider(String baseUrl, String apiKey) {
        GatewayProperties.ProviderConfig provider = new GatewayProperties.ProviderConfig();
        provider.setBaseUrl(baseUrl);
        provider.setApiKey(apiKey);
        return provider;
    }
}
