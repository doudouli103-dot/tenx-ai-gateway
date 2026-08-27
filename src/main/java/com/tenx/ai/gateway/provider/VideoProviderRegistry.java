package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VideoProviderRegistry {

    private final List<VideoProvider> providers;

    public VideoProviderRegistry(List<VideoProvider> providers) {
        this.providers = providers;
    }

    public VideoProvider get(String providerType) {
        for (VideoProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No video provider supports type: " + providerType);
    }
}
