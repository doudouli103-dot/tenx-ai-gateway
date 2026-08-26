package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ChatRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelProvider {

    boolean supports(String providerType);

    Mono<JsonNode> chat(ChatRequest request, GatewayProperties.ProviderConfig provider);

    Flux<String> streamChat(ChatRequest request, GatewayProperties.ProviderConfig provider);
}
