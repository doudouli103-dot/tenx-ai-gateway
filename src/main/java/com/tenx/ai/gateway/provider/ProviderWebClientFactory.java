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

/**
 * 按 provider 配置构建并缓存 WebClient。
 *
 * <p>设计要点：
 * <ul>
 *   <li>所有 provider 共享同一个 {@link ConnectionProvider}（连接池）和 {@link ExchangeStrategies}（编解码/内存上限）。</li>
 *   <li>每个 provider 拥有独立的 HttpClient 管道配置（响应超时、读空闲超时），
 *       以便视频/图像等长任务使用比聊天更宽松的超时。</li>
 *   <li>以「baseUrl + apiKey + 两个超时」为缓存 key，相同配置复用同一个 WebClient 实例。</li>
 * </ul>
 */
@Component
public class ProviderWebClientFactory {

    /** 全局配置对象，提供 HTTP 默认超时等参数。 */
    private final GatewayProperties properties;

    /** 共享连接池。 */
    private final ConnectionProvider connectionProvider;

    /** 共享编解码策略（内存上限）。 */
    private final ExchangeStrategies exchangeStrategies;

    /** WebClient 缓存，key 为 provider 的关键配置。 */
    private final ConcurrentMap<ClientKey, WebClient> clients = new ConcurrentHashMap<ClientKey, WebClient>();

    /** 构造 WebClient 工厂。 */
    public ProviderWebClientFactory(GatewayProperties properties,
                                    ConnectionProvider connectionProvider,
                                    ExchangeStrategies exchangeStrategies) {
        this.properties = properties;
        this.connectionProvider = connectionProvider;
        this.exchangeStrategies = exchangeStrategies;
    }

    /** 取得（或按需构建）指定 provider 的 WebClient。 */
    public WebClient client(GatewayProperties.ProviderConfig provider) {
        ClientKey key = new ClientKey(
                provider.getBaseUrl(),
                provider.getApiKey(),
                provider.getResponseTimeoutMillis(),
                provider.getReadTimeoutMillis()
        );
        return clients.computeIfAbsent(key, ignored -> buildClient(provider));
    }

    /** 构建单个 provider 的 WebClient，超时优先用 provider 覆盖值，否则用全局默认值。 */
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

    /** 取覆盖值，覆盖值为空时回落到默认值。 */
    private static int valueOr(Integer override, int fallback) {
        return override != null ? override : fallback;
    }

    /** WebClient 缓存 key：包含决定连接行为的所有 provider 配置。 */
    private static class ClientKey {
        /** 上游根地址。 */
        private final String baseUrl;

        /** 上游 API Key。 */
        private final String apiKey;

        /** 整体响应超时（可为空）。 */
        private final Integer responseTimeoutMillis;

        /** 读空闲超时（可为空）。 */
        private final Integer readTimeoutMillis;

        /** 构造缓存 key。 */
        ClientKey(String baseUrl, String apiKey, Integer responseTimeoutMillis, Integer readTimeoutMillis) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.responseTimeoutMillis = responseTimeoutMillis;
            this.readTimeoutMillis = readTimeoutMillis;
        }

        /** 判断两个 key 是否等价。 */
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

        /** 计算哈希码，保证 equals 的对象哈希一致。 */
        @Override
        public int hashCode() {
            int result = baseUrl == null ? 0 : baseUrl.hashCode();
            result = 31 * result + (apiKey == null ? 0 : apiKey.hashCode());
            result = 31 * result + (responseTimeoutMillis == null ? 0 : responseTimeoutMillis.hashCode());
            result = 31 * result + (readTimeoutMillis == null ? 0 : readTimeoutMillis.hashCode());
            return result;
        }

        /** 空安全的 equals。 */
        private static boolean equalsNullable(Object left, Object right) {
            return left == null ? right == null : left.equals(right);
        }
    }
}
