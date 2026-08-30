package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.SpeechRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAiCompatibleSpeechProvider implements SpeechProvider {

    private final WebClient.Builder webClientBuilder;

    public OpenAiCompatibleSpeechProvider(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public boolean supports(String providerType) {
        return "openai-audio-compatible".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<ResponseEntity<byte[]>> speech(SpeechRequest request, GatewayProperties.ProviderConfig provider) {
        return client(provider)
                .post()
                .uri("/v1/audio/speech")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toEntity(byte[].class);
    }

    private WebClient client(GatewayProperties.ProviderConfig provider) {
        WebClient.Builder builder = webClientBuilder.clone().baseUrl(provider.getBaseUrl());
        if (provider.getApiKey() != null && provider.getApiKey().trim().length() > 0) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey());
        }
        return builder.build();
    }
}
