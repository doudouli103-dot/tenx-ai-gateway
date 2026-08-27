package com.tenx.ai.gateway.video;

public class VideoTask {

    private final String taskId;
    private volatile String status;
    private volatile String fileId;
    private volatile String resultUrl;
    private volatile String error;

    public VideoTask(String taskId) {
        this.taskId = taskId;
        this.status = "queued";
    }

    public String getTaskId() {
        return taskId;
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
