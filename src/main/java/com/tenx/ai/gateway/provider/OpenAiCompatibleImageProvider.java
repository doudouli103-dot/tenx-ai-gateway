package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OpenAiCompatibleImageProvider implements ImageProvider {

    private final ProviderWebClientFactory webClientFactory;

    public OpenAiCompatibleImageProvider(ProviderWebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    @Override
    public boolean supports(String providerType) {
        return "openai-image-compatible".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<JsonNode> generate(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

}
