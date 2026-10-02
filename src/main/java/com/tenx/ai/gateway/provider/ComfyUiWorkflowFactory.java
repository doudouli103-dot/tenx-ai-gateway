package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tenx.ai.gateway.model.ImageGenerationRequest;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Builds a ComfyUI API workflow from one of the packaged, fixed templates. */
@Component
public class ComfyUiWorkflowFactory {

    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public ComfyUiWorkflowFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectNode create(ImageGenerationRequest request, String resultPrefix) {
        String model = request.getModel();
        if (!"qwen-image".equals(model) && !"flux-dev".equals(model)) {
            throw new IllegalArgumentException("Unsupported ComfyUI image model: " + model);
        }
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("prompt is required");
        }

        String size = request.getSize();
        if (size == null || !size.matches("[0-9]{2,4}x[0-9]{2,4}")) {
            throw new IllegalArgumentException("size must be WIDTHxHEIGHT");
        }
        String[] dimensions = size.split("x");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        if (width < 256 || width > 2048 || height < 256 || height > 2048
                || width % 8 != 0 || height % 8 != 0) {
            throw new IllegalArgumentException("width and height must be multiples of 8 from 256 to 2048");
        }
        int count = request.getN() == null ? 1 : request.getN().intValue();
        if (count < 1 || count > 4) {
            throw new IllegalArgumentException("n must be from 1 to 4");
        }

        long seed = random.nextLong() & Long.MAX_VALUE;
        int steps = 20;
        String negativePrompt = "";
        for (Map.Entry<String, JsonNode> extra : request.getExtra().entrySet()) {
            String key = extra.getKey();
            JsonNode value = extra.getValue();
            if ("seed".equals(key)) {
                if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
                    throw new IllegalArgumentException("seed must be a non-negative integer");
                }
                seed = value.longValue();
            } else if ("steps".equals(key)) {
                if (!value.isInt() || value.intValue() < 1 || value.intValue() > 100) {
                    throw new IllegalArgumentException("steps must be from 1 to 100");
                }
                steps = value.intValue();
            } else if ("negative_prompt".equals(key)) {
                if (!value.isTextual()) {
                    throw new IllegalArgumentException("negative_prompt must be text");
                }
                negativePrompt = value.asText();
            } else if ("response_format".equals(key)) {
                if (!"url".equals(value.asText())) {
                    throw new IllegalArgumentException("Only response_format=url is supported");
                }
            } else if (!"user".equals(key)) {
                throw new IllegalArgumentException("Unsupported image parameter: " + key);
            }
        }
        if ("flux-dev".equals(model) && !negativePrompt.isEmpty()) {
            throw new IllegalArgumentException("negative_prompt is not supported for flux-dev");
        }

        String resourceName = "comfyui/" + model + ".json";
        ObjectNode workflow;
        try (InputStream stream = new ClassPathResource(resourceName).getInputStream()) {
            workflow = (ObjectNode) mapper.readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("ComfyUI workflow template is unavailable: " + model, exception);
        }
        String promptNode = "qwen-image".equals(model) ? "4" : "2";
        String latentNode = "qwen-image".equals(model) ? "6" : "4";
        String samplerNode = "qwen-image".equals(model) ? "8" : "5";
        ((ObjectNode) workflow.path(promptNode).path("inputs")).put("text", request.getPrompt());
        if ("qwen-image".equals(model)) {
            ((ObjectNode) workflow.path("5").path("inputs")).put("text", negativePrompt);
        }
        ObjectNode latentInputs = (ObjectNode) workflow.path(latentNode).path("inputs");
        latentInputs.put("width", width);
        latentInputs.put("height", height);
        latentInputs.put("batch_size", count);
        ObjectNode samplerInputs = (ObjectNode) workflow.path(samplerNode).path("inputs");
        samplerInputs.put("seed", seed);
        samplerInputs.put("steps", steps);
        ((ObjectNode) workflow.path("10").path("inputs")).put("filename_prefix", "tenx/" + resultPrefix);
        return workflow;
    }
}
