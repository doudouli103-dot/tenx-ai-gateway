package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GatewayPropertiesBindingTest {

    @Autowired
    private GatewayProperties properties;

    @Test
    public void bindsModelNamesThatContainDots() {
        Assertions.assertTrue(properties.getRoutes().containsKey("HunyuanVideo-1.5"));
        Assertions.assertEquals("video", properties.getRoutes().get("HunyuanVideo-1.5").getCapability());
        Assertions.assertEquals(Integer.valueOf(5), properties.getRoutes().get("HunyuanVideo-1.5").getMaxDurationSeconds());

        Assertions.assertTrue(properties.getRoutes().containsKey("Wan2.2-TI2V-5B"));
        Assertions.assertEquals("video", properties.getRoutes().get("Wan2.2-TI2V-5B").getCapability());
        Assertions.assertEquals(Integer.valueOf(5), properties.getRoutes().get("Wan2.2-TI2V-5B").getDefaultDurationSeconds());
    }
}
