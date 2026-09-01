package com.tenx.ai.gateway.runtime;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ModelRuntimeService {

    private final GatewayProperties properties;
    private final ModelRuntimeHealthChecker healthChecker;
    private final ModelRuntimeCommandRunner commandRunner;

    public ModelRuntimeService(GatewayProperties properties,
                               ModelRuntimeHealthChecker healthChecker,
                               ModelRuntimeCommandRunner commandRunner) {
        this.properties = properties;
        this.healthChecker = healthChecker;
        this.commandRunner = commandRunner;
    }

    public List<ModelRuntimeStatus> listModels() {
        List<ModelRuntimeStatus> models = new ArrayList<ModelRuntimeStatus>();
        for (Map.Entry<String, GatewayProperties.RouteConfig> entry : properties.getRoutes().entrySet()) {
            String modelId = entry.getKey();
            GatewayProperties.RouteConfig route = entry.getValue();
            GatewayProperties.RuntimeConfig runtime = properties.getRuntimes().get(modelId);
            boolean runtimeConfigured = runtime != null;
            String healthUrl = runtimeConfigured ? runtime.getHealthUrl() : null;
            String status = status(runtime);
            models.add(new ModelRuntimeStatus(
                    modelId,
                    route.getCapability(),
                    route.getProvider(),
                    route.getModel(),
                    healthUrl,
                    runtimeConfigured,
                    status
            ));
        }
        return models;
    }

    public ModelRuntimeOperationResult start(String model) {
        GatewayProperties.RuntimeConfig runtime = requireRuntime(model);
        return execute(model, "start", runtime, runtime.getStartCommand());
    }

    public ModelRuntimeOperationResult stop(String model) {
        GatewayProperties.RuntimeConfig runtime = requireRuntime(model);
        return execute(model, "stop", runtime, runtime.getStopCommand());
    }

    private ModelRuntimeOperationResult execute(String model, String action,
                                                GatewayProperties.RuntimeConfig runtime,
                                                String command) {
        String statusBefore = status(runtime);
        CommandResult commandResult = commandRunner.run(command, properties.getAdmin().getCommandTimeoutMillis());
        String statusAfter = status(runtime);
        String resourceCheckOutput = resourceCheck(runtime);
        return ModelRuntimeOperationResult.from(model, action, statusBefore, statusAfter, commandResult, resourceCheckOutput);
    }

    private String resourceCheck(GatewayProperties.RuntimeConfig runtime) {
        String command = runtime.getResourceCheckCommand();
        if (command == null || command.trim().length() == 0) {
            return null;
        }
        CommandResult result = commandRunner.run(command, properties.getAdmin().getCommandTimeoutMillis());
        return result.getOutput();
    }

    private String status(GatewayProperties.RuntimeConfig runtime) {
        if (runtime == null) {
            return "unmanaged";
        }
        if (runtime.getHealthUrl() == null || runtime.getHealthUrl().trim().length() == 0) {
            return "unknown";
        }
        return healthChecker.isOnline(runtime.getHealthUrl()) ? "online" : "offline";
    }

    private GatewayProperties.RuntimeConfig requireRuntime(String model) {
        GatewayProperties.RuntimeConfig runtime = properties.getRuntimes().get(model);
        if (runtime == null) {
            throw new IllegalArgumentException("Runtime is not configured for model: " + model);
        }
        return runtime;
    }
}
