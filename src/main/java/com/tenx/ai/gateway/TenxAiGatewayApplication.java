package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 应用入口。
 *
 * <p>{@code @EnableConfigurationProperties} 激活 {@link GatewayProperties} 的配置绑定，
 * 使 application.yml 中 {@code tenx.ai.gateway} 前缀的配置能注入到各组件。
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class TenxAiGatewayApplication {

    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(TenxAiGatewayApplication.class, args);
    }
}
