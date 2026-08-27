package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ImageProviderRegistry {

    private final List<ImageProvider> providers;

    public ImageProviderRegistry(List<ImageProvider> providers) {
        this.providers = providers;
    }

    public ImageProvider get(String providerType) {
        for (ImageProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No image provider supports type: " + providerType);
    }
}
