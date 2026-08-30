package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.SpeechRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface SpeechProvider {

    boolean supports(String providerType);

    Mono<ResponseEntity<byte[]>> speech(SpeechRequest request, GatewayProperties.ProviderConfig provider);
}
