package com.tenx.ai.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import com.tenx.ai.gateway.provider.VideoProvider;
import com.tenx.ai.gateway.provider.VideoProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * OpenAI 兼容的视频生成入口，处理 {@code /v1/videos/generations}。
 *
 * <p>流程：解析路由 → 校验 capability 为 video → 解析并校验时长 → 转发给对应 provider。
 * 网关本身不生成视频，也不保存视频文件。
 */
@RestController
public class VideoController {

    /** 路由解析器，把请求模型名解析成路由。 */
    private final ModelRouter modelRouter;

    /** 视频 provider 注册表，按类型取 provider 实现。 */
    private final VideoProviderRegistry videoProviderRegistry;

    /** 构造视频生成入口。 */
    public VideoController(ModelRouter modelRouter, VideoProviderRegistry videoProviderRegistry) {
        this.modelRouter = modelRouter;
        this.videoProviderRegistry = videoProviderRegistry;
    }

    /** 视频生成入口：解析路由、校验能力、解析时长，转发给对应 provider。 */
    @PostMapping("/v1/videos/generations")
    public Mono<ResponseEntity<JsonNode>> generate(@Valid @RequestBody VideoGenerationRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("video")) {
            return Mono.error(new IllegalArgumentException("Model is not configured for video generation: " + request.getModel()));
        }

        VideoProvider provider = videoProviderRegistry.get(route.getProvider().getType());
        return provider.generate(request.copyForModel(route.getModel(), resolveDuration(request, route)), route.getProvider())
                .map(ResponseEntity::ok);
    }

    /**
     * 解析视频时长：请求未传则用路由默认值，仍为空则用 5 秒；
     * 超过路由配置的最大时长则拒绝请求。
     */
    private Integer resolveDuration(VideoGenerationRequest request, ModelRoute route) {
        Integer duration = request.getDuration();
        if (duration == null) {
            duration = route.getDefaultDurationSeconds();
        }
        if (duration == null) {
            duration = Integer.valueOf(5);
        }

        Integer maxDuration = route.getMaxDurationSeconds();
        if (maxDuration != null && duration.intValue() > maxDuration.intValue()) {
            throw new IllegalArgumentException("Model " + request.getModel() + " supports max duration " + maxDuration + " seconds");
        }
        return duration;
    }
}
