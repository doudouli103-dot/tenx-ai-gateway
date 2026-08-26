package com.tenx.ai.gateway.routing;

public class UnknownModelException extends RuntimeException {

    public UnknownModelException(String model) {
        super("Unknown model alias: " + model);
    }
}
