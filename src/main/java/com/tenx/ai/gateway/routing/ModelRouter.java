package com.tenx.ai.gateway.routing;

import com.tenx.ai.gateway.config.GatewayProperties;
import org.springframework.stereotype.Component;

/**
 * 把「调用方请求里的模型名」解析成一条完整路由（{@link ModelRoute}）。
 *
 * <p>解析过程：用请求模型名查 {@code routes} 配置 → 得到 capability / 主 provider / 真实模型名 /
 * fallback 信息 → 再从 {@code providers} 配置查出 provider 的具体连接参数。
 *
 * <p>如果模型名未配置，抛 {@link UnknownModelException}（最终映射为 400）；
 * 如果引用的 provider 不存在，抛 {@link IllegalStateException}（配置错误）。
 */
@Component
public class ModelRouter {

    /** 全局配置对象，提供 routes 与 providers 配置。 */
    private final GatewayProperties properties;

    /** 构造路由解析器。 */
    public ModelRouter(GatewayProperties properties) {
        this.properties = properties;
    }

    /** 根据请求模型名解析路由，模型名或 provider 不存在时抛异常。 */
    public ModelRoute route(String requestedModel) {
        GatewayProperties.RouteConfig routeConfig = properties.getRoutes().get(requestedModel);
        if (routeConfig == null) {
            throw new UnknownModelException(requestedModel);
        }

        GatewayProperties.ProviderConfig provider = properties.getProviders().get(routeConfig.getProvider());
        if (provider == null) {
            throw new IllegalStateException("Provider not configured: " + routeConfig.getProvider());
        }

        // fallback 是可选配置，仅在显式声明时解析备用 provider。
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
