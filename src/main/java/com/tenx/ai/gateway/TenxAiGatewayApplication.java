package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class TenxAiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenxAiGatewayApplication.class, args);
    }
}
