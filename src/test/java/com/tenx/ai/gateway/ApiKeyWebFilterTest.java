package com.tenx.ai.gateway;

import com.tenx.ai.gateway.auth.ApiKeyWebFilter;
import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ApiKeyWebFilterTest {

    @Test
    public void rejectsMissingAuthorizationHeader() {
        ApiKeyWebFilter filter = new ApiKeyWebFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/v1/chat/completions").build()
        );

        StepVerifier.create(filter.filter(exchange, successfulChain()))
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    public void rejectsInvalidApiKey() {
        ApiKeyWebFilter filter = new ApiKeyWebFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/v1/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong")
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, successfulChain()))
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    public void allowsValidApiKey() {
        ApiKeyWebFilter filter = new ApiKeyWebFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/v1/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer local-dev-key")
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, successfulChain()))
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    public void allowsHealthCheckWithoutApiKey() {
        ApiKeyWebFilter filter = new ApiKeyWebFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/healthz").build()
        );

        StepVerifier.create(filter.filter(exchange, successfulChain()))
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertNull(exchange.getResponse().getStatusCode());
    }

    private GatewayProperties properties() {
        GatewayProperties properties = new GatewayProperties();
        properties.setApiKeys(Arrays.asList("local-dev-key", "github-agent-key"));
        return properties;
    }

    private WebFilterChain successfulChain() {
        return exchange -> Mono.empty();
    }
}
