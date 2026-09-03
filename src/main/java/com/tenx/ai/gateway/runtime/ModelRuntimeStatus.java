package com.tenx.ai.gateway.runtime;

/**
 * 单个模型的运行时状态视图（admin 接口返回项）。
 */
public class ModelRuntimeStatus {

    /** 模型名（路由配置的 key）。 */
    private String id;

    /** 能力类型：chat / image / video。 */
    private String capability;

    /** provider 名。 */
    private String provider;

    /** 转发给上游时使用的真实模型名。 */
    private String targetModel;

    /** 健康检查地址（可能为空）。 */
    private String healthUrl;

    /** 是否配置了运行时控制（有无 runtimes 配置）。 */
    private boolean runtimeConfigured;

    /** 状态：online / offline / unmanaged / unknown。 */
    private String status;

    /** 构造状态视图。 */
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

    /** 返回模型名。 */
    public String getId() {
        return id;
    }

    /** 返回能力类型。 */
    public String getCapability() {
        return capability;
    }

    /** 返回 provider 名。 */
    public String getProvider() {
        return provider;
    }

    /** 返回真实模型名。 */
    public String getTargetModel() {
        return targetModel;
    }

    /** 返回健康检查地址。 */
    public String getHealthUrl() {
        return healthUrl;
    }

    /** 返回是否配置了运行时控制。 */
    public boolean isRuntimeConfigured() {
        return runtimeConfigured;
    }

    /** 返回运行时状态。 */
    public String getStatus() {
        return status;
    }
}
