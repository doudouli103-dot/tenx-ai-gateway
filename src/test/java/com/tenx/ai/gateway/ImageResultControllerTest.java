package com.tenx.ai.gateway;

import com.tenx.ai.gateway.api.ImageResultController;
import com.tenx.ai.gateway.auth.ApiKeyWebFilter;
import com.tenx.ai.gateway.config.GatewayProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.reactive.server.WebTestClient;

public class ImageResultControllerTest {

    @TempDir
    Path output;

    @Test
    public void requiresApiKeyAndStreamsSavedImage() throws Exception {
        String id = "tenx_0123456789abcdef0123456789abcdef_00001_";
        Files.createDirectories(output.resolve("tenx"));
        Files.write(output.resolve("tenx").resolve(id + ".png"), new byte[] {1, 2, 3});
        GatewayProperties properties = new GatewayProperties();
        properties.setApiKeys(Collections.singletonList("test-key"));
        properties.getComfyui().setResultsDirectory(output.toString());
        WebTestClient client = WebTestClient.bindToController(new ImageResultController(properties))
                .webFilter(new ApiKeyWebFilter(properties)).build();

        client.get().uri("/v1/images/results/" + id + "/content")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/v1/images/results/" + id + "/content")
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isOk()
                .expectHeader().contentType("image/png")
                .expectBody().consumeWith(result -> Assertions.assertArrayEquals(
                        new byte[] {1, 2, 3}, result.getResponseBody()));
        client.get().uri("/v1/images/results/not-a-result/content")
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isBadRequest();
    }
}
