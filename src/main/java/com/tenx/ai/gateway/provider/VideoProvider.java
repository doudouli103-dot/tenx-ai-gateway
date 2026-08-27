package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.GeneratedAsset;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import reactor.core.publisher.Mono;

public interface VideoProvider {

    boolean supports(String providerType);

    Mono<GeneratedAsset> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider);
}
