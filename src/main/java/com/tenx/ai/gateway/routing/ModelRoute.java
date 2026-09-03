package com.tenx.ai.gateway.routing;

import com.tenx.ai.gateway.config.GatewayProperties;

/**
 * 一条解析完成的路由：描述「调用方请求的模型」应该转发到哪个上游、用什么真实模型名、以及失败时的备用方案。
 *
 * <p>由 {@link ModelRouter#route(String)} 生成，是路由层与 Controller/Provider 层之间的传递对象。
 */
public class ModelRoute {

    /** 调用方在请求里填的模型名（路由配置的 key）。 */
    private final String requestedModel;

    /** 能力类型：chat / image / video。 */
    private final String capability;

    /** 主 provider 名。 */
    private final String providerName;

    /** 转发给主 provider 时使用的真实模型名。 */
    private final String model;

    /** 主 provider 的连接配置。 */
    private final GatewayProperties.ProviderConfig provider;

    /** 备用 provider 名（可选）。 */
    private final String fallbackProviderName;

    /** 备用 provider 使用的真实模型名（可选）。 */
    private final String fallbackModel;

    /** 备用 provider 的连接配置（可选）。 */
    private final GatewayProperties.ProviderConfig fallbackProvider;

    /** 视频默认时长（秒），可为空。 */
    private final Integer defaultDurationSeconds;

    /** 视频最大时长（秒），可为空。 */
    private final Integer maxDurationSeconds;

    /** 构造一条完整路由。 */
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

    /** 返回调用方请求的模型名。 */
    public String getRequestedModel() {
        return requestedModel;
    }

    /** 返回能力类型。 */
    public String getCapability() {
        return capability;
    }

    /** 返回主 provider 名。 */
    public String getProviderName() {
        return providerName;
    }

    /** 返回转发用的真实模型名。 */
    public String getModel() {
        return model;
    }

    /** 返回主 provider 连接配置。 */
    public GatewayProperties.ProviderConfig getProvider() {
        return provider;
    }

    /** 返回备用 provider 名（可选）。 */
    public String getFallbackProviderName() {
        return fallbackProviderName;
    }

    /** 返回备用 provider 使用的真实模型名（可选）。 */
    public String getFallbackModel() {
        return fallbackModel;
    }

    /** 返回备用 provider 连接配置（可选）。 */
    public GatewayProperties.ProviderConfig getFallbackProvider() {
        return fallbackProvider;
    }

    /** 返回视频默认时长（秒）。 */
    public Integer getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    /** 返回视频最大时长（秒）。 */
    public Integer getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    /**
     * 是否配置了可用 fallback（备用 provider 和备用模型名都存在）。
     * 只有返回 true 时，请求失败才可能切换到备用 provider。
     */
    public boolean hasFallback() {
        return fallbackProvider != null && fallbackModel != null && fallbackModel.trim().length() > 0;
    }

    /** 判断该路由是否属于某类能力（不区分大小写）。 */
    public boolean isCapability(String expectedCapability) {
        return expectedCapability != null && expectedCapability.equalsIgnoreCase(capability);
    }
}
