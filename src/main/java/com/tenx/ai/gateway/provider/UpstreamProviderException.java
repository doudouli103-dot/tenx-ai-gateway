package com.tenx.ai.gateway.provider;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * 上游 provider 返回非 2xx 时抛出的异常。
 * 携带原始状态码、响应体和 Content-Type，让网关把错误原样透传，而不是折叠成笼统的 500。
 */
public class UpstreamProviderException extends RuntimeException {

    /** 上游返回的 HTTP 状态码。 */
    private final HttpStatus status;

    /** 上游返回的响应体（文本）。 */
    private final String body;

    /** 上游响应的 Content-Type（可为空）。 */
    private final MediaType contentType;

    /** 构造上游错误异常。 */
    public UpstreamProviderException(HttpStatus status, String body, MediaType contentType) {
        super("Upstream provider returned " + status.value());
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    /** 返回上游状态码。 */
    public HttpStatus getStatus() {
        return status;
    }

    /** 返回上游响应体。 */
    public String getBody() {
        return body;
    }

    /** 返回上游响应的 Content-Type。 */
    public MediaType getContentType() {
        return contentType;
    }

    /**
     * 服务端错误和限流是临时性的，值得回落重试；
     * 客户端错误（4xx，除 429）说明请求本身有问题，备用 provider 也无法修复，不应回落。
     */
    public boolean isRetryable() {
        int code = status.value();
        return code == 429 || (code >= 500 && code <= 599);
    }

    /** 从 ClientResponse 读取状态码、Content-Type 和响应体，构建异常。 */
    public static Mono<? extends Throwable> fromResponse(ClientResponse response) {
        MediaType contentType = response.headers().contentType().orElse(null);
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new UpstreamProviderException(response.statusCode(), body, contentType));
    }
}
