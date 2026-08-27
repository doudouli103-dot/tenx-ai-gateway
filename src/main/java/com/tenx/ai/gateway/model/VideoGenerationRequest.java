package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;

public class VideoGenerationRequest {

    @NotBlank
    private String model;

    @NotBlank
    private String prompt;

    private Integer duration;
    private String size = "1280x720";
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @JsonAnyGetter
    public Map<String, JsonNode> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void setExtra(String name, JsonNode value) {
        this.extra.put(name, value);
    }

    public VideoGenerationRequest copyForModel(String realModel, Integer resolvedDuration) {
        VideoGenerationRequest copy = new VideoGenerationRequest();
        copy.setModel(realModel);
        copy.setPrompt(prompt);
        copy.setDuration(resolvedDuration);
        copy.setSize(size);
        copy.getExtra().putAll(extra);
        return copy;
    }
}
