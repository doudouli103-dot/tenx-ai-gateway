package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import reactor.core.publisher.Mono;

public interface ImageProvider {

    boolean supports(String providerType);

    Mono<JsonNode> generate(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider);
}
