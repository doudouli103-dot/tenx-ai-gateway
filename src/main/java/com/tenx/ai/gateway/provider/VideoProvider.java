package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import reactor.core.publisher.Mono;

public interface VideoProvider {

    boolean supports(String providerType);

    Mono<JsonNode> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider);
}
