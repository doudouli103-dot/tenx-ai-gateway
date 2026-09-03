package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * OpenAI 兼容的视频 provider，把请求转发到上游的 {@code /v1/videos/generations}（如 video-adapter）。
 */
@Component
public class OpenAiCompatibleVideoProvider implements VideoProvider {

    /** WebClient 工厂，按 provider 配置取得缓存的 WebClient。 */
    private final ProviderWebClientFactory webClientFactory;

    /** 构造 OpenAI 兼容视频 provider。 */
    public OpenAiCompatibleVideoProvider(ProviderWebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    /** 支持 openai-video-compatible 类型。 */
    @Override
    public boolean supports(String providerType) {
        return "openai-video-compatible".equalsIgnoreCase(providerType);
    }

    /** 视频生成：POST 到上游 /v1/videos/generations，非 2xx 转成 UpstreamProviderException 透传。 */
    @Override
    public Mono<JsonNode> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/videos/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus::isError, UpstreamProviderException::fromResponse)
                .bodyToMono(JsonNode.class);
    }

}
