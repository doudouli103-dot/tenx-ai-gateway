package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.runtime.CommandResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeOperationResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeService;
import com.tenx.ai.gateway.runtime.ModelRuntimeStatus;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelRuntimeController {

    private final ModelRuntimeService service;

    public ModelRuntimeController(ModelRuntimeService service) {
        this.service = service;
    }

    @GetMapping("/admin/models")
    public ResponseEntity<List<ModelRuntimeStatus>> models() {
        return ResponseEntity.ok(service.listModels());
    }

    @PostMapping("/admin/models/{model:.+}/start")
    public ResponseEntity<ModelRuntimeOperationResult> start(@PathVariable String model) {
        return ResponseEntity.ok(service.start(model));
    }

    @PostMapping("/admin/models/{model:.+}/stop")
    public ResponseEntity<ModelRuntimeOperationResult> stop(@PathVariable String model) {
        return ResponseEntity.ok(service.stop(model));
    }
}
