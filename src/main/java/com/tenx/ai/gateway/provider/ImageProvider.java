package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import reactor.core.publisher.Mono;

/**
 * 图像生成 provider 的统一接口。网关只转发，不生成、不保存图像。
 */
public interface ImageProvider {

    /** 是否支持某类 provider 类型。 */
    boolean supports(String providerType);

    /** 图像生成：转发请求并返回上游响应。 */
    Mono<JsonNode> generate(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider);
}
