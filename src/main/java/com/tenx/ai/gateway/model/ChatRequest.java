package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天补全请求（OpenAI /v1/chat/completions 请求体）。
 *
 * <p>除 model/messages/stream 三个显式字段外，其余字段（temperature、max_tokens 等）
 * 通过 {@code @JsonAnySetter/Getter} 原样透传给上游，保证网关不丢失客户端参数。
 */
public class ChatRequest {

    /** 请求的模型名（对应 routes 配置的 key）。 */
    private String model;

    /** 对话消息列表。 */
    private List<ChatMessage> messages = new ArrayList<ChatMessage>();

    /** 是否流式返回，可为 null（表示未指定）。 */
    private Boolean stream;

    /** 未显式声明的其它字段（temperature、max_tokens 等），透传给上游。 */
    private Map<String, JsonNode> extra = new LinkedHashMap<String, JsonNode>();

    /** 返回请求的模型名。 */
    public String getModel() {
        return model;
    }

    /** 设置请求的模型名。 */
    public void setModel(String model) {
        this.model = model;
    }

    /** 返回对话消息列表。 */
    public List<ChatMessage> getMessages() {
        return messages;
    }

    /** 设置对话消息列表。 */
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    /** 返回流式标志（可为 null）。 */
    public Boolean getStream() {
        return stream;
    }

    /** 设置流式标志。 */
    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    /** 判断是否请求流式返回（仅显式 true 时为流式）。 */
    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

    /** 返回所有透传的扩展字段。 */
    @JsonAnyGetter
    public Map<String, JsonNode> getExtra() {
        return extra;
    }

    /** 收集反序列化时未匹配到显式字段的键值对，用于透传。 */
    @JsonAnySetter
    public void setExtra(String name, JsonNode value) {
        this.extra.put(name, value);
    }

    /** 复制一份请求并把 model 替换为真实模型名，用于转发给上游。 */
    public ChatRequest copyForModel(String realModel) {
        ChatRequest copy = new ChatRequest();
        copy.setModel(realModel);
        copy.setMessages(this.messages);
        copy.setStream(this.stream);
        copy.getExtra().putAll(this.extra);
        return copy;
    }
}
