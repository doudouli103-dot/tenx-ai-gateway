package com.tenx.ai.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.document.DocumentCenterClient;
import com.tenx.ai.gateway.document.DocumentUploadResult;
import com.tenx.ai.gateway.model.GeneratedAsset;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DocumentCenterClientTest {

    @Test
    public void parsesWrappedDocumentCenterResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentCenterClient client = new DocumentCenterClient(new GatewayProperties(), null, objectMapper);
        Method method = DocumentCenterClient.class.getDeclaredMethod(
                "toUploadResult",
                com.fasterxml.jackson.databind.JsonNode.class
        );
        method.setAccessible(true);

        Object value = method.invoke(client, objectMapper.readTree("{\"code\":0,\"message\":\"ok\",\"data\":{\"fileId\":\"file-1\",\"downloadUrl\":\"http://doc/api/files/file-1/download\",\"fileName\":\"a.mp4\",\"fileType\":\"video/mp4\",\"fileSize\":123}}"));
        DocumentUploadResult result = (DocumentUploadResult) value;

        Assertions.assertEquals("file-1", result.getFileId());
        Assertions.assertEquals("http://doc/api/files/file-1/download", result.getUrl());
        Assertions.assertEquals("a.mp4", result.getFileName());
        Assertions.assertEquals("video/mp4", result.getContentType());
        Assertions.assertEquals(Long.valueOf(123), result.getSize());
    }

    @Test
    public void rejectsFailedDocumentCenterResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentCenterClient client = new DocumentCenterClient(new GatewayProperties(), null, objectMapper);
        Method method = DocumentCenterClient.class.getDeclaredMethod(
                "toUploadResult",
                com.fasterxml.jackson.databind.JsonNode.class
        );
        method.setAccessible(true);

        Exception exception = Assertions.assertThrows(Exception.class, () ->
                method.invoke(client, objectMapper.readTree("{\"code\":500,\"message\":\"upload failed\"}"))
        );

        Assertions.assertTrue(exception.getCause().getMessage().contains("upload failed"));
    }

    @Test
    public void keepsGeneratedAssetInMemory() {
        GeneratedAsset asset = new GeneratedAsset("image".getBytes(), "a.png", "image/png");

        Assertions.assertEquals("a.png", asset.getFileName());
        Assertions.assertEquals("image/png", asset.getContentType());
        Assertions.assertEquals(5, asset.getBytes().length);
    }
}
