package com.tenx.ai.gateway.runtime;

public interface ModelRuntimeCommandRunner {

    CommandResult run(String command, long timeoutMillis);
}
