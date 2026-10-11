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
import java.util.concurrent.atomic.AtomicInteger;
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
    public void returnsFrameManifestForSavedFrames() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<JsonNode> workflow = new AtomicReference<JsonNode>();
        AtomicInteger uploads = new AtomicInteger();
        String referenceId = "tenx_0123456789abcdef0123456789abcdef_00001_";
        Path imageDirectory = output.resolve("tenx");
        Files.createDirectories(imageDirectory);
        Files.write(imageDirectory.resolve(referenceId + ".png"), new byte[] {1, 2, 3});
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/upload/image", exchange -> {
            uploads.incrementAndGet();
            while (exchange.getRequestBody().read() != -1) { }
            respond(exchange, "{\"name\":\"" + referenceId
                    + ".png\",\"subfolder\":\"tenx-reference\",\"type\":\"input\"}");
        });
        server.createContext("/prompt", exchange -> {
            workflow.set(mapper.readTree(exchange.getRequestBody()).path("prompt"));
            respond(exchange, "{\"prompt_id\":\"video-prompt\"}");
        });
        server.createContext("/history/video-prompt", exchange -> {
            String prefix = workflow.get().path("11").path("inputs").path("filename_prefix").asText();
            String subfolder = prefix.substring(0, prefix.lastIndexOf('/'));
            Path directory = output.resolve(subfolder);
            Files.createDirectories(directory);
            int frameCount = workflow.get().path("6").path("inputs").path("length").asInt();
            StringBuilder frames = new StringBuilder();
            for (int index = frameCount; index >= 1; index--) {
                String filename = String.format("frame_%05d_.png", index);
                Files.write(directory.resolve(filename), new byte[] {(byte) index});
                if (frames.length() > 0) frames.append(',');
                frames.append(frameJson(filename, subfolder));
            }
            respond(exchange, "{\"video-prompt\":{\"status\":{\"completed\":true,\"status_str\":\"success\"},"
                    + "\"outputs\":{\"11\":{\"images\":[" + frames + "]}}}}");
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
        request.setDuration(1);
        request.setProjectId("drama-01");
        request.setSceneId("scene-01");
        request.setShotId("shot-001");
        request.setStartImageResultId(referenceId);
        JsonNode response = provider.generate(request, upstream).block();

        Assertions.assertEquals("robot walking", workflow.get().path("4").path("inputs").path("text").asText());
        Assertions.assertEquals("SaveImage", workflow.get().path("11").path("class_type").asText());
        Assertions.assertTrue(workflow.get().path("10").isMissingNode());
        Assertions.assertEquals("umt5_xxl_fp16.safetensors",
                workflow.get().path("1").path("inputs").path("clip_name").asText());
        Assertions.assertEquals(640, workflow.get().path("6").path("inputs").path("width").asInt());
        Assertions.assertEquals(352, workflow.get().path("6").path("inputs").path("height").asInt());
        Assertions.assertEquals(25, workflow.get().path("6").path("inputs").path("length").asInt());
        Assertions.assertEquals(14, workflow.get().path("8").path("inputs").path("steps").asInt());
        Assertions.assertEquals(1, uploads.get());
        Assertions.assertEquals("LoadImage", workflow.get().path("12").path("class_type").asText());
        Assertions.assertEquals("tenx-reference/" + referenceId + ".png",
                workflow.get().path("12").path("inputs").path("image").asText());
        Assertions.assertEquals("12", workflow.get().path("6").path("inputs").path("start_image").get(0).asText());
        Assertions.assertEquals("euler", workflow.get().path("8").path("inputs").path("sampler_name").asText());
        String resultId = response.path("result_id").asText();
        Assertions.assertTrue(Files.isDirectory(output.resolve("tenx-video-frames").resolve(resultId)));
        Assertions.assertEquals("frames_ready", response.path("status").asText());
        Assertions.assertEquals("image_sequence", response.path("output_type").asText());
        Assertions.assertEquals(25, response.path("frame_count").asInt());
        Assertions.assertEquals("frame_%05d_.png", response.path("frame_pattern").asText());
        Assertions.assertEquals("tenx-video-frames/" + resultId,
                response.path("frames_relative_path").asText());
        Assertions.assertEquals("http://example.test:8088/v1/videos/results/" + resultId + "/frames",
                response.path("frames_base_url").asText());
        Assertions.assertEquals("frame_00001_.png", response.path("frame_files").get(0).asText());
        Assertions.assertEquals("640x352", response.path("size").asText());
        Assertions.assertEquals("drama-01", response.path("project_id").asText());
        Assertions.assertEquals("scene-01", response.path("scene_id").asText());
        Assertions.assertEquals("shot-001", response.path("shot_id").asText());
        Assertions.assertEquals("start_frame", response.path("reference_mode").asText());
        Assertions.assertEquals("short-drama-v1", response.path("workflow_version").asText());
        Assertions.assertEquals(14, response.path("steps").asInt());
        Assertions.assertFalse(response.has("url"));
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
        Assertions.assertEquals(352, workflow.path("6").path("inputs").path("height").asInt());
        Assertions.assertEquals("SaveImage", workflow.path("11").path("class_type").asText());
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

    private static String frameJson(String filename, String subfolder) {
        return "{\"filename\":\"" + filename + "\",\"subfolder\":\"" + subfolder
                + "\",\"type\":\"output\"}";
    }
}
