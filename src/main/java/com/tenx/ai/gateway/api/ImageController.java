package com.tenx.ai.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import com.tenx.ai.gateway.provider.ImageProvider;
import com.tenx.ai.gateway.provider.ImageProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * OpenAI 兼容的图像生成入口，处理 {@code /v1/images/generations}。
 *
 * <p>流程：解析路由 → 校验 capability 为 image → 转发给对应 provider → 原样返回上游响应。
 * 网关本身不生成图像，也不保存图像文件。
 */
@RestController
public class ImageController {

    /** 路由解析器，把请求模型名解析成路由。 */
    private final ModelRouter modelRouter;

    /** 图像 provider 注册表，按类型取 provider 实现。 */
    private final ImageProviderRegistry imageProviderRegistry;

    /** 构造图像生成入口。 */
    public ImageController(ModelRouter modelRouter,
                           ImageProviderRegistry imageProviderRegistry) {
        this.modelRouter = modelRouter;
        this.imageProviderRegistry = imageProviderRegistry;
    }

    /** 图像生成入口：解析路由、校验能力，转发给对应 provider 并原样返回。 */
    @PostMapping("/v1/images/generations")
    public Mono<ResponseEntity<JsonNode>> generate(@Valid @RequestBody ImageGenerationRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("image")) {
            return Mono.error(new IllegalArgumentException("Model is not configured for image generation: " + request.getModel()));
        }

        ImageProvider provider = imageProviderRegistry.get(route.getProvider().getType());
        return provider.generate(request.copyForModel(route.getModel()), route.getProvider())
                .map(ResponseEntity::ok);
    }
}
