package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ChatRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OpenAiCompatibleProvider implements ModelProvider {

    private final ProviderWebClientFactory webClientFactory;

    public OpenAiCompatibleProvider(ProviderWebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    public boolean supports(String providerType) {
        return "openai-compatible".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<JsonNode> chat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    @Override
    public Flux<String> streamChat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);
    }

}
