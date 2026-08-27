package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelController {

    private final GatewayProperties properties;

    public ModelController(GatewayProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/v1/models")
    public ResponseEntity<Map<String, Object>> models() {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (String modelName : properties.getRoutes().keySet()) {
            Map<String, String> model = new LinkedHashMap<String, String>();
            model.put("id", modelName);
            model.put("object", "model");
            model.put("capability", properties.getRoutes().get(modelName).getCapability());
            data.add(model);
        }

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("object", "list");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
