package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 视频 provider 注册表：注入所有 {@link VideoProvider} 实现，按 providerType 匹配并返回对应实现。
 */
@Component
public class VideoProviderRegistry {

    /** 所有已注入的视频 provider 实现。 */
    private final List<VideoProvider> providers;

    /** 构造注册表。 */
    public VideoProviderRegistry(List<VideoProvider> providers) {
        this.providers = providers;
    }

    /** 按 providerType 返回匹配的 provider，找不到则抛异常。 */
    public VideoProvider get(String providerType) {
        for (VideoProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No video provider supports type: " + providerType);
    }
}
