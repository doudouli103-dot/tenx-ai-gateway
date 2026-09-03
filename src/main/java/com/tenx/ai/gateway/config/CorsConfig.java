package com.tenx.ai.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * CORS 配置。仅为 admin 接口（{@code /admin/**}）开放跨域，来源列表来自配置，
 * 供 admin 前端（如 tenx-ai-gateway-admin）跨域调用。
 */
@Configuration
public class CorsConfig implements WebFluxConfigurer {

    /** 全局配置对象，提供 admin 允许的跨域来源。 */
    private final GatewayProperties properties;

    /** 构造 CORS 配置。 */
    public CorsConfig(GatewayProperties properties) {
        this.properties = properties;
    }

    /** 注册 admin 接口的 CORS 映射。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.getAdmin().getCorsAllowedOrigins();
        registry.addMapping("/admin/**")
                .allowedOrigins(origins.toArray(new String[origins.size()]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
