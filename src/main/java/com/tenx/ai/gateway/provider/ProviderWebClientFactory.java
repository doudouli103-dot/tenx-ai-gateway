package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Component
public class ProviderWebClientFactory {

    private final GatewayProperties properties;
    private final ConnectionProvider connectionProvider;
    private final ExchangeStrategies exchangeStrategies;
    private final ConcurrentMap<ClientKey, WebClient> clients = new ConcurrentHashMap<ClientKey, WebClient>();

    public ProviderWebClientFactory(GatewayProperties properties,
                                    ConnectionProvider connectionProvider,
                                    ExchangeStrategies exchangeStrategies) {
        this.properties = properties;
        this.connectionProvider = connectionProvider;
        this.exchangeStrategies = exchangeStrategies;
    }

    public WebClient client(GatewayProperties.ProviderConfig provider) {
        ClientKey key = new ClientKey(
                provider.getBaseUrl(),
                provider.getApiKey(),
                provider.getResponseTimeoutMillis(),
                provider.getReadTimeoutMillis()
        );
        return clients.computeIfAbsent(key, ignored -> buildClient(provider));
    }

    private WebClient buildClient(GatewayProperties.ProviderConfig provider) {
        GatewayProperties.HttpConfig http = properties.getHttp();
        int responseTimeout = valueOr(provider.getResponseTimeoutMillis(), (int) http.getResponseTimeoutMillis());
        int readTimeout = valueOr(provider.getReadTimeoutMillis(), (int) http.getReadTimeoutMillis());

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, http.getConnectTimeoutMillis())
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(http.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS)));

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(provider.getBaseUrl());
        if (provider.getApiKey() != null && provider.getApiKey().trim().length() > 0) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey());
        }
        return builder.build();
    }

    private static int valueOr(Integer override, int fallback) {
        return override != null ? override : fallback;
    }

    private static class ClientKey {
        private final String baseUrl;
        private final String apiKey;
        private final Integer responseTimeoutMillis;
        private final Integer readTimeoutMillis;

        ClientKey(String baseUrl, String apiKey, Integer responseTimeoutMillis, Integer readTimeoutMillis) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.responseTimeoutMillis = responseTimeoutMillis;
            this.readTimeoutMillis = readTimeoutMillis;
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
            return equalsNullable(baseUrl, that.baseUrl)
                    && equalsNullable(apiKey, that.apiKey)
                    && equalsNullable(responseTimeoutMillis, that.responseTimeoutMillis)
                    && equalsNullable(readTimeoutMillis, that.readTimeoutMillis);
        }

        @Override
        public int hashCode() {
            int result = baseUrl == null ? 0 : baseUrl.hashCode();
            result = 31 * result + (apiKey == null ? 0 : apiKey.hashCode());
            result = 31 * result + (responseTimeoutMillis == null ? 0 : responseTimeoutMillis.hashCode());
            result = 31 * result + (readTimeoutMillis == null ? 0 : readTimeoutMillis.hashCode());
            return result;
        }

        private static boolean equalsNullable(Object left, Object right) {
            return left == null ? right == null : left.equals(right);
        }
    }
}
