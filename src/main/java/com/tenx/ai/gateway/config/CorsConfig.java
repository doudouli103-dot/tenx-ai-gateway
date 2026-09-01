package com.tenx.ai.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfig implements WebFluxConfigurer {

    private final GatewayProperties properties;

    public CorsConfig(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.getAdmin().getCorsAllowedOrigins();
        registry.addMapping("/admin/**")
                .allowedOrigins(origins.toArray(new String[origins.size()]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
