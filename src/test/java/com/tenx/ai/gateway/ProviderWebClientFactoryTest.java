package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.provider.ProviderWebClientFactory;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

public class ProviderWebClientFactoryTest {

    private ProviderWebClientFactory factory() {
        GatewayProperties properties = new GatewayProperties();
        ConnectionProvider connectionProvider = ConnectionProvider.create("test");
        ExchangeStrategies strategies = ExchangeStrategies.withDefaults();
        return new ProviderWebClientFactory(properties, connectionProvider, strategies);
    }

    @Test
    public void reusesClientForSameProviderConfiguration() {
        ProviderWebClientFactory factory = factory();

        GatewayProperties.ProviderConfig provider = provider("http://127.0.0.1:4000", "key-a");

        Assertions.assertSame(factory.client(provider), factory.client(provider));
    }

    @Test
    public void createsDifferentClientsForDifferentProviderConfigurations() {
        ProviderWebClientFactory factory = factory();

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
