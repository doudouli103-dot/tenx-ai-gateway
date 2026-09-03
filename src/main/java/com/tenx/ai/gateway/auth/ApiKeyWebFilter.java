package com.tenx.ai.gateway.auth;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 全局 API Key 鉴权过滤器，是网关的第一道关卡。
 *
 * <p>除以下情况外，所有请求都必须携带 {@code Authorization: Bearer <key>}，且 key 必须在白名单内：
 * <ul>
 *   <li>{@code OPTIONS} 预检请求（配合 CORS）</li>
 *   <li>{@code /actuator/**}（预留，实际未启用 actuator）</li>
 *   <li>{@code /healthz} 健康检查</li>
 * </ul>
 *
 * <p>缺 key 或格式错误返回 401，key 不在白名单返回 403。
 */
@Component
public class ApiKeyWebFilter implements WebFilter {

    /** 允许的 API Key 集合（由配置注入，用于 O(1) 校验）。 */
    private final Set<String> apiKeys;

    /** 构造过滤器，把配置中的 key 列表转成 Set。 */
    public ApiKeyWebFilter(GatewayProperties properties) {
        this.apiKeys = new HashSet<String>(properties.getApiKeys());
    }

    /** 执行鉴权过滤。 */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
                || path.startsWith("/actuator")
                || "/healthz".equals(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (!apiKeys.contains(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
