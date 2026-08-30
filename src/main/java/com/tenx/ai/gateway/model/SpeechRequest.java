package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;

public class SpeechRequest {

    @NotBlank
    private String model;

    @NotBlank
    private String input;

    private String voice = "default";
    @JsonProperty("response_format")
    private String responseFormat = "wav";
    private Double speed;
    private Map<String, JsonNode> extra = new LinkedHashMap<String, JsonNode>();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    @JsonAnyGetter
    public Map<String, JsonNode> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void setExtra(String name, JsonNode value) {
        this.extra.put(name, value);
    }

    public SpeechRequest copyForModel(String realModel) {
        SpeechRequest copy = new SpeechRequest();
        copy.setModel(realModel);
        copy.setInput(input);
        copy.setVoice(voice);
        copy.setResponseFormat(responseFormat);
        copy.setSpeed(speed);
        copy.getExtra().putAll(extra);
        return copy;
    }
}
