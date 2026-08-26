package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ChatRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OpenAiCompatibleProvider implements ModelProvider {

    private final WebClient.Builder webClientBuilder;

    public OpenAiCompatibleProvider(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public boolean supports(String providerType) {
        return "openai-compatible".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<JsonNode> chat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
        return client(provider)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    @Override
    public Flux<String> streamChat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
        return client(provider)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);
    }

    private WebClient client(GatewayProperties.ProviderConfig provider) {
        WebClient.Builder builder = webClientBuilder.clone().baseUrl(provider.getBaseUrl());
        if (provider.getApiKey() != null && provider.getApiKey().trim().length() > 0) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey());
        }
        return builder.build();
    }
}
