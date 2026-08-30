package com.tenx.ai.gateway;

import com.tenx.ai.gateway.api.SpeechController;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.SpeechRequest;
import com.tenx.ai.gateway.provider.SpeechProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRouter;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpeechControllerTest {

    @Test
    public void rejectsNonSpeechModel() {
        SpeechController controller = new SpeechController(
                new ModelRouter(sampleProperties()),
                new SpeechProviderRegistry(Collections.emptyList())
        );

        SpeechRequest request = new SpeechRequest();
        request.setModel("qwen3-coder-next");
        request.setInput("hello");

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> controller.speech(request).block()
        );
        Assertions.assertTrue(exception.getMessage().contains("not configured for speech"));
    }

    private GatewayProperties sampleProperties() {
        GatewayProperties properties = new GatewayProperties();

        GatewayProperties.ProviderConfig local = new GatewayProperties.ProviderConfig();
        local.setType("openai-compatible");
        local.setBaseUrl("http://127.0.0.1:4000");
        properties.getProviders().put("local-compatible", local);

        GatewayProperties.RouteConfig route = new GatewayProperties.RouteConfig();
        route.setCapability("chat");
        route.setProvider("local-compatible");
        route.setModel("qwen3-coder-next");
        properties.getRoutes().put("qwen3-coder-next", route);

        return properties;
    }
}
