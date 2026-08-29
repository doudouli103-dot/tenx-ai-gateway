package com.tenx.ai.gateway;

import com.tenx.ai.gateway.api.VideoController;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import com.tenx.ai.gateway.provider.VideoProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRouter;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VideoControllerTest {

    @Test
    public void rejectsDurationGreaterThanConfiguredMax() {
        VideoController controller = new VideoController(
                new ModelRouter(sampleProperties()),
                new VideoProviderRegistry(Collections.emptyList())
        );

        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setModel("Wan2.2-TI2V-5B");
        request.setPrompt("test");
        request.setDuration(Integer.valueOf(6));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> controller.generate(request)
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
        route.setModel("Wan2.2-TI2V-5B");
        route.setDefaultDurationSeconds(Integer.valueOf(5));
        route.setMaxDurationSeconds(Integer.valueOf(5));
        properties.getRoutes().put("Wan2.2-TI2V-5B", route);

        return properties;
    }
}
