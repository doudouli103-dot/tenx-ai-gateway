package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OpenAiCompatibleVideoProvider implements VideoProvider {

    private final ProviderWebClientFactory webClientFactory;

    public OpenAiCompatibleVideoProvider(ProviderWebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    public boolean supports(String providerType) {
        return "openai-video-compatible".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<JsonNode> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/videos/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

}
