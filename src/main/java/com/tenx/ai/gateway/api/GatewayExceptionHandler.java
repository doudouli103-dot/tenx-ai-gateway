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

/**
 * 全局异常处理器，把网关内部和上游错误统一转成 HTTP 响应。
 *
 * <ul>
 *   <li>未知模型 / 参数非法 / 配置缺失 → 400（调用方可修正）。</li>
 *   <li>上游 provider 返回的非 2xx → 透传上游的状态码、响应体和 Content-Type，
 *       避免把上游错误折叠成笼统的 500。</li>
 * </ul>
 */
@RestControllerAdvice
public class GatewayExceptionHandler {

    /** 未知模型名 → 400。 */
    @ExceptionHandler(UnknownModelException.class)
    public Mono<ResponseEntity<String>> unknownModel(UnknownModelException exception) {
        return Mono.just(ResponseEntity.badRequest().body(exception.getMessage()));
    }

    /** 参数非法（如视频时长超限）→ 400。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<String>> illegalArgument(IllegalArgumentException exception) {
        return Mono.just(ResponseEntity.badRequest().body(exception.getMessage()));
    }

    /** 配置缺失（如 provider 未配置）→ 400。 */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<String>> illegalState(IllegalStateException exception) {
        return Mono.just(ResponseEntity.badRequest().body(exception.getMessage()));
    }

    /**
     * 上游 provider 出错：把上游返回的状态码、响应体、Content-Type 原样透传给客户端。
     */
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
