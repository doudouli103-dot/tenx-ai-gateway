package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpeechProviderRegistry {

    private final List<SpeechProvider> providers;

    public SpeechProviderRegistry(List<SpeechProvider> providers) {
        this.providers = providers;
    }

    public SpeechProvider get(String providerType) {
        for (SpeechProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No speech provider supports type: " + providerType);
    }
}
