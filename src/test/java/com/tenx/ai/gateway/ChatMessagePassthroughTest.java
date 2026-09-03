package com.tenx.ai.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenx.ai.gateway.model.ChatMessage;
import com.tenx.ai.gateway.provider.UpstreamProviderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class ChatMessagePassthroughTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void preservesToolCallsAndOtherUnknownFields() throws Exception {
        String json = "{\"role\":\"assistant\",\"content\":\"ok\","
                + "\"tool_calls\":[{\"id\":\"1\",\"type\":\"function\",\"function\":{\"name\":\"f\",\"arguments\":\"{}\"}}],"
                + "\"name\":\"assistant\"}";

        ChatMessage message = mapper.readValue(json, ChatMessage.class);

        Assertions.assertEquals("assistant", message.getRole());
        Assertions.assertEquals("ok", message.getContent().asText());
        Assertions.assertTrue(message.getExtra().containsKey("tool_calls"));
        Assertions.assertTrue(message.getExtra().containsKey("name"));
        Assertions.assertEquals("f",
                message.getExtra().get("tool_calls").get(0).get("function").get("name").asText());
    }

    @Test
    public void preservesNonStringContentForMultimodalInput() throws Exception {
        String json = "{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"hi\"},"
                + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"http://x/img.png\"}}]}";

        ChatMessage message = mapper.readValue(json, ChatMessage.class);

        JsonNode content = message.getContent();
        Assertions.assertTrue(content.isArray());
        Assertions.assertEquals("hi", content.get(0).get("text").asText());
        Assertions.assertEquals("http://x/img.png", content.get(1).get("image_url").get("url").asText());
    }

    @Test
    public void upstreamExceptionMarksOnlyTransientStatusesAsRetryable() {
        Assertions.assertTrue(new UpstreamProviderException(HttpStatus.SERVICE_UNAVAILABLE, "", MediaType.APPLICATION_JSON).isRetryable());
        Assertions.assertTrue(new UpstreamProviderException(HttpStatus.TOO_MANY_REQUESTS, "", MediaType.APPLICATION_JSON).isRetryable());

        Assertions.assertFalse(new UpstreamProviderException(HttpStatus.BAD_REQUEST, "", MediaType.APPLICATION_JSON).isRetryable());
        Assertions.assertFalse(new UpstreamProviderException(HttpStatus.NOT_FOUND, "", MediaType.APPLICATION_JSON).isRetryable());
        Assertions.assertFalse(new UpstreamProviderException(HttpStatus.UNAUTHORIZED, "", MediaType.APPLICATION_JSON).isRetryable());
    }
}
