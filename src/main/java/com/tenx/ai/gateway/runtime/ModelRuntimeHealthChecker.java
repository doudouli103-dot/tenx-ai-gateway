package com.tenx.ai.gateway.runtime;

/**
 * 模型运行时健康检查接口。返回 health-url 对应的运行时是否在线。
 */
public interface ModelRuntimeHealthChecker {

    /** 判断 health-url 对应的运行时是否在线。 */
    boolean isOnline(String healthUrl);
}
