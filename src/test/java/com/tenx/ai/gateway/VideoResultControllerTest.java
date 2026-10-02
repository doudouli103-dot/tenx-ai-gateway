package com.tenx.ai.gateway;

import com.tenx.ai.gateway.api.VideoResultController;
import com.tenx.ai.gateway.auth.ApiKeyWebFilter;
import com.tenx.ai.gateway.config.GatewayProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.reactive.server.WebTestClient;

public class VideoResultControllerTest {

    @TempDir Path output;

    @Test
    public void requiresApiKeyAndStreamsMp4() throws Exception {
        String id = "tenx_0123456789abcdef0123456789abcdef_00001_";
        Files.createDirectories(output.resolve("tenx-video"));
        Files.write(output.resolve("tenx-video").resolve(id + ".mp4"), new byte[] {0, 0, 0, 1});
        GatewayProperties properties = new GatewayProperties();
        properties.setApiKeys(Collections.singletonList("test-key"));
        properties.getComfyui().setResultsDirectory(output.toString());
        WebTestClient client = WebTestClient.bindToController(new VideoResultController(properties))
                .webFilter(new ApiKeyWebFilter(properties)).build();
        client.get().uri("/v1/videos/results/" + id + "/content")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/v1/videos/results/" + id + "/content")
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isOk().expectHeader().contentType("video/mp4")
                .expectBody().consumeWith(result -> Assertions.assertArrayEquals(
                        new byte[] {0, 0, 0, 1}, result.getResponseBody()));
        client.get().uri("/v1/videos/results/not-a-result/content")
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isBadRequest();
    }
}
