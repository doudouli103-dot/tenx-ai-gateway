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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

/** Submits text-to-frame jobs to ComfyUI for final video encoding on Windows. */
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
        int expectedFrameCount = request.getDuration() * 24 + 1;
        if (!files.isArray() || files.size() != expectedFrameCount) {
            throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                    "ComfyUI completed without the expected number of video frames", MediaType.TEXT_PLAIN);
        }
        String resultId = "tenx_" + runId;
        String subfolder = "tenx-video-frames/" + resultId;
        Path root = Paths.get(config.getResultsDirectory()).toAbsolutePath().normalize();
        Path frameDirectory = root.resolve(subfolder).normalize();
        if (!frameDirectory.startsWith(root)) {
            throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                    "ComfyUI returned an invalid frame directory", MediaType.TEXT_PLAIN);
        }

        List<String> frameNames = new ArrayList<String>();
        Set<String> uniqueNames = new HashSet<String>();
        for (JsonNode frame : files) {
            String filename = frame.path("filename").asText("");
            if (!"output".equals(frame.path("type").asText())
                    || !subfolder.equals(frame.path("subfolder").asText())
                    || !filename.matches("^frame_[0-9]{5}_\\.png$")
                    || !uniqueNames.add(filename)) {
                throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                        "ComfyUI returned an unexpected video frame", MediaType.TEXT_PLAIN);
            }
            Path file = frameDirectory.resolve(filename).normalize();
            if (!file.startsWith(frameDirectory)
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isReadable(file)) {
                throw new UpstreamProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Generated video frame is not available on the mounted output disk", MediaType.TEXT_PLAIN);
            }
            frameNames.add(filename);
        }
        Collections.sort(frameNames);
        for (int index = 0; index < frameNames.size(); index++) {
            String expectedName = String.format(Locale.ROOT, "frame_%05d_.png", index + 1);
            if (!expectedName.equals(frameNames.get(index))) {
                throw new UpstreamProviderException(HttpStatus.BAD_GATEWAY,
                        "ComfyUI returned a non-contiguous video frame sequence", MediaType.TEXT_PLAIN);
            }
        }

        ObjectNode response = mapper.createObjectNode();
        response.put("created", Instant.now().getEpochSecond());
        response.put("model", request.getModel());
        response.put("duration", request.getDuration());
        response.put("size", request.getSize() == null ? "640x352" : request.getSize());
        response.put("result_id", resultId);
        response.put("status", "frames_ready");
        response.put("output_type", "image_sequence");
        response.put("fps", 24);
        response.put("frame_count", frameNames.size());
        response.put("frame_pattern", "frame_%05d_.png");
        response.put("frames_relative_path", subfolder);
        response.put("frames_base_url", config.getPublicBaseUrl().replaceAll("/+$", "")
                + "/v1/videos/results/" + resultId + "/frames");
        response.set("frame_files", mapper.valueToTree(frameNames));
        return response;
    }
}
