package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import com.tenx.ai.gateway.routing.UnknownModelException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModelRouterTest {

    @Test
    public void routeResolvesModelNameToProviderAndRealModel() {
        GatewayProperties properties = sampleProperties();
        ModelRouter router = new ModelRouter(properties);

        ModelRoute route = router.route("qwen3-coder-next");

        Assertions.assertEquals("qwen3-coder-next", route.getRequestedModel());
        Assertions.assertEquals("local-compatible", route.getProviderName());
        Assertions.assertEquals("qwen3-coder-next", route.getModel());
        Assertions.assertEquals("openai-compatible", route.getProvider().getType());
        Assertions.assertTrue(route.hasFallback());
        Assertions.assertEquals("cloud-openai", route.getFallbackProviderName());
        Assertions.assertEquals("gpt-5", route.getFallbackModel());
    }

    @Test
    public void routeRejectsUnknownModelName() {
        GatewayProperties properties = sampleProperties();
        ModelRouter router = new ModelRouter(properties);

        Assertions.assertThrows(UnknownModelException.class, () -> router.route("missing"));
    }

    private GatewayProperties sampleProperties() {
        GatewayProperties properties = new GatewayProperties();

        GatewayProperties.ProviderConfig local = new GatewayProperties.ProviderConfig();
        local.setType("openai-compatible");
        local.setBaseUrl("http://127.0.0.1:4000");
        properties.getProviders().put("local-compatible", local);

        GatewayProperties.ProviderConfig cloud = new GatewayProperties.ProviderConfig();
        cloud.setType("openai-compatible");
        cloud.setBaseUrl("https://api.openai.com");
        properties.getProviders().put("cloud-openai", cloud);

        GatewayProperties.RouteConfig qwenCoder = new GatewayProperties.RouteConfig();
        qwenCoder.setProvider("local-compatible");
        qwenCoder.setModel("qwen3-coder-next");
        qwenCoder.setFallbackProvider("cloud-openai");
        qwenCoder.setFallbackModel("gpt-5");
        properties.getRoutes().put("qwen3-coder-next", qwenCoder);

        return properties;
    }
}
