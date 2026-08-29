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

@RestController
public class ImageController {

    private final ModelRouter modelRouter;
    private final ImageProviderRegistry imageProviderRegistry;

    public ImageController(ModelRouter modelRouter,
                           ImageProviderRegistry imageProviderRegistry) {
        this.modelRouter = modelRouter;
        this.imageProviderRegistry = imageProviderRegistry;
    }

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
