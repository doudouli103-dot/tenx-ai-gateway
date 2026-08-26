package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChatRequest {

    private String model;
    private List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private Boolean stream;
    private Map<String, JsonNode> extra = new LinkedHashMap<String, JsonNode>();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void setExtra(String name, JsonNode value) {
        this.extra.put(name, value);
    }

    public ChatRequest copyForModel(String realModel) {
        ChatRequest copy = new ChatRequest();
        copy.setModel(realModel);
        copy.setMessages(this.messages);
        copy.setStream(this.stream);
        copy.getExtra().putAll(this.extra);
        return copy;
    }
}
