package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.GeneratedAsset;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAiCompatibleVideoProvider implements VideoProvider {

    private final WebClient.Builder webClientBuilder;

    public OpenAiCompatibleVideoProvider(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public boolean supports(String providerType) {
        return "openai-video-compatible".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<GeneratedAsset> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return client(provider)
                .post()
                .uri("/v1/videos/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(this::extractVideo);
    }

    private Mono<GeneratedAsset> extractVideo(JsonNode response) {
        JsonNode first = response.path("data").isArray() && response.path("data").size() > 0
                ? response.path("data").get(0)
                : response;

        JsonNode b64Json = first.get("b64_json");
        if (b64Json != null && !b64Json.isNull()) {
            byte[] bytes = Base64.getDecoder().decode(b64Json.asText());
            return Mono.just(new GeneratedAsset(bytes, randomName("mp4"), "video/mp4"));
        }

        JsonNode url = first.get("url");
        if (url == null || url.isNull()) {
            url = first.get("video_url");
        }
        if (url != null && !url.isNull()) {
            return download(url.asText(), "video/mp4", "mp4");
        }

        return Mono.error(new IllegalStateException("Video provider response must contain data[0].b64_json, data[0].url, or data[0].video_url"));
    }

    private Mono<GeneratedAsset> download(String url, String contentType, String extension) {
        return webClientBuilder.clone()
                .build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .map(bytes -> new GeneratedAsset(bytes, randomName(extension), contentType));
    }

    private WebClient client(GatewayProperties.ProviderConfig provider) {
        WebClient.Builder builder = webClientBuilder.clone().baseUrl(provider.getBaseUrl());
        if (provider.getApiKey() != null && provider.getApiKey().trim().length() > 0) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey());
        }
        return builder.build();
    }

    private String randomName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }
}
