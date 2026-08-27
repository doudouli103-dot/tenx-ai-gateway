package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;

public class ImageGenerationRequest {

    @NotBlank
    private String model;

    @NotBlank
    private String prompt;

    private String size = "1024x1024";
    private Integer n = 1;
    private Map<String, JsonNode> extra = new LinkedHashMap<String, JsonNode>();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    @JsonAnyGetter
    public Map<String, JsonNode> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void setExtra(String name, JsonNode value) {
        this.extra.put(name, value);
    }

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
