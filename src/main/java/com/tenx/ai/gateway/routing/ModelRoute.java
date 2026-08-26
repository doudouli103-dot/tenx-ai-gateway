package com.tenx.ai.gateway.routing;

import com.tenx.ai.gateway.config.GatewayProperties;

public class ModelRoute {

    private final String alias;
    private final String providerName;
    private final String model;
    private final GatewayProperties.ProviderConfig provider;
    private final String fallbackProviderName;
    private final String fallbackModel;
    private final GatewayProperties.ProviderConfig fallbackProvider;

    public ModelRoute(String alias, String providerName, String model,
                      GatewayProperties.ProviderConfig provider,
                      String fallbackProviderName, String fallbackModel,
                      GatewayProperties.ProviderConfig fallbackProvider) {
        this.alias = alias;
        this.providerName = providerName;
        this.model = model;
        this.provider = provider;
        this.fallbackProviderName = fallbackProviderName;
        this.fallbackModel = fallbackModel;
        this.fallbackProvider = fallbackProvider;
    }

    public String getAlias() {
        return alias;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getModel() {
        return model;
    }

    public GatewayProperties.ProviderConfig getProvider() {
        return provider;
    }

    public String getFallbackProviderName() {
        return fallbackProviderName;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public GatewayProperties.ProviderConfig getFallbackProvider() {
        return fallbackProvider;
    }

    public boolean hasFallback() {
        return fallbackProvider != null && fallbackModel != null && fallbackModel.trim().length() > 0;
    }
}
