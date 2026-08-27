package com.tenx.ai.gateway.routing;

import com.tenx.ai.gateway.config.GatewayProperties;

public class ModelRoute {

    private final String requestedModel;
    private final String capability;
    private final String providerName;
    private final String model;
    private final GatewayProperties.ProviderConfig provider;
    private final String fallbackProviderName;
    private final String fallbackModel;
    private final GatewayProperties.ProviderConfig fallbackProvider;
    private final Integer defaultDurationSeconds;
    private final Integer maxDurationSeconds;

    public ModelRoute(String requestedModel, String capability, String providerName, String model,
                      GatewayProperties.ProviderConfig provider,
                      String fallbackProviderName, String fallbackModel,
                      GatewayProperties.ProviderConfig fallbackProvider,
                      Integer defaultDurationSeconds, Integer maxDurationSeconds) {
        this.requestedModel = requestedModel;
        this.capability = capability;
        this.providerName = providerName;
        this.model = model;
        this.provider = provider;
        this.fallbackProviderName = fallbackProviderName;
        this.fallbackModel = fallbackModel;
        this.fallbackProvider = fallbackProvider;
        this.defaultDurationSeconds = defaultDurationSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public String getRequestedModel() {
        return requestedModel;
    }

    public String getCapability() {
        return capability;
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

    public Integer getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public Integer getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public boolean hasFallback() {
        return fallbackProvider != null && fallbackModel != null && fallbackModel.trim().length() > 0;
    }

    public boolean isCapability(String expectedCapability) {
        return expectedCapability != null && expectedCapability.equalsIgnoreCase(capability);
    }
}
