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
        Assertions.assertTrue(properties.getRoutes().containsKey("wan2.2-ti2v-5b"));
        Assertions.assertEquals("video", properties.getRoutes().get("wan2.2-ti2v-5b").getCapability());
        Assertions.assertEquals(Integer.valueOf(5), properties.getRoutes().get("wan2.2-ti2v-5b").getMaxDurationSeconds());

        Assertions.assertTrue(properties.getRoutes().containsKey("wan2.2-i2v-a14b"));
        Assertions.assertEquals("video", properties.getRoutes().get("wan2.2-i2v-a14b").getCapability());
        Assertions.assertEquals(Integer.valueOf(5), properties.getRoutes().get("wan2.2-i2v-a14b").getDefaultDurationSeconds());
    }
}
