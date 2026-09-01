package com.tenx.ai.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder(GatewayProperties properties) {
        GatewayProperties.HttpConfig httpConfig = properties.getHttp();
        ConnectionProvider connectionProvider = ConnectionProvider.builder("tenx-ai-gateway")
                .maxConnections(httpConfig.getMaxConnections())
                .pendingAcquireTimeout(Duration.ofMillis(httpConfig.getPendingAcquireTimeoutMillis()))
                .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, httpConfig.getConnectTimeoutMillis())
                .responseTimeout(Duration.ofMillis(httpConfig.getResponseTimeoutMillis()))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(httpConfig.getReadTimeoutMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(httpConfig.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS)));
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(httpConfig.getMaxInMemorySizeBytes()))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }
}
