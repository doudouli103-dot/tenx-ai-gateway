package com.tenx.ai.gateway.video;

import com.tenx.ai.gateway.document.DocumentCenterClient;
import com.tenx.ai.gateway.document.DocumentUploadResult;
import com.tenx.ai.gateway.model.GeneratedAsset;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import com.tenx.ai.gateway.model.VideoTaskResponse;
import com.tenx.ai.gateway.provider.VideoProvider;
import com.tenx.ai.gateway.provider.VideoProviderRegistry;
import com.tenx.ai.gateway.routing.ModelRoute;
import com.tenx.ai.gateway.routing.ModelRouter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class VideoTaskService {

    private final ModelRouter modelRouter;
    private final VideoProviderRegistry videoProviderRegistry;
    private final DocumentCenterClient documentCenterClient;
    private final Map<String, VideoTask> tasks = new ConcurrentHashMap<String, VideoTask>();

    public VideoTaskService(ModelRouter modelRouter,
                            VideoProviderRegistry videoProviderRegistry,
                            DocumentCenterClient documentCenterClient) {
        this.modelRouter = modelRouter;
        this.videoProviderRegistry = videoProviderRegistry;
        this.documentCenterClient = documentCenterClient;
    }

    public VideoTaskResponse submit(VideoGenerationRequest request) {
        ModelRoute route = modelRouter.route(request.getModel());
        if (!route.isCapability("video")) {
            throw new IllegalArgumentException("Model is not configured for video generation: " + request.getModel());
        }

        Integer duration = resolveDuration(request, route);
        String taskId = "video-task-" + UUID.randomUUID().toString();
        VideoTask task = new VideoTask(taskId);
        tasks.put(taskId, task);

        VideoProvider provider = videoProviderRegistry.get(route.getProvider().getType());
        VideoGenerationRequest routedRequest = request.copyForModel(route.getModel(), duration);

        task.setStatus("running");
        provider.generate(routedRequest, route.getProvider())
                .flatMap(asset -> upload(request, asset))
                .subscribe(
                        uploadResult -> markSucceeded(task, uploadResult),
                        error -> markFailed(task, error)
                );

        return toResponse(task);
    }

    public VideoTaskResponse get(String taskId) {
        VideoTask task = tasks.get(taskId);
        if (task == null) {
            return null;
        }
        return toResponse(task);
    }

    private Integer resolveDuration(VideoGenerationRequest request, ModelRoute route) {
        Integer duration = request.getDuration();
        if (duration == null) {
            duration = route.getDefaultDurationSeconds();
        }
        if (duration == null) {
            duration = Integer.valueOf(5);
        }

        Integer maxDuration = route.getMaxDurationSeconds();
        if (maxDuration != null && duration.intValue() > maxDuration.intValue()) {
            throw new IllegalArgumentException("Model " + request.getModel() + " supports max duration " + maxDuration + " seconds");
        }
        return duration;
    }

    private reactor.core.publisher.Mono<DocumentUploadResult> upload(VideoGenerationRequest request, GeneratedAsset asset) {
        return documentCenterClient.upload("video_generation", request.getModel(), asset);
    }

    private void markSucceeded(VideoTask task, DocumentUploadResult uploadResult) {
        task.setFileId(uploadResult.getFileId());
        task.setResultUrl(uploadResult.getUrl());
        task.setStatus("succeeded");
    }

    private void markFailed(VideoTask task, Throwable error) {
        task.setError(error.getMessage());
        task.setStatus("failed");
    }

    private VideoTaskResponse toResponse(VideoTask task) {
        VideoTaskResponse response = new VideoTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setStatus(task.getStatus());
        response.setFileId(task.getFileId());
        response.setResultUrl(task.getResultUrl());
        response.setError(task.getError());
        return response;
    }
}
