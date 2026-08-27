package com.tenx.ai.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VideoTaskResponse {

    @JsonProperty("task_id")
    private String taskId;
    private String status;
    @JsonProperty("file_id")
    private String fileId;
    @JsonProperty("result_url")
    private String resultUrl;
    private String error;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
