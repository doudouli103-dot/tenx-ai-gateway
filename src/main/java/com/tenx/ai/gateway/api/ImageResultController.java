package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Authenticated, streaming access to images saved permanently by ComfyUI. */
@RestController
public class ImageResultController {

    private static final Pattern RESULT_ID = Pattern.compile("tenx_[0-9a-f]{32}_[0-9]{5}_");
    private final GatewayProperties properties;

    public ImageResultController(GatewayProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/v1/images/results/{resultId}/content")
    public Mono<ResponseEntity<Resource>> download(@PathVariable String resultId) {
        if (!RESULT_ID.matcher(resultId).matches()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return Mono.<ResponseEntity<Resource>>fromCallable(() -> {
            Path root = Paths.get(properties.getComfyui().getResultsDirectory());
            if (!Files.isDirectory(root) || !Files.isReadable(root)) {
                return ResponseEntity.<Resource>status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            Path file = root.resolve("tenx").resolve(resultId + ".png");
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file)) {
                return ResponseEntity.<Resource>notFound().build();
            }
            ResponseEntity<Resource> response = ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(Files.size(file))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultId + ".png\"")
                    .body((Resource) new FileSystemResource(file));
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
