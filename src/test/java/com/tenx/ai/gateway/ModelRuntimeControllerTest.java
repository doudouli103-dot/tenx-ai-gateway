package com.tenx.ai.gateway;

import com.tenx.ai.gateway.api.ModelRuntimeController;
import com.tenx.ai.gateway.runtime.ModelRuntimeOperationResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeService;
import com.tenx.ai.gateway.runtime.ModelRuntimeStatus;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

public class ModelRuntimeControllerTest {

    @Test
    public void exposesModelRuntimeList() {
        ModelRuntimeController controller = new ModelRuntimeController(new StubService());

        StepVerifier.create(controller.models())
                .assertNext(response -> {
                    Assertions.assertEquals(200, response.getStatusCodeValue());
                    Assertions.assertEquals("qwen-small", response.getBody().get(0).getId());
                })
                .verifyComplete();
    }

    @Test
    public void startsModelByName() {
        ModelRuntimeController controller = new ModelRuntimeController(new StubService());

        StepVerifier.create(controller.start("qwen-small"))
                .assertNext(response -> {
                    Assertions.assertEquals(200, response.getStatusCodeValue());
                    Assertions.assertEquals("qwen-small", response.getBody().getModel());
                    Assertions.assertEquals("start", response.getBody().getAction());
                })
                .verifyComplete();
    }

    @Test
    public void stopsModelByName() {
        ModelRuntimeController controller = new ModelRuntimeController(new StubService());

        StepVerifier.create(controller.stop("qwen-small"))
                .assertNext(response -> {
                    Assertions.assertEquals(200, response.getStatusCodeValue());
                    Assertions.assertEquals("qwen-small", response.getBody().getModel());
                    Assertions.assertEquals("stop", response.getBody().getAction());
                })
                .verifyComplete();
    }

    private static class StubService extends ModelRuntimeService {
        StubService() {
            super(null, null, null);
        }

        @Override
        public List<ModelRuntimeStatus> listModels() {
            return Arrays.asList(new ModelRuntimeStatus(
                    "qwen-small",
                    "chat",
                    "local-compatible",
                    "qwen-small",
                    "http://127.0.0.1:4000/health",
                    true,
                    "online"
            ));
        }

        @Override
        public ModelRuntimeOperationResult start(String model) {
            return ModelRuntimeOperationResult.from(model, "start", "offline", "online", null, null);
        }

        @Override
        public ModelRuntimeOperationResult stop(String model) {
            return ModelRuntimeOperationResult.from(model, "stop", "online", "offline", null, null);
        }
    }
}
