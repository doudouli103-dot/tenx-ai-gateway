package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天消息（OpenAI ChatCompletion 的 message 结构）。
 *
 * <p>为「透传」给上游做了两层设计：
 * <ul>
 *   <li>{@code content} 用 {@link JsonNode} 承载，兼容纯文本字符串，也兼容多模态数组（如图片输入）。</li>
 *   <li>role/content 之外的未知字段（tool_calls、name、tool_call_id 等）通过
 *       {@code @JsonAnySetter/Getter} 原样保留，转发时不会被丢弃。</li>
 * </ul>
 */
public class ChatMessage {

    /** 消息角色：system / user / assistant / tool 等。 */
    private String role;

    /** 消息内容：可为文本字符串，也可为多模态的 JSON 数组。 */
    private JsonNode content;

    /** 未显式声明的其它字段（tool_calls、name、tool_call_id 等），透传给上游。 */
    private Map<String, JsonNode> extra = new LinkedHashMap<String, JsonNode>();

    /** 无参构造，供 Jackson 反序列化使用。 */
    public ChatMessage() {
    }

    /** 返回消息角色。 */
    public String getRole() {
        return role;
    }

    /** 设置消息角色。 */
    public void setRole(String role) {
        this.role = role;
    }

    /** 返回消息内容（文本或数组）。 */
    public JsonNode getContent() {
        return content;
    }

    /** 设置消息内容。 */
    public void setContent(JsonNode content) {
        this.content = content;
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
}
