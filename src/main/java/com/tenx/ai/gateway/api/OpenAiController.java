package com.tenx.ai.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.model.ChatRequest;
import com.tenx.ai.gateway.provider.ModelProvider;
import com.tenx.ai.gateway.provider.ModelProviderRegistry;
import com.tenx.ai.gateway.provider.UpstreamProviderException;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * OpenAI 兼容的聊天入口，处理 {@code /v1/chat/completions} 与 {@code /healthz}。
 *
 * <p>核心逻辑：
 * <ol>
 *   <li>按请求模型名解析路由，校验 capability 必须是 chat。</li>
 *   <li>根据请求体 {@code stream} 字段选择同步或流式转发。</li>
 *   <li>转发到主 provider；若配置了 fallback 且主 provider 发生<b>临时性</b>错误（5xx / 429 / 网络故障），
 *       则切换到备用 provider。4xx 客户端错误不会回落（回落也解决不了）。</li>
 * </ol>
 */
@RestController
public class OpenAiController {

    /** 路由解析器，把请求模型名解析成路由。 */
    private final ModelRouter modelRouter;

    /** 聊天 provider 注册表，按类型取 provider 实现。 */
    private final ModelProviderRegistry providerRegistry;

    /** 构造聊天入口。 */
    public OpenAiController(ModelRouter modelRouter, ModelProviderRegistry providerRegistry) {
        this.modelRouter = modelRouter;
        this.providerRegistry = providerRegistry;
    }

    /** 聊天补全入口：解析路由、校验能力，按 stream 分流到同步或流式转发。 */
    @PostMapping(value = "/v1/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody ChatRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("chat")) {
            return Mono.error(new IllegalArgumentException("Model is not configured for chat completions: " + request.getModel()));
        }
        if (request.isStream()) {
            Flux<String> stream = streamWithRoute(request, route);
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(stream));
        }

        return chatWithRoute(request, route).map(ResponseEntity::ok);
    }

    /** 健康检查入口，无需鉴权。 */
    @GetMapping("/healthz")
    public Mono<ResponseEntity<String>> healthz() {
        return Mono.just(ResponseEntity.ok("ok"));
    }

    /** 同步聊天：转发到主 provider，失败时按需回落到备用 provider。 */
    private Mono<JsonNode> chatWithRoute(ChatRequest request, ModelRoute route) {
        ModelProvider provider = providerRegistry.get(route.getProvider().getType());
        Mono<JsonNode> primary = provider.chat(request.copyForModel(route.getModel()), route.getProvider());
        if (!route.hasFallback()) {
            return primary;
        }

        return primary.onErrorResume(error -> {
            if (!isRetryable(error)) {
                return Mono.error(error);
            }
            ModelProvider fallbackProvider = providerRegistry.get(route.getFallbackProvider().getType());
            return fallbackProvider.chat(request.copyForModel(route.getFallbackModel()), route.getFallbackProvider());
        });
    }

    /** 流式聊天（SSE）：转发为事件流，失败时按需回落到备用 provider。 */
    private Flux<String> streamWithRoute(ChatRequest request, ModelRoute route) {
        ModelProvider provider = providerRegistry.get(route.getProvider().getType());
        Flux<String> primary = provider.streamChat(request.copyForModel(route.getModel()), route.getProvider());
        if (!route.hasFallback()) {
            return primary;
        }

        return primary.onErrorResume(error -> {
            if (!isRetryable(error)) {
                return Flux.error(error);
            }
            ModelProvider fallbackProvider = providerRegistry.get(route.getFallbackProvider().getType());
            return fallbackProvider.streamChat(request.copyForModel(route.getFallbackModel()), route.getFallbackProvider());
        });
    }

    /**
     * 判断某个错误是否值得回落：
     * 上游返回的 5xx/429 可回落；4xx 客户端错误不可回落；其它（连接失败、超时等）默认可回落。
     */
    private boolean isRetryable(Throwable error) {
        if (error instanceof UpstreamProviderException) {
            return ((UpstreamProviderException) error).isRetryable();
        }
        // Connection failures, timeouts, and other transport errors are worth a fallback.
        return true;
    }
}
