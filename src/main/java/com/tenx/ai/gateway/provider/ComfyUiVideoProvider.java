package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Submits text-to-video jobs directly to ComfyUI and returns a gateway download URL. */
@Component
public class ComfyUiVideoProvider implements VideoProvider {

    private final ProviderWebClientFactory clients;
    private final ComfyUiVideoWorkflowFactory workflows;
    private final GatewayProperties properties;
    private final ObjectMapper mapper;
    private final ComfyUiGenerationLimiter generationSlots;

    public ComfyUiVideoProvider(ProviderWebClientFactory clients, ComfyUiVideoWorkflowFactory workflows,
                                GatewayProperties properties, ObjectMapper mapper,
                                ComfyUiGenerationLimiter generationSlots) {
        this.clients = clients;
        this.workflows = workflows;
        this.properties = properties;
        this.mapper = mapper;
        this.generationSlots = generationSlots;
    }

    @Override
    public boolean supports(String providerType) {
        return "comfyui-video".equalsIgnoreCase(providerType);
    }

    @Override
    public Mono<JsonNode> generate(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        return Mono.defer(() -> {
            if (!generationSlots.tryAcquire()) {
                return Mono.error(new UpstreamProviderException(HttpStatus.TOO_MANY_REQUESTS,
                        "ComfyUI video generation is busy; retry later", MediaType.TEXT_PLAIN));
            }
            return Mono.defer(() -> generateOnce(request, provider)).doFinally(signal -> generationSlots.release());
        });
    }

    private Mono<JsonNode> generateOnce(VideoGenerationRequest request, GatewayProperties.ProviderConfig provider) {
        GatewayProperties.ComfyUiConfig config = properties.getComfyui();
        Path output = Paths.get(config.getResultsDirectory());
        if (!Files.isDirectory(output) || !Files.isReadable(output)) {
            return Mono.error(new UpstreamProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ComfyUI output disk is unavailable", MediaType.TEXT_PLAIN));
        }
        String runId = UUID.randomUUID().toString().replace("-", "");
        ObjectNode workflow = workflows.create(request, "tenx_" + runId);
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
                    return waitForResult(client, promptId, runId, request, config);
                })
                .onErrorMap(TimeoutException.class, error -> new UpstreamProviderException(
                        HttpStatus.GATEWAY_TIMEOUT, "ComfyUI video generation timed out", MediaType.TEXT_PLAIN))
                .onErrorMap(WebClientRequestException.class, error -> new UpstreamProviderException(
                        HttpStatus.BAD_GATEWAY, "Cannot reach ComfyUI", MediaType.TEXT_PLAIN));
    }

    private Mono<JsonNode> waitForResult(WebClient client, String promptId, String runId,
                                          VideoGenerationRequest request, GatewayProperties.ComfyUiConfig config) {
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
                                "ComfyUI video generation failed: " + entry.path("status"), MediaType.TEXT_PLAIN));
                    }
                    return entry.path("status").path("completed").asBoolean(false) ? Mono.just(entry) : Mono.empty();
                })
                .next()
                .timeout(Duration.ofMillis(config.getVideoGenerationTimeoutMillis()))
                .flatMap(entry -> Mono.fromCallable(() -> buildResponse(entry, runId, request, config))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private JsonNode buildResponse(JsonNode entry, String runId, VideoGenerationRequest request,
                                   GatewayProperties.ComfyUiConfig config) {
        JsonNode files = entry.path("outputs").path("11").path("images");
        if (!files.isArray() || files.size() != 1) {
            throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                    "ComfyUI completed without exactly one saved video", MediaType.TEXT_PLAIN);
        }
        JsonNode video = files.get(0);
        String filename = video.path("filename").asText("");
        if (!"output".equals(video.path("type").asText())
                || !"tenx-video".equals(video.path("subfolder").asText())
                || !filename.matches("^tenx_" + runId + "_[0-9]{5}_\\.mp4$")) {
            throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                    "ComfyUI returned an unexpected video filename", MediaType.TEXT_PLAIN);
        }
        Path file = Paths.get(config.getResultsDirectory()).resolve("tenx-video").resolve(filename);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file)) {
            throw new UpstreamProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Generated video is not available on the mounted output disk", MediaType.TEXT_PLAIN);
        }
        String resultId = filename.substring(0, filename.length() - 4);
        ObjectNode response = mapper.createObjectNode();
        response.put("created", Instant.now().getEpochSecond());
        response.put("model", request.getModel());
        response.put("duration", request.getDuration());
        response.put("size", request.getSize() == null
                ? ("HunyuanVideo-1.5".equals(request.getModel()) ? "1280x720" : "1280x704") : request.getSize());
        response.put("result_id", resultId);
        response.put("url", config.getPublicBaseUrl().replaceAll("/+$", "")
                + "/v1/videos/results/" + resultId + "/content");
        return response;
    }
}
