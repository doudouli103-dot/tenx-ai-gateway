package com.tenx.ai.gateway.runtime;

/**
 * 运行时命令执行接口。执行配置好的命令并返回结果。
 */
public interface ModelRuntimeCommandRunner {

    /** 执行命令，最多等待 timeoutMillis 毫秒，返回执行结果。 */
    CommandResult run(String command, long timeoutMillis);
}
