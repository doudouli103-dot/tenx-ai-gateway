package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.document.DocumentCenterClient;
import com.tenx.ai.gateway.document.DocumentUploadResult;
import com.tenx.ai.gateway.model.GeneratedAsset;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import com.tenx.ai.gateway.model.ImageGenerationResponse;
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
    private final DocumentCenterClient documentCenterClient;

    public ImageController(ModelRouter modelRouter,
                           ImageProviderRegistry imageProviderRegistry,
                           DocumentCenterClient documentCenterClient) {
        this.modelRouter = modelRouter;
        this.imageProviderRegistry = imageProviderRegistry;
        this.documentCenterClient = documentCenterClient;
    }

    @PostMapping("/v1/images/generations")
    public Mono<ResponseEntity<ImageGenerationResponse>> generate(@Valid @RequestBody ImageGenerationRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("image")) {
            return Mono.error(new IllegalArgumentException("Model is not configured for image generation: " + request.getModel()));
        }

        ImageProvider provider = imageProviderRegistry.get(route.getProvider().getType());
        return provider.generate(request.copyForModel(route.getModel()), route.getProvider())
                .flatMap(asset -> upload(request, asset))
                .map(uploadResult -> ResponseEntity.ok(toResponse(uploadResult)));
    }

    private Mono<DocumentUploadResult> upload(ImageGenerationRequest request, GeneratedAsset asset) {
        return documentCenterClient.upload("image_generation", request.getModel(), asset);
    }

    private ImageGenerationResponse toResponse(DocumentUploadResult uploadResult) {
        ImageGenerationResponse response = new ImageGenerationResponse();
        response.setCreated(System.currentTimeMillis() / 1000);

        ImageGenerationResponse.ImageData data = new ImageGenerationResponse.ImageData();
        data.setFileId(uploadResult.getFileId());
        data.setUrl(uploadResult.getUrl());
        response.getData().add(data);
        return response;
    }
}
