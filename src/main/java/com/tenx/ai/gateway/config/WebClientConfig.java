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

    @Bean(destroyMethod = "dispose")
    public ConnectionProvider connectionProvider(GatewayProperties properties) {
        GatewayProperties.HttpConfig httpConfig = properties.getHttp();
        return ConnectionProvider.builder("tenx-ai-gateway")
                .maxConnections(httpConfig.getMaxConnections())
                .pendingAcquireTimeout(Duration.ofMillis(httpConfig.getPendingAcquireTimeoutMillis()))
                .build();
    }

    @Bean
    public ExchangeStrategies exchangeStrategies(GatewayProperties properties) {
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(properties.getHttp().getMaxInMemorySizeBytes()))
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder(ConnectionProvider connectionProvider,
                                              ExchangeStrategies exchangeStrategies,
                                              GatewayProperties properties) {
        GatewayProperties.HttpConfig httpConfig = properties.getHttp();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, httpConfig.getConnectTimeoutMillis())
                .responseTimeout(Duration.ofMillis(httpConfig.getResponseTimeoutMillis()))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(httpConfig.getReadTimeoutMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(httpConfig.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies);
    }
}
