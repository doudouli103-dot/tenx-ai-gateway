package com.tenx.ai.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.config.WebClientConfig;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import com.tenx.ai.gateway.provider.ComfyUiImageProvider;
import com.tenx.ai.gateway.provider.ComfyUiWorkflowFactory;
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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.netty.resources.ConnectionProvider;

public class ComfyUiImageProviderTest {

    @TempDir
    Path output;

    private HttpServer server;
    private ConnectionProvider pool;

    @AfterEach
    public void stop() {
        if (server != null) server.stop(0);
        if (pool != null) pool.dispose();
    }

    @Test
    public void returnsAuthenticatedGatewayUrlForSavedImage() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<JsonNode> submittedWorkflow = new AtomicReference<JsonNode>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/prompt", exchange -> {
            submittedWorkflow.set(mapper.readTree(exchange.getRequestBody()).path("prompt"));
            byte[] body = "{\"prompt_id\":\"test-prompt\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/history/test-prompt", exchange -> {
            String prefix = submittedWorkflow.get().path("10").path("inputs").path("filename_prefix").asText();
            String filename = prefix.substring("tenx/".length()) + "_00001_.png";
            Path file = output.resolve("tenx").resolve(filename);
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[] {1, 2, 3});
            byte[] body = ("{\"test-prompt\":{\"status\":{\"completed\":true,\"status_str\":\"success\"},"
                    + "\"outputs\":{\"10\":{\"images\":[{\"filename\":\"" + filename
                    + "\",\"subfolder\":\"tenx\",\"type\":\"output\"}]}}}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        GatewayProperties properties = new GatewayProperties();
        properties.getComfyui().setResultsDirectory(output.toString());
        properties.getComfyui().setPublicBaseUrl("http://example.test:8088/");
        properties.getComfyui().setPollIntervalMillis(250);
        GatewayProperties.ProviderConfig providerConfig = new GatewayProperties.ProviderConfig();
        providerConfig.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        WebClientConfig config = new WebClientConfig();
        pool = config.connectionProvider(properties);
        ExchangeStrategies strategies = config.exchangeStrategies(properties);
        ProviderWebClientFactory clients = new ProviderWebClientFactory(properties, pool, strategies);
        ComfyUiImageProvider provider = new ComfyUiImageProvider(
                clients, new ComfyUiWorkflowFactory(mapper), properties, mapper);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setModel("qwen-image");
        request.setPrompt("orange cat");
        request.setSize("512x512");
        JsonNode response = provider.generate(request, providerConfig).block();

        Assertions.assertEquals("orange cat", submittedWorkflow.get().path("4").path("inputs").path("text").asText());
        Assertions.assertEquals(512, submittedWorkflow.get().path("6").path("inputs").path("width").asInt());
        String resultId = response.path("data").get(0).path("result_id").asText();
        Assertions.assertTrue(Files.exists(output.resolve("tenx").resolve(resultId + ".png")));
        Assertions.assertEquals("http://example.test:8088/v1/images/results/" + resultId + "/content",
                response.path("data").get(0).path("url").asText());
        Assertions.assertFalse(response.toString().contains("b64_json"));
    }

    @Test
    public void rejectsBase64AndUnsupportedImageSize() {
        ComfyUiWorkflowFactory factory = new ComfyUiWorkflowFactory(new ObjectMapper());
        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setModel("qwen-image");
        request.setPrompt("test");
        request.setExtra("response_format", new ObjectMapper().getNodeFactory().textNode("b64_json"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> factory.create(request, "tenx_test"));
        request.getExtra().clear();
        request.setSize("8192x8192");
        Assertions.assertThrows(IllegalArgumentException.class, () -> factory.create(request, "tenx_test"));
    }
}
