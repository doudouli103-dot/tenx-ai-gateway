package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.runtime.ModelRuntimeOperationResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeService;
import com.tenx.ai.gateway.runtime.ModelRuntimeStatus;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class ModelRuntimeController {

    private final ModelRuntimeService service;

    public ModelRuntimeController(ModelRuntimeService service) {
        this.service = service;
    }

    @GetMapping("/admin/models")
    public Mono<ResponseEntity<List<ModelRuntimeStatus>>> models() {
        return blocking(() -> service.listModels()).map(ResponseEntity::ok);
    }

    @PostMapping("/admin/models/{model:.+}/start")
    public Mono<ResponseEntity<ModelRuntimeOperationResult>> start(@PathVariable String model) {
        return blocking(() -> service.start(model)).map(ResponseEntity::ok);
    }

    @PostMapping("/admin/models/{model:.+}/stop")
    public Mono<ResponseEntity<ModelRuntimeOperationResult>> stop(@PathVariable String model) {
        return blocking(() -> service.stop(model)).map(ResponseEntity::ok);
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }
}
