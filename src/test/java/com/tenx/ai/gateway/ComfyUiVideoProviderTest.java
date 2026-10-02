package com.tenx.ai.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.config.WebClientConfig;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import com.tenx.ai.gateway.provider.ComfyUiVideoProvider;
import com.tenx.ai.gateway.provider.ComfyUiGenerationLimiter;
import com.tenx.ai.gateway.provider.ComfyUiVideoWorkflowFactory;
import com.tenx.ai.gateway.provider.ProviderWebClientFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.netty.resources.ConnectionProvider;

public class ComfyUiVideoProviderTest {

    @TempDir Path output;
    private HttpServer server;
    private ConnectionProvider pool;

    @AfterEach
    public void stop() {
        if (server != null) server.stop(0);
        if (pool != null) pool.dispose();
    }

    @Test
    public void returnsGatewayUrlForSavedVideo() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<JsonNode> workflow = new AtomicReference<JsonNode>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/prompt", exchange -> {
            workflow.set(mapper.readTree(exchange.getRequestBody()).path("prompt"));
            respond(exchange, "{\"prompt_id\":\"video-prompt\"}");
        });
        server.createContext("/history/video-prompt", exchange -> {
            String prefix = workflow.get().path("11").path("inputs").path("filename_prefix").asText();
            String filename = prefix.substring("tenx-video/".length()) + "_00001_.mp4";
            Path file = output.resolve("tenx-video").resolve(filename);
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[] {0, 0, 0, 1});
            respond(exchange, "{\"video-prompt\":{\"status\":{\"completed\":true,\"status_str\":\"success\"},"
                    + "\"outputs\":{\"11\":{\"images\":[{\"filename\":\"" + filename
                    + "\",\"subfolder\":\"tenx-video\",\"type\":\"output\"}]}}}}");
        });
        server.start();

        GatewayProperties properties = new GatewayProperties();
        properties.getComfyui().setResultsDirectory(output.toString());
        properties.getComfyui().setPublicBaseUrl("http://example.test:8088/");
        GatewayProperties.ProviderConfig upstream = new GatewayProperties.ProviderConfig();
        upstream.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        WebClientConfig config = new WebClientConfig();
        pool = config.connectionProvider(properties);
        ComfyUiVideoProvider provider = new ComfyUiVideoProvider(
                new ProviderWebClientFactory(properties, pool, config.exchangeStrategies(properties)),
                new ComfyUiVideoWorkflowFactory(mapper), properties, mapper,
                new ComfyUiGenerationLimiter(properties));
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setModel("Wan2.2-TI2V-5B");
        request.setPrompt("robot walking");
        request.setDuration(5);
        JsonNode response = provider.generate(request, upstream).block();

        Assertions.assertEquals("robot walking", workflow.get().path("4").path("inputs").path("text").asText());
        Assertions.assertEquals(704, workflow.get().path("6").path("inputs").path("height").asInt());
        Assertions.assertEquals(121, workflow.get().path("6").path("inputs").path("length").asInt());
        String resultId = response.path("result_id").asText();
        Assertions.assertTrue(Files.exists(output.resolve("tenx-video").resolve(resultId + ".mp4")));
        Assertions.assertEquals("http://example.test:8088/v1/videos/results/" + resultId + "/content",
                response.path("url").asText());
        Assertions.assertEquals("1280x704", response.path("size").asText());
        Assertions.assertFalse(response.toString().contains("b64_json"));
    }

    @Test
    public void buildsHunyuanWorkflowAndRejectsInvalidSizes() {
        ComfyUiVideoWorkflowFactory factory = new ComfyUiVideoWorkflowFactory(new ObjectMapper());
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setModel("HunyuanVideo-1.5");
        request.setPrompt("robot walking");
        request.setDuration(5);
        JsonNode workflow = factory.create(request, "tenx_test");
        Assertions.assertEquals("hunyuan_video_15", workflow.path("1").path("inputs").path("type").asText());
        Assertions.assertEquals(720, workflow.path("6").path("inputs").path("height").asInt());
        request.setModel("Wan2.2-TI2V-5B");
        request.setSize("1280x720");
        Assertions.assertThrows(IllegalArgumentException.class, () -> factory.create(request, "tenx_test"));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws java.io.IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
