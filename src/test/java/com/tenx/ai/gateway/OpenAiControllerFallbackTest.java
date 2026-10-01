package com.tenx.ai.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.api.OpenAiController;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ChatRequest;
import com.tenx.ai.gateway.provider.ModelProvider;
import com.tenx.ai.gateway.provider.ModelProviderRegistry;
import com.tenx.ai.gateway.provider.UpstreamProviderException;
import com.tenx.ai.gateway.routing.ModelRouter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class OpenAiControllerFallbackTest {

    @Test
    public void fallsBackWhenStreamFailsBeforeFirstChunk() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        OpenAiController controller = controller(
                Flux.error(retryableError()),
                Flux.defer(() -> {
                    fallbackCalls.incrementAndGet();
                    return Flux.just("fallback");
                })
        );

        Flux<String> body = streamBody(controller);

        StepVerifier.create(body)
                .expectNext("fallback")
                .verifyComplete();
        Assertions.assertEquals(1, fallbackCalls.get());
    }

    @Test
    public void doesNotFallBackAfterFirstChunkWasEmitted() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        OpenAiController controller = controller(
                Flux.concat(Flux.just("primary"), Flux.error(retryableError())),
                Flux.defer(() -> {
                    fallbackCalls.incrementAndGet();
                    return Flux.just("fallback");
                })
        );

        Flux<String> body = streamBody(controller);

        StepVerifier.create(body)
                .expectNext("primary")
                .expectError(UpstreamProviderException.class)
                .verify();
        Assertions.assertEquals(0, fallbackCalls.get());
    }

    @SuppressWarnings("unchecked")
    private Flux<String> streamBody(OpenAiController controller) {
        ChatRequest request = new ChatRequest();
        request.setModel("chat-model");
        request.setStream(Boolean.TRUE);
        ResponseEntity<?> response = controller.chatCompletions(request).block();
        Assertions.assertNotNull(response);
        return (Flux<String>) response.getBody();
    }

    private OpenAiController controller(Flux<String> primaryStream, Flux<String> fallbackStream) {
        GatewayProperties properties = new GatewayProperties();
        properties.getProviders().put("primary-provider", provider("primary"));
        properties.getProviders().put("fallback-provider", provider("fallback"));

        GatewayProperties.RouteConfig route = new GatewayProperties.RouteConfig();
        route.setCapability("chat");
        route.setProvider("primary-provider");
        route.setModel("primary-model");
        route.setFallbackProvider("fallback-provider");
        route.setFallbackModel("fallback-model");
        properties.getRoutes().put("chat-model", route);

        ModelProvider primary = new StubProvider("primary", primaryStream);
        ModelProvider fallback = new StubProvider("fallback", fallbackStream);
        return new OpenAiController(
                new ModelRouter(properties),
                new ModelProviderRegistry(Arrays.asList(primary, fallback))
        );
    }

    private GatewayProperties.ProviderConfig provider(String type) {
        GatewayProperties.ProviderConfig provider = new GatewayProperties.ProviderConfig();
        provider.setType(type);
        provider.setBaseUrl("http://127.0.0.1");
        return provider;
    }

    private UpstreamProviderException retryableError() {
        return new UpstreamProviderException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "unavailable",
                MediaType.APPLICATION_JSON
        );
    }

    private static class StubProvider implements ModelProvider {
        private final String type;
        private final Flux<String> stream;

        StubProvider(String type, Flux<String> stream) {
            this.type = type;
            this.stream = stream;
        }

        @Override
        public boolean supports(String providerType) {
            return type.equals(providerType);
        }

        @Override
        public Mono<JsonNode> chat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
            return Mono.empty();
        }

        @Override
        public Flux<String> streamChat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
            return stream;
        }
    }
}
