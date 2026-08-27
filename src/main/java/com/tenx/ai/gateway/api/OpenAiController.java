package com.tenx.ai.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.model.ChatRequest;
import com.tenx.ai.gateway.provider.ModelProvider;
import com.tenx.ai.gateway.provider.ModelProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class OpenAiController {

    private final ModelRouter modelRouter;
    private final ModelProviderRegistry providerRegistry;

    public OpenAiController(ModelRouter modelRouter, ModelProviderRegistry providerRegistry) {
        this.modelRouter = modelRouter;
        this.providerRegistry = providerRegistry;
    }

    @PostMapping(value = "/v1/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody ChatRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("chat")) {
            return Mono.error(new IllegalArgumentException("Model is not configured for chat completions: " + request.getModel()));
        }
        if (request.isStream()) {
            Flux<String> stream = streamWithRoute(request, route);
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(stream));
        }

        return chatWithRoute(request, route).map(ResponseEntity::ok);
    }

    @GetMapping("/healthz")
    public Mono<ResponseEntity<String>> healthz() {
        return Mono.just(ResponseEntity.ok("ok"));
    }

    private Mono<JsonNode> chatWithRoute(ChatRequest request, ModelRoute route) {
        ModelProvider provider = providerRegistry.get(route.getProvider().getType());
        Mono<JsonNode> primary = provider.chat(request.copyForModel(route.getModel()), route.getProvider());
        if (!route.hasFallback()) {
            return primary;
        }

        return primary.onErrorResume(error -> {
            ModelProvider fallbackProvider = providerRegistry.get(route.getFallbackProvider().getType());
            return fallbackProvider.chat(request.copyForModel(route.getFallbackModel()), route.getFallbackProvider());
        });
    }

    private Flux<String> streamWithRoute(ChatRequest request, ModelRoute route) {
        ModelProvider provider = providerRegistry.get(route.getProvider().getType());
        Flux<String> primary = provider.streamChat(request.copyForModel(route.getModel()), route.getProvider());
        if (!route.hasFallback()) {
            return primary;
        }

        return primary.onErrorResume(error -> {
            ModelProvider fallbackProvider = providerRegistry.get(route.getFallbackProvider().getType());
            return fallbackProvider.streamChat(request.copyForModel(route.getFallbackModel()), route.getFallbackProvider());
        });
    }
}
