package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ChatRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 聊天模型 provider 的统一接口。不同上游（本地 llama.cpp、云端 OpenAI 等）实现各自的转发逻辑。
 */
public interface ModelProvider {

    /** 是否支持某类 provider 类型（对应配置里的 {@code provider.type}）。 */
    boolean supports(String providerType);

    /** 同步聊天：返回上游完整 JSON 响应。 */
    Mono<JsonNode> chat(ChatRequest request, GatewayProperties.ProviderConfig provider);

    /** 流式聊天：返回 SSE 事件流（每个元素是一段文本）。 */
    Flux<String> streamChat(ChatRequest request, GatewayProperties.ProviderConfig provider);
}
