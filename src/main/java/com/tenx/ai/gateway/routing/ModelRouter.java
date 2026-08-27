package com.tenx.ai.gateway.routing;

import com.tenx.ai.gateway.config.GatewayProperties;
import org.springframework.stereotype.Component;

@Component
public class ModelRouter {

    private final GatewayProperties properties;

    public ModelRouter(GatewayProperties properties) {
        this.properties = properties;
    }

    public ModelRoute route(String requestedModel) {
        GatewayProperties.RouteConfig routeConfig = properties.getRoutes().get(requestedModel);
        if (routeConfig == null) {
            throw new UnknownModelException(requestedModel);
        }

        GatewayProperties.ProviderConfig provider = properties.getProviders().get(routeConfig.getProvider());
        if (provider == null) {
            throw new IllegalStateException("Provider not configured: " + routeConfig.getProvider());
        }

        GatewayProperties.ProviderConfig fallbackProvider = null;
        if (routeConfig.getFallbackProvider() != null && routeConfig.getFallbackProvider().trim().length() > 0) {
            fallbackProvider = properties.getProviders().get(routeConfig.getFallbackProvider());
            if (fallbackProvider == null) {
                throw new IllegalStateException("Fallback provider not configured: " + routeConfig.getFallbackProvider());
            }
        }

        return new ModelRoute(
                requestedModel,
                routeConfig.getCapability(),
                routeConfig.getProvider(),
                routeConfig.getModel(),
                provider,
                routeConfig.getFallbackProvider(),
                routeConfig.getFallbackModel(),
                fallbackProvider,
                routeConfig.getDefaultDurationSeconds(),
                routeConfig.getMaxDurationSeconds()
        );
    }
}
