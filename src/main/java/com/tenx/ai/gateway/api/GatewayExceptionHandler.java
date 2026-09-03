package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.provider.UpstreamProviderException;
import com.tenx.ai.gateway.routing.UnknownModelException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @ExceptionHandler(UpstreamProviderException.class)
    public Mono<ResponseEntity<String>> upstreamError(UpstreamProviderException exception) {
        HttpStatus status = exception.getStatus() != null ? exception.getStatus() : HttpStatus.BAD_GATEWAY;
        String contentType = exception.getContentType() != null
                ? exception.getContentType().toString()
                : MediaType.APPLICATION_JSON_VALUE;
        return Mono.just(ResponseEntity.status(status.value())
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(exception.getBody()));
    }
}
