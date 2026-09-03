package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;

/**
 * 图像生成请求（OpenAI /v1/images/generations 请求体）。
 * 除显式字段外，未知字段透传给上游。
 */
public class ImageGenerationRequest {

    /** 请求的模型名（对应 routes 配置的 key），必填。 */
    @NotBlank
    private String model;

    /** 图像生成提示词，必填。 */
    @NotBlank
    private String prompt;

    /** 生成图像尺寸，默认 1024x1024。 */
    private String size = "1024x1024";

    /** 生成图像数量，默认 1。 */
    private Integer n = 1;

    /** 未显式声明的其它字段，透传给上游。 */
    private Map<String, JsonNode> extra = new LinkedHashMap<String, JsonNode>();

    /** 返回请求的模型名。 */
    public String getModel() {
        return model;
    }

    /** 设置请求的模型名。 */
    public void setModel(String model) {
        this.model = model;
    }

    /** 返回生成提示词。 */
    public String getPrompt() {
        return prompt;
    }

    /** 设置生成提示词。 */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /** 返回生成尺寸。 */
    public String getSize() {
        return size;
    }

    /** 设置生成尺寸。 */
    public void setSize(String size) {
        this.size = size;
    }

    /** 返回生成数量。 */
    public Integer getN() {
        return n;
    }

    /** 设置生成数量。 */
    public void setN(Integer n) {
        this.n = n;
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
    public ImageGenerationRequest copyForModel(String realModel) {
        ImageGenerationRequest copy = new ImageGenerationRequest();
        copy.setModel(realModel);
        copy.setPrompt(prompt);
        copy.setSize(size);
        copy.setN(n);
        copy.getExtra().putAll(extra);
        return copy;
    }
}
