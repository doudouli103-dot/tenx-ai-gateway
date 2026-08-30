package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.model.SpeechRequest;
import com.tenx.ai.gateway.provider.SpeechProvider;
import com.tenx.ai.gateway.provider.SpeechProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SpeechController {

    private final ModelRouter modelRouter;
    private final SpeechProviderRegistry speechProviderRegistry;

    public SpeechController(ModelRouter modelRouter, SpeechProviderRegistry speechProviderRegistry) {
        this.modelRouter = modelRouter;
        this.speechProviderRegistry = speechProviderRegistry;
    }

    @PostMapping("/v1/audio/speech")
    public Mono<ResponseEntity<byte[]>> speech(@Valid @RequestBody SpeechRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("speech")) {
            return Mono.error(new IllegalArgumentException("Model is not configured for speech generation: " + request.getModel()));
        }

        SpeechProvider provider = speechProviderRegistry.get(route.getProvider().getType());
        return provider.speech(request.copyForModel(route.getModel()), route.getProvider());
    }
}
