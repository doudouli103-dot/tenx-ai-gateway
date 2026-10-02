package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Submits a fixed image workflow to ComfyUI and exposes its saved output through the gateway. */
@Component
public class ComfyUiImageProvider implements ImageProvider {

    private final ProviderWebClientFactory clients;
    private final ComfyUiWorkflowFactory workflows;
    private final GatewayProperties properties;
    private final ObjectMapper mapper;
    private final Semaphore generationSlots;

    public ComfyUiImageProvider(ProviderWebClientFactory clients, ComfyUiWorkflowFactory workflows,
                                GatewayProperties properties, ObjectMapper mapper) {
        this.clients = clients;
        this.workflows = workflows;
        this.properties = properties;
        this.mapper = mapper;
        this.generationSlots = new Semaphore(Math.max(1, properties.getComfyui().getMaxConcurrentGenerations()));
    }

    @Override
    public boolean supports(String providerType) {
        return "comfyui".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<JsonNode> generate(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return Mono.defer(() -> {
            if (!generationSlots.tryAcquire()) {
                return Mono.error(new UpstreamProviderException(HttpStatus.TOO_MANY_REQUESTS,
                        "ComfyUI image generation is busy; retry later", MediaType.TEXT_PLAIN));
            }
            return Mono.defer(() -> generateOnce(request, provider))
                    .doFinally(signal -> generationSlots.release());
        });
    }

    private Mono<JsonNode> generateOnce(ImageGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        GatewayProperties.ComfyUiConfig config = properties.getComfyui();
        Path output = Paths.get(config.getResultsDirectory());
        if (!Files.isDirectory(output) || !Files.isReadable(output)) {
            return Mono.error(new UpstreamProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ComfyUI output disk is unavailable", MediaType.TEXT_PLAIN));
        }

        String runId = UUID.randomUUID().toString().replace("-", "");
        String resultPrefix = "tenx_" + runId;
        ObjectNode workflow = workflows.create(request, resultPrefix);
        ObjectNode body = mapper.createObjectNode();
        body.set("prompt", workflow);
        WebClient client = clients.client(provider);

        return client.post().uri("/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatus::isError, UpstreamProviderException::fromResponse)
                .bodyToMono(JsonNode.class)
                .flatMap(submitted -> {
                    String promptId = submitted.path("prompt_id").asText(null);
                    if (promptId == null || promptId.isEmpty()) {
                        return Mono.error(new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                                "ComfyUI did not return prompt_id: " + submitted, MediaType.TEXT_PLAIN));
                    }
                    return waitForResult(client, promptId, runId, config);
                })
                .onErrorMap(TimeoutException.class, error -> new UpstreamProviderException(
                        HttpStatus.GATEWAY_TIMEOUT, "ComfyUI generation timed out", MediaType.TEXT_PLAIN))
                .onErrorMap(WebClientRequestException.class, error -> new UpstreamProviderException(
                        HttpStatus.BAD_GATEWAY, "Cannot reach ComfyUI", MediaType.TEXT_PLAIN));
    }

    private Mono<JsonNode> waitForResult(WebClient client, String promptId, String runId,
                                          GatewayProperties.ComfyUiConfig config) {
        long interval = Math.max(250, config.getPollIntervalMillis());
        return Flux.interval(Duration.ZERO, Duration.ofMillis(interval))
                .concatMap(tick -> client.get().uri("/history/{id}", promptId)
                        .retrieve()
                        .onStatus(HttpStatus::isError, UpstreamProviderException::fromResponse)
                        .bodyToMono(JsonNode.class))
                .map(history -> history.path(promptId))
                .filter(entry -> !entry.isMissingNode())
                .flatMap(entry -> {
                    String status = entry.path("status").path("status_str").asText("");
                    if ("error".equals(status)) {
                        return Mono.error(new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                                "ComfyUI generation failed: " + entry.path("status"), MediaType.TEXT_PLAIN));
                    }
                    if (!entry.path("status").path("completed").asBoolean(false)) {
                        return Mono.empty();
                    }
                    return Mono.just(entry);
                })
                .next()
                .timeout(Duration.ofMillis(config.getGenerationTimeoutMillis()))
                .flatMap(entry -> Mono.fromCallable(() -> buildResponse(entry, runId, config))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private JsonNode buildResponse(JsonNode entry, String runId, GatewayProperties.ComfyUiConfig config) {
        JsonNode images = entry.path("outputs").path("10").path("images");
        if (!images.isArray() || images.size() == 0) {
            throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                    "ComfyUI completed without saved images", MediaType.TEXT_PLAIN);
        }
        ObjectNode response = mapper.createObjectNode();
        response.put("created", Instant.now().getEpochSecond());
        ArrayNode data = response.putArray("data");
        Path directory = Paths.get(config.getResultsDirectory()).resolve("tenx");
        String expectedPrefix = "tenx_" + runId + "_";
        String baseUrl = config.getPublicBaseUrl().replaceAll("/+$", "");
        for (JsonNode image : images) {
            String filename = image.path("filename").asText("");
            if (!"output".equals(image.path("type").asText())
                    || !"tenx".equals(image.path("subfolder").asText())
                    || !filename.matches("^" + expectedPrefix + "[0-9]{5}_\\.png$")) {
                throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                        "ComfyUI returned an unexpected output filename", MediaType.TEXT_PLAIN);
            }
            Path file = directory.resolve(filename);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file)) {
                throw new UpstreamProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Generated image is not available on the mounted output disk", MediaType.TEXT_PLAIN);
            }
            String resultId = filename.substring(0, filename.length() - 4);
            ObjectNode item = data.addObject();
            item.put("result_id", resultId);
            item.put("url", baseUrl + "/v1/images/results/" + resultId + "/content");
        }
        return response;
    }
}
