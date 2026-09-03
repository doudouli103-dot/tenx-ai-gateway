package com.tenx.ai.gateway.provider;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 聊天 provider 注册表：注入所有 {@link ModelProvider} 实现，按 providerType 匹配并返回对应实现。
 */
@Component
public class ModelProviderRegistry {

    /** 所有已注入的聊天 provider 实现。 */
    private final List<ModelProvider> providers;

    /** 构造注册表。 */
    public ModelProviderRegistry(List<ModelProvider> providers) {
        this.providers = providers;
    }

    /** 按 providerType 返回匹配的 provider，找不到则抛异常。 */
    public ModelProvider get(String providerType) {
        for (ModelProvider provider : providers) {
            if (provider.supports(providerType)) {
                return provider;
            }
        }
        throw new IllegalStateException("No provider supports type: " + providerType);
    }
}
