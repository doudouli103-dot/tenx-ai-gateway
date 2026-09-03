package com.tenx.ai.gateway.runtime;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 模型运行时管理服务：列出各模型的运行时状态，并执行配置好的启动/停止命令。
 *
 * <p>注意：网关本身<b>不启动</b>模型运行时，只负责执行 {@code runtimes} 配置里的脚本，
 * 并通过 health-url 判断运行时是否在线。命令内容仅来自配置，绝不由前端传入。
 */
@Service
public class ModelRuntimeService {

    /** 全局配置对象，提供 routes 与 runtimes 配置。 */
    private final GatewayProperties properties;

    /** 健康检查器，判断运行时是否在线。 */
    private final ModelRuntimeHealthChecker healthChecker;

    /** 命令执行器，执行 start/stop 脚本。 */
    private final ModelRuntimeCommandRunner commandRunner;

    /** 构造运行时管理服务。 */
    public ModelRuntimeService(GatewayProperties properties,
                               ModelRuntimeHealthChecker healthChecker,
                               ModelRuntimeCommandRunner commandRunner) {
        this.properties = properties;
        this.healthChecker = healthChecker;
        this.commandRunner = commandRunner;
    }

    /** 列出所有已配置路由模型及其运行时状态。无运行时配置的模型状态记为 unmanaged。 */
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

    /** 启动指定模型的运行时。 */
    public ModelRuntimeOperationResult start(String model) {
        GatewayProperties.RuntimeConfig runtime = requireRuntime(model);
        return execute(model, "start", runtime, runtime.getStartCommand());
    }

    /** 停止指定模型的运行时。 */
    public ModelRuntimeOperationResult stop(String model) {
        GatewayProperties.RuntimeConfig runtime = requireRuntime(model);
        return execute(model, "stop", runtime, runtime.getStopCommand());
    }

    /**
     * 执行一次启动/停止操作：记录操作前状态 → 执行命令 → 记录操作后状态 → 执行资源检查。
     * 操作是否成功由「命令退出码 + 操作后状态是否符合预期」共同决定。
     */
    private ModelRuntimeOperationResult execute(String model, String action,
                                                GatewayProperties.RuntimeConfig runtime,
                                                String command) {
        String statusBefore = status(runtime);
        CommandResult commandResult = commandRunner.run(command, properties.getAdmin().getCommandTimeoutMillis());
        String statusAfter = status(runtime);
        String resourceCheckOutput = resourceCheck(runtime);
        return ModelRuntimeOperationResult.from(model, action, statusBefore, statusAfter, commandResult, resourceCheckOutput);
    }

    /** 执行资源检查命令并返回其输出（未配置返回 null）。 */
    private String resourceCheck(GatewayProperties.RuntimeConfig runtime) {
        String command = runtime.getResourceCheckCommand();
        if (command == null || command.trim().length() == 0) {
            return null;
        }
        CommandResult result = commandRunner.run(command, properties.getAdmin().getCommandTimeoutMillis());
        return result.getOutput();
    }

    /**
     * 判断某个模型运行时是否在线：
     * 无运行时配置返回 unmanaged；有配置但无 health-url 返回 unknown；否则按 health-url 是否 2xx 返回 online/offline。
     */
    private String status(GatewayProperties.RuntimeConfig runtime) {
        if (runtime == null) {
            return "unmanaged";
        }
        if (runtime.getHealthUrl() == null || runtime.getHealthUrl().trim().length() == 0) {
            return "unknown";
        }
        return healthChecker.isOnline(runtime.getHealthUrl()) ? "online" : "offline";
    }

    /** 查找某模型的运行时配置，未配置则抛异常（该模型不可通过 admin 接口启停）。 */
    private GatewayProperties.RuntimeConfig requireRuntime(String model) {
        GatewayProperties.RuntimeConfig runtime = properties.getRuntimes().get(model);
        if (runtime == null) {
            throw new IllegalArgumentException("Runtime is not configured for model: " + model);
        }
        return runtime;
    }
}
