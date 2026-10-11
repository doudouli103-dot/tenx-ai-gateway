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

    @Test
    public void requiresApiKeyAndStreamsVideoFrame() throws Exception {
        String id = "tenx_0123456789abcdef0123456789abcdef";
        String filename = "frame_00001_.png";
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        Path directory = output.resolve("tenx-video-frames").resolve(id);
        Files.createDirectories(directory);
        Files.write(directory.resolve(filename), png);
        GatewayProperties properties = properties();
        WebTestClient client = WebTestClient.bindToController(new VideoResultController(properties))
                .webFilter(new ApiKeyWebFilter(properties)).build();

        String uri = "/v1/videos/results/" + id + "/frames/" + filename;
        client.get().uri(uri).exchange().expectStatus().isUnauthorized();
        client.get().uri(uri).header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isOk().expectHeader().contentType("image/png")
                .expectHeader().contentLength(png.length)
                .expectBody().consumeWith(result -> Assertions.assertArrayEquals(png, result.getResponseBody()));
        client.get().uri("/v1/videos/results/not-a-result/frames/" + filename)
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isBadRequest();
        client.get().uri("/v1/videos/results/" + id + "/frames/not-a-frame.jpg")
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isBadRequest();
        client.get().uri("/v1/videos/results/" + id + "/frames/frame_00002_.png")
                .header("Authorization", "Bearer test-key")
                .exchange().expectStatus().isNotFound();
    }

    private GatewayProperties properties() {
        GatewayProperties properties = new GatewayProperties();
        properties.setApiKeys(Collections.singletonList("test-key"));
        properties.getComfyui().setResultsDirectory(output.toString());
        return properties;
    }
}
