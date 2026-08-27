package com.tenx.ai.gateway.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.model.GeneratedAsset;
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

    public DocumentCenterClient(GatewayProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
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
        bodyBuilder.part("bizType", bizType);
        bodyBuilder.part("source", "tenx-ai-gateway");
        bodyBuilder.part("model", sourceModel);

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
        DocumentUploadResult result = new DocumentUploadResult();
        result.setFileId(text(node, "fileId", text(node, "id", null)));
        result.setUrl(text(node, "url", text(node, "fileUrl", text(node, "downloadUrl", null))));
        result.setFileName(text(node, "fileName", null));
        result.setContentType(text(node, "contentType", null));
        JsonNode sizeNode = node.get("size");
        if (sizeNode != null && sizeNode.canConvertToLong()) {
            result.setSize(sizeNode.asLong());
        }
        return result;
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
