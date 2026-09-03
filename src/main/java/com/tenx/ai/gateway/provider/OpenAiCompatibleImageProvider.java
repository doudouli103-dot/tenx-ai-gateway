package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * OpenAI 兼容的图像 provider，把请求转发到上游的 {@code /v1/images/generations}（如 image-adapter）。
 */
@Component
public class OpenAiCompatibleImageProvider implements ImageProvider {

    /** WebClient 工厂，按 provider 配置取得缓存的 WebClient。 */
    private final ProviderWebClientFactory webClientFactory;

    /** 构造 OpenAI 兼容图像 provider。 */
    public OpenAiCompatibleImageProvider(ProviderWebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    /** 支持 openai-image-compatible 类型。 */
    @Override
    public boolean supports(String providerType) {
        return "openai-image-compatible".equalsIgnoreCase(providerType);
    }

    /** 图像生成：POST 到上游 /v1/images/generations，非 2xx 转成 UpstreamProviderException 透传。 */
    @Override
    public Mono<JsonNode> generate(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus::isError, UpstreamProviderException::fromResponse)
                .bodyToMono(JsonNode.class);
    }

}
