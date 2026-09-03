package com.tenx.ai.gateway.provider;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Raised when an upstream provider responds with a non-2xx status.
 * Carries the original status, body, and content type so the Gateway can
 * pass the error through instead of collapsing it into a generic 500.
 */
public class UpstreamProviderException extends RuntimeException {

    private final HttpStatus status;
    private final String body;
    private final MediaType contentType;

    public UpstreamProviderException(HttpStatus status, String body, MediaType contentType) {
        super("Upstream provider returned " + status.value());
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public MediaType getContentType() {
        return contentType;
    }

    /**
     * Server errors and rate limits are transient and worth retrying against a
     * fallback. Client errors (4xx except 429) indicate a bad request that a
     * fallback provider cannot fix, so they must not trigger fallback routing.
     */
    public boolean isRetryable() {
        int code = status.value();
        return code == 429 || (code >= 500 && code <= 599);
    }

    public static Mono<? extends Throwable> fromResponse(ClientResponse response) {
        MediaType contentType = response.headers().contentType().orElse(null);
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new UpstreamProviderException(response.statusCode(), body, contentType));
    }
}
