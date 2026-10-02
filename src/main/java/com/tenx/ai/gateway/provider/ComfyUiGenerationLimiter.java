package com.tenx.ai.gateway.provider;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

/** Shared concurrency limit across image and video jobs on the same ComfyUI runtime. */
@Component
public class ComfyUiGenerationLimiter {
    private final Semaphore slots;

    public ComfyUiGenerationLimiter(GatewayProperties properties) {
        slots = new Semaphore(Math.max(1, properties.getComfyui().getMaxConcurrentGenerations()));
    }

    public boolean tryAcquire() { return slots.tryAcquire(); }
    public void release() { slots.release(); }
}
