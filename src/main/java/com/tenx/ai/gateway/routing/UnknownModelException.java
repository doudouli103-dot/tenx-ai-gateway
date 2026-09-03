package com.tenx.ai.gateway.routing;

/**
 * 请求的模型名未在 {@code routes} 配置中定义。由异常处理器映射为 HTTP 400。
 */
public class UnknownModelException extends RuntimeException {

    /** 构造异常并附带模型名。 */
    public UnknownModelException(String model) {
        super("Unknown model: " + model);
    }
}
