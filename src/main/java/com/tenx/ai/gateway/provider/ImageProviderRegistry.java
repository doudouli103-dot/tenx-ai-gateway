package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 图像 provider 注册表：注入所有 {@link ImageProvider} 实现，按 providerType 匹配并返回对应实现。
 */
@Component
public class ImageProviderRegistry {

    /** 所有已注入的图像 provider 实现。 */
    private final List<ImageProvider> providers;

    /** 构造注册表。 */
    public ImageProviderRegistry(List<ImageProvider> providers) {
        this.providers = providers;
    }

    /** 按 providerType 返回匹配的 provider，找不到则抛异常。 */
    public ImageProvider get(String providerType) {
        for (ImageProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No image provider supports type: " + providerType);
    }
}
