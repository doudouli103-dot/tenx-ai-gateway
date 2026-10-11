package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;

/**
 * 视频生成请求（OpenAI /v1/videos/generations 请求体）。
 * 除显式字段外，未知字段透传给上游。duration 未传时由 Controller 用路由默认值补齐。
 */
public class VideoGenerationRequest {

    /** 请求的模型名（对应 routes 配置的 key），必填。 */
    @NotBlank
    private String model;

    /** 视频生成提示词，必填。 */
    @NotBlank
    private String prompt;

    /** 视频时长（秒），可为空，为空时用路由默认值。 */
    private Integer duration;

    /** 视频尺寸；未指定时由各模型工作流选择合适默认值。 */
    private String size;

    /** Optional short-drama project identifier supplied by the orchestration layer. */
    @JsonProperty("project_id")
    private String projectId;

    /** Optional scene identifier supplied by the orchestration layer. */
    @JsonProperty("scene_id")
    private String sceneId;

    /** Optional shot identifier supplied by the orchestration layer. */
    @JsonProperty("shot_id")
    private String shotId;

    /** Existing generated image used as the first frame. */
    @JsonProperty("start_image_result_id")
    @JsonAlias("input_image_result_id")
    private String startImageResultId;

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

    /** 返回视频时长（秒，可为空）。 */
    public Integer getDuration() {
        return duration;
    }

    /** 设置视频时长（秒）。 */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /** 返回视频尺寸。 */
    public String getSize() {
        return size;
    }

    /** 设置视频尺寸。 */
    public void setSize(String size) {
        this.size = size;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String value) { this.projectId = value; }
    public String getSceneId() { return sceneId; }
    public void setSceneId(String value) { this.sceneId = value; }
    public String getShotId() { return shotId; }
    public void setShotId(String value) { this.shotId = value; }
    public String getStartImageResultId() { return startImageResultId; }
    public void setStartImageResultId(String value) { this.startImageResultId = value; }

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

    /** 复制一份请求并把 model 替换为真实模型名、duration 替换为解析后的时长，用于转发给上游。 */
    public VideoGenerationRequest copyForModel(String realModel, Integer resolvedDuration) {
        VideoGenerationRequest copy = new VideoGenerationRequest();
        copy.setModel(realModel);
        copy.setPrompt(prompt);
        copy.setDuration(resolvedDuration);
        copy.setSize(size);
        copy.setProjectId(projectId);
        copy.setSceneId(sceneId);
        copy.setShotId(shotId);
        copy.setStartImageResultId(startImageResultId);
        copy.getExtra().putAll(extra);
        return copy;
    }
}
