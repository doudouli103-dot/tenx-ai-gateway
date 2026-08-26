package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ModelProviderRegistry {

    private final List<ModelProvider> providers;

    public ModelProviderRegistry(List<ModelProvider> providers) {
        this.providers = providers;
    }

    public ModelProvider get(String providerType) {
        for (ModelProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No provider supports type: " + providerType);
    }
}
