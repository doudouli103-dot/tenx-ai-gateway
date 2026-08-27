package com.tenx.ai.gateway.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.GeneratedAsset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class DocumentCenterClient {

    private final GatewayProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public DocumentCenterClient(GatewayProperties properties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    public Mono<DocumentUploadResult> upload(String bizType, String sourceModel, GeneratedAsset asset) {
        GatewayProperties.DocumentCenterConfig config = properties.getDocumentCenter();
        if (config == null || isBlank(config.getBaseUrl())) {
            return Mono.error(new IllegalStateException("Document center base-url is not configured"));
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new NamedByteArrayResource(asset.getBytes(), asset.getFileName()))
                .filename(asset.getFileName())
                .contentType(MediaType.parseMediaType(asset.getContentType()));
        bodyBuilder.part("category", bizType);
        bodyBuilder.part("remark", buildRemark(bizType, sourceModel));

        return webClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .build()
                .post()
                .uri(config.getUploadPath())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::toUploadResult);
    }

    private DocumentUploadResult toUploadResult(JsonNode node) {
        JsonNode code = node.get("code");
        if (code != null && code.canConvertToInt() && code.asInt() != 0) {
            throw new IllegalStateException("Document center upload failed: " + text(node, "message", "unknown error"));
        }

        JsonNode payload = node.has("data") && node.get("data") != null && !node.get("data").isNull()
                ? node.get("data")
                : node;
        DocumentUploadResult result = new DocumentUploadResult();
        result.setFileId(text(payload, "fileId", text(payload, "id", null)));
        result.setUrl(text(payload, "url", text(payload, "fileUrl", text(payload, "downloadUrl", null))));
        result.setFileName(text(payload, "fileName", null));
        result.setContentType(text(payload, "contentType", text(payload, "fileType", null)));
        JsonNode sizeNode = payload.has("size") ? payload.get("size") : payload.get("fileSize");
        if (sizeNode != null && sizeNode.canConvertToLong()) {
            result.setSize(sizeNode.asLong());
        }
        return result;
    }

    private String buildRemark(String bizType, String sourceModel) {
        Map<String, String> remark = new LinkedHashMap<String, String>();
        remark.put("source", "tenx-ai-gateway");
        remark.put("bizType", bizType);
        remark.put("model", sourceModel);
        try {
            return objectMapper.writeValueAsString(remark);
        } catch (Exception ignored) {
            return "source=tenx-ai-gateway,bizType=" + bizType + ",model=" + sourceModel;
        }
    }

    private String text(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asText();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String fileName;

        NamedByteArrayResource(byte[] byteArray, String fileName) {
            super(byteArray);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
