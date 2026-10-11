package com.tenx.ai.gateway.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tenx.ai.gateway.model.VideoGenerationRequest;
import java.security.SecureRandom;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Fixed, server-owned text-to-frame workflows for the locally installed ComfyUI models. */
@Component
public class ComfyUiVideoWorkflowFactory {

    public static final String WORKFLOW_VERSION = "short-drama-v1";

    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public ComfyUiVideoWorkflowFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectNode create(VideoGenerationRequest request, String resultPrefix) {
        return create(request, resultPrefix, null);
    }

    public ObjectNode create(VideoGenerationRequest request, String resultPrefix,
                             String startImageName) {
        boolean hunyuan = "HunyuanVideo-1.5".equals(request.getModel());
        if (!hunyuan && !"Wan2.2-TI2V-5B".equals(request.getModel())) {
            throw new IllegalArgumentException("Unsupported ComfyUI video model: " + request.getModel());
        }
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("prompt is required");
        }
        if (request.getDuration() == null || request.getDuration() < 1 || request.getDuration() > 5) {
            throw new IllegalArgumentException("duration must be from 1 to 5 seconds");
        }
        validateIdentifier("project_id", request.getProjectId());
        validateIdentifier("scene_id", request.getSceneId());
        validateIdentifier("shot_id", request.getShotId());
        if (hunyuan && startImageName != null) {
            throw new IllegalArgumentException("Reference frames are currently supported only by Wan2.2-TI2V-5B");
        }
        String size = request.getSize() == null ? "640x352" : request.getSize();
        if (!size.matches("[0-9]{2,4}x[0-9]{2,4}")) {
            throw new IllegalArgumentException("size must be WIDTHxHEIGHT");
        }
        String[] parts = size.split("x");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);
        int multiple = hunyuan ? 16 : 32;
        if (width < 256 || height < 256 || width > 1280 || height > 720
                || width % multiple != 0 || height % multiple != 0) {
            throw new IllegalArgumentException("width and height must be multiples of " + multiple + " from 256 up to 1280x720");
        }
        long seed = random.nextLong() & Long.MAX_VALUE;
        int steps = 14;
        String negative = "";
        for (Map.Entry<String, JsonNode> extra : request.getExtra().entrySet()) {
            JsonNode value = extra.getValue();
            switch (extra.getKey()) {
                case "seed":
                    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
                        throw new IllegalArgumentException("seed must be a non-negative integer");
                    }
                    seed = value.longValue();
                    break;
                case "steps":
                    if (!value.isInt() || value.intValue() < 1 || value.intValue() > 30) {
                        throw new IllegalArgumentException("steps must be from 1 to 30");
                    }
                    steps = value.intValue();
                    break;
                case "negative_prompt":
                    if (!value.isTextual()) {
                        throw new IllegalArgumentException("negative_prompt must be text");
                    }
                    negative = value.asText();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported video parameter: " + extra.getKey());
            }
        }
        int frames = request.getDuration() * 24 + 1;
        ObjectNode workflow = mapper.createObjectNode();
        if (hunyuan) {
            node(workflow, "1", "DualCLIPLoader", "clip_name1", "qwen_2.5_vl_7b_fp8_scaled.safetensors",
                    "clip_name2", "byt5_small_glyphxl_fp16.safetensors", "type", "hunyuan_video_15");
            node(workflow, "2", "UNETLoader", "unet_name", "hunyuanvideo1.5_720p_t2v_fp16.safetensors", "weight_dtype", "default");
            node(workflow, "3", "VAELoader", "vae_name", "hunyuanvideo15_vae_fp16.safetensors");
            node(workflow, "6", "EmptyHunyuanVideo15Latent", "width", width, "height", height,
                    "length", frames, "batch_size", 1);
        } else {
            node(workflow, "1", "CLIPLoader", "clip_name", "umt5_xxl_fp16.safetensors", "type", "wan");
            node(workflow, "2", "UNETLoader", "unet_name", "wan2.2_ti2v_5B_fp16.safetensors", "weight_dtype", "default");
            node(workflow, "3", "VAELoader", "vae_name", "wan2.2_vae.safetensors");
            node(workflow, "6", "Wan22ImageToVideoLatent", "vae", link("3", 0), "width", width,
                    "height", height, "length", frames, "batch_size", 1);
            if (startImageName != null) {
                node(workflow, "12", "LoadImage", "image", startImageName);
                ((ObjectNode) workflow.path("6").path("inputs")).set("start_image", mapper.valueToTree(link("12", 0)));
            }
        }
        node(workflow, "4", "CLIPTextEncode", "clip", link("1", 0), "text", request.getPrompt());
        node(workflow, "5", "CLIPTextEncode", "clip", link("1", 0), "text", negative);
        node(workflow, "7", "ModelSamplingSD3", "model", link("2", 0), "shift", hunyuan ? 7 : 8);
        node(workflow, "8", "KSampler", "model", link("7", 0), "positive", link("4", 0),
                "negative", link("5", 0), "latent_image", link("6", 0), "seed", seed,
                "steps", steps, "cfg", hunyuan ? 6 : 5, "sampler_name", "euler",
                "scheduler", "simple", "denoise", 1);
        node(workflow, "9", "VAEDecode", "samples", link("8", 0), "vae", link("3", 0));
        node(workflow, "11", "SaveImage", "images", link("9", 0),
                "filename_prefix", "tenx-video-frames/" + resultPrefix + "/frame");
        return workflow;
    }

    private void validateIdentifier(String name, String value) {
        if (value != null && !value.isEmpty() && !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
            throw new IllegalArgumentException(name + " must use 1-64 letters, numbers, dots, underscores or hyphens");
        }
    }

    private ObjectNode node(ObjectNode workflow, String id, String type, Object... pairs) {
        ObjectNode node = workflow.putObject(id);
        node.put("class_type", type);
        ObjectNode inputs = node.putObject("inputs");
        for (int i = 0; i < pairs.length; i += 2) {
            inputs.set((String) pairs[i], mapper.valueToTree(pairs[i + 1]));
        }
        return node;
    }

    private Object[] link(String id, int index) {
        return new Object[] {id, index};
    }
}
