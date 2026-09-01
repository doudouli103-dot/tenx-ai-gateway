package com.tenx.ai.gateway.runtime;

public class ModelRuntimeStatus {

    private String id;
    private String capability;
    private String provider;
    private String targetModel;
    private String healthUrl;
    private boolean runtimeConfigured;
    private String status;

    public ModelRuntimeStatus(String id, String capability, String provider, String targetModel,
                              String healthUrl, boolean runtimeConfigured, String status) {
        this.id = id;
        this.capability = capability;
        this.provider = provider;
        this.targetModel = targetModel;
        this.healthUrl = healthUrl;
        this.runtimeConfigured = runtimeConfigured;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getCapability() {
        return capability;
    }

    public String getProvider() {
        return provider;
    }

    public String getTargetModel() {
        return targetModel;
    }

    public String getHealthUrl() {
        return healthUrl;
    }

    public boolean isRuntimeConfigured() {
        return runtimeConfigured;
    }

    public String getStatus() {
        return status;
    }
}
