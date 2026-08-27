package com.tenx.ai.gateway.model;

public class GeneratedAsset {

    private final byte[] bytes;
    private final String fileName;
    private final String contentType;

    public GeneratedAsset(byte[] bytes, String fileName, String contentType) {
        this.bytes = bytes;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }
}
