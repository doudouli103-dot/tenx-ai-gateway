package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.document.DocumentCenterClient;
import com.tenx.ai.gateway.document.DocumentUploadResult;
import com.tenx.ai.gateway.model.GeneratedAsset;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import com.tenx.ai.gateway.provider.VideoProvider;
import com.tenx.ai.gateway.provider.VideoProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRouter;
import com.tenx.ai.gateway.video.VideoTaskService;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class VideoTaskServiceTest {

    @Test
    public void rejectsDurationGreaterThanConfiguredMax() {
        VideoTaskService service = new VideoTaskService(
                new ModelRouter(sampleProperties()),
                new VideoProviderRegistry(Arrays.asList(new StubVideoProvider())),
                new StubDocumentCenterClient()
        );

        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setModel("wan2.2-ti2v-5b");
        request.setPrompt("test");
        request.setDuration(Integer.valueOf(6));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.submit(request)
        );
        Assertions.assertTrue(exception.getMessage().contains("max duration 5 seconds"));
    }

    private GatewayProperties sampleProperties() {
        GatewayProperties properties = new GatewayProperties();

        GatewayProperties.ProviderConfig video = new GatewayProperties.ProviderConfig();
        video.setType("openai-video-compatible");
        video.setBaseUrl("http://127.0.0.1:4020");
        properties.getProviders().put("video-compatible", video);

        GatewayProperties.RouteConfig route = new GatewayProperties.RouteConfig();
        route.setCapability("video");
        route.setProvider("video-compatible");
        route.setModel("wan2.2-ti2v-5b");
        route.setDefaultDurationSeconds(Integer.valueOf(5));
        route.setMaxDurationSeconds(Integer.valueOf(5));
        properties.getRoutes().put("wan2.2-ti2v-5b", route);

        return properties;
    }

    private static class StubVideoProvider implements VideoProvider {
        @Override
        public boolean supports(String providerType) {
            return "openai-video-compatible".equals(providerType);
        }

        @Override
        public Mono<GeneratedAsset> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider) {
            return Mono.just(new GeneratedAsset("video".getBytes(), "test.mp4", "video/mp4"));
        }
    }

    private static class StubDocumentCenterClient extends DocumentCenterClient {
        StubDocumentCenterClient() {
            super(new GatewayProperties(), null, null);
        }

        @Override
        public Mono<DocumentUploadResult> upload(String bizType, String sourceModel, GeneratedAsset asset) {
            DocumentUploadResult result = new DocumentUploadResult();
            result.setFileId("file-1");
            result.setUrl("http://doc-center/files/file-1");
            return Mono.just(result);
        }
    }
}
