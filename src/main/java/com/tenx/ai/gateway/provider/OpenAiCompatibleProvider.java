package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ChatRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * OpenAI 兼容的聊天 provider，把请求转发到上游的 {@code /v1/chat/completions}。
 * 适用于本地 llama.cpp、LiteLLM、vLLM、云端 OpenAI 等所有 OpenAI 兼容服务。
 */
@Component
public class OpenAiCompatibleProvider implements ModelProvider {

    /** WebClient 工厂，按 provider 配置取得缓存的 WebClient。 */
    private final ProviderWebClientFactory webClientFactory;

    /** 构造 OpenAI 兼容聊天 provider。 */
    public OpenAiCompatibleProvider(ProviderWebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    /** 支持 openai-compatible 类型。 */
    @Override
    public boolean supports(String providerType) {
        return "openai-compatible".equalsIgnoreCase(providerType);
    }

    /** 同步聊天：POST 到上游 /v1/chat/completions，非 2xx 转成 UpstreamProviderException 透传。 */
    @Override
    public Mono<JsonNode> chat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus::isError, UpstreamProviderException::fromResponse)
                .bodyToMono(JsonNode.class);
    }

    /** 流式聊天：POST 到上游 /v1/chat/completions 并以 SSE 事件流返回。 */
    @Override
    public Flux<String> streamChat(ChatRequest request, GatewayProperties.ProviderConfig provider) {
        return webClientFactory.client(provider)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus::isError, UpstreamProviderException::fromResponse)
                .bodyToFlux(String.class);
    }

}
