package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.GeneratedAsset;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import reactor.core.publisher.Mono;

public interface ImageProvider {

    boolean supports(String providerType);

    Mono<GeneratedAsset> generate(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider);
}
