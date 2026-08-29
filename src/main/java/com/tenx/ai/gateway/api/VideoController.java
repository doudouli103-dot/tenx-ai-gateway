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

@RestController
public class VideoController {

    private final ModelRouter modelRouter;
    private final VideoProviderRegistry videoProviderRegistry;

    public VideoController(ModelRouter modelRouter, VideoProviderRegistry videoProviderRegistry) {
        this.modelRouter = modelRouter;
        this.videoProviderRegistry = videoProviderRegistry;
    }

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
