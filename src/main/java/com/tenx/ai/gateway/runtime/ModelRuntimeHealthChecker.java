package com.tenx.ai.gateway.runtime;

public interface ModelRuntimeHealthChecker {

    boolean isOnline(String healthUrl);
}
