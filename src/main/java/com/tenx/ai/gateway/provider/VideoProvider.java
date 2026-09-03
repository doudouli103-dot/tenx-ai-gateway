package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import reactor.core.publisher.Mono;

/**
 * 视频生成 provider 的统一接口。网关只转发，不生成、不保存视频。
 */
public interface VideoProvider {

    /** 是否支持某类 provider 类型。 */
    boolean supports(String providerType);

    /** 视频生成：转发请求并返回上游响应。 */
    Mono<JsonNode> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider);
}
