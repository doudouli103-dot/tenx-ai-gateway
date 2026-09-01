package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.routing.UnknownModelException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(UnknownModelException.class)
    public Mono<ResponseEntity<String>> unknownModel(UnknownModelException exception) {
        return Mono.just(ResponseEntity.badRequest().body(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<String>> illegalArgument(IllegalArgumentException exception) {
        return Mono.just(ResponseEntity.badRequest().body(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<String>> illegalState(IllegalStateException exception) {
        return Mono.just(ResponseEntity.badRequest().body(exception.getMessage()));
    }
}
