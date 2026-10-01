package com.tenx.ai.gateway;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 通过真实 llama-server 验证 Gateway 的鉴权、路由和 OpenAI 兼容转发。
 *
 * <p>该测试依赖本机模型服务，默认不执行。设置
 * {@code TENX_RUN_LIVE_MODEL_TEST=true} 后才会启用，避免普通单测和 CI
 * 因外部模型未启动而失败。
 */
@EnabledIfEnvironmentVariable(named = "TENX_RUN_LIVE_MODEL_TEST", matches = "(?i:true|1)")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "tenx.ai.gateway.api-keys=integration-test-key",
                "tenx.ai.gateway.admin.enabled=false",
                "tenx.ai.gateway.providers.local-compatible.base-url=${TENX_LIVE_MODEL_BASE_URL:http://127.0.0.1:4000}",
                "tenx.ai.gateway.providers.local-compatible.api-key=${TENX_LIVE_MODEL_API_KEY:local-dev-key}"
        }
)
public class LiveLlamaGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void forwardsChatCompletionToLiveQwenSmallModel() {
        String request = "{"
                + "\"model\":\"qwen-small\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"只回复 OK\"}],"
                + "\"chat_template_kwargs\":{\"enable_thinking\":false},"
                + "\"temperature\":0,"
                + "\"max_tokens\":32"
                + "}";

        webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(120))
                .build()
                .post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer integration-test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.object").isEqualTo("chat.completion")
                .jsonPath("$.model").isEqualTo("qwen-small")
                .jsonPath("$.choices[0].message.content").isEqualTo("OK");
    }
}
