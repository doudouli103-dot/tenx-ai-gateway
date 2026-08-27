package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.model.VideoGenerationRequest;
import com.tenx.ai.gateway.model.VideoTaskResponse;
import com.tenx.ai.gateway.video.VideoTaskService;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class VideoController {

    private final VideoTaskService videoTaskService;

    public VideoController(VideoTaskService videoTaskService) {
        this.videoTaskService = videoTaskService;
    }

    @PostMapping("/v1/videos/generations")
    public Mono<ResponseEntity<VideoTaskResponse>> generate(@Valid @RequestBody VideoGenerationRequest request) {
        return Mono.just(ResponseEntity.accepted().body(videoTaskService.submit(request)));
    }

    @GetMapping("/v1/videos/tasks/{taskId}")
    public Mono<ResponseEntity<VideoTaskResponse>> task(@PathVariable String taskId) {
        VideoTaskResponse response = videoTaskService.get(taskId);
        if (response == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok(response));
    }
}
