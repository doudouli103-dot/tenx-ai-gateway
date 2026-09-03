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

/**
 * WebClient 基础设施配置。把连接池、编解码策略、WebClient.Builder 拆成独立 bean，
 * 供 {@code ProviderWebClientFactory} 复用（所有 provider 共享连接池，各自配置超时）。
 */
@Configuration
public class WebClientConfig {

    /**
     * 共享连接池。maxConnections 与 pendingAcquireTimeout 来自全局 http 配置。
     * 声明 destroyMethod 以便应用关闭时正确释放池资源。
     */
    @Bean(destroyMethod = "dispose")
    public ConnectionProvider connectionProvider(GatewayProperties properties) {
        GatewayProperties.HttpConfig httpConfig = properties.getHttp();
        return ConnectionProvider.builder("tenx-ai-gateway")
                .maxConnections(httpConfig.getMaxConnections())
                .pendingAcquireTimeout(Duration.ofMillis(httpConfig.getPendingAcquireTimeoutMillis()))
                .build();
    }

    /**
     * 共享编解码策略。设置响应体解码的最大内存占用，避免超限抛出 DataBufferLimitException。
     */
    @Bean
    public ExchangeStrategies exchangeStrategies(GatewayProperties properties) {
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(properties.getHttp().getMaxInMemorySizeBytes()))
                .build();
    }

    /**
     * 共享的 WebClient.Builder。这里仅设定默认的 HttpClient（连接超时、响应超时、读写空闲超时）；
     * 具体的 baseUrl、apiKey、以及 per-provider 超时由 ProviderWebClientFactory 在 clone 时覆盖。
     */
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
