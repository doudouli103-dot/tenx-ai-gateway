package com.tenx.ai.gateway;

import com.tenx.ai.gateway.config.GatewayProperties;
import com.tenx.ai.gateway.runtime.CommandResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeCommandRunner;
import com.tenx.ai.gateway.runtime.ModelRuntimeHealthChecker;
import com.tenx.ai.gateway.runtime.ModelRuntimeOperationResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeService;
import com.tenx.ai.gateway.runtime.ModelRuntimeStatus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModelRuntimeServiceTest {

    @Test
    public void listsConfiguredRoutesWithRuntimeStatus() {
        FakeHealthChecker healthChecker = new FakeHealthChecker();
        healthChecker.online = true;
        ModelRuntimeService service = new ModelRuntimeService(properties(), healthChecker, new FakeCommandRunner());

        List<ModelRuntimeStatus> models = service.listModels();

        Assertions.assertEquals(2, models.size());
        Assertions.assertEquals("qwen-small", models.get(0).getId());
        Assertions.assertEquals("chat", models.get(0).getCapability());
        Assertions.assertEquals("local-compatible", models.get(0).getProvider());
        Assertions.assertEquals("qwen-small", models.get(0).getTargetModel());
        Assertions.assertEquals("online", models.get(0).getStatus());
        Assertions.assertTrue(models.get(0).isRuntimeConfigured());

        Assertions.assertEquals("gpt-5", models.get(1).getId());
        Assertions.assertEquals("unmanaged", models.get(1).getStatus());
        Assertions.assertFalse(models.get(1).isRuntimeConfigured());
    }

    @Test
    public void startExecutesOnlyConfiguredStartCommand() {
        FakeHealthChecker healthChecker = new FakeHealthChecker();
        healthChecker.statuses.add(Boolean.FALSE);
        healthChecker.statuses.add(Boolean.TRUE);
        FakeCommandRunner commandRunner = new FakeCommandRunner();
        ModelRuntimeService service = new ModelRuntimeService(properties(), healthChecker, commandRunner);

        ModelRuntimeOperationResult result = service.start("qwen-small");

        Assertions.assertEquals("/Users/lijunwei/ai-scripts/start-qwen-small.sh", commandRunner.commands.get(0));
        Assertions.assertEquals("offline", result.getStatusBefore());
        Assertions.assertEquals("online", result.getStatusAfter());
        Assertions.assertTrue(result.isStatusVerified());
        Assertions.assertTrue(result.isSuccess());
    }

    @Test
    public void stopExecutesOnlyConfiguredStopCommand() {
        FakeHealthChecker healthChecker = new FakeHealthChecker();
        healthChecker.statuses.add(Boolean.TRUE);
        healthChecker.statuses.add(Boolean.FALSE);
        FakeCommandRunner commandRunner = new FakeCommandRunner();
        ModelRuntimeService service = new ModelRuntimeService(properties(), healthChecker, commandRunner);

        ModelRuntimeOperationResult result = service.stop("qwen-small");

        Assertions.assertEquals("/Users/lijunwei/ai-scripts/stop-chat-model.sh", commandRunner.commands.get(0));
        Assertions.assertEquals("online", result.getStatusBefore());
        Assertions.assertEquals("offline", result.getStatusAfter());
        Assertions.assertTrue(result.isStatusVerified());
        Assertions.assertTrue(result.isSuccess());
    }

    @Test
    public void operationIncludesResourceCheckOutputWhenConfigured() {
        FakeHealthChecker healthChecker = new FakeHealthChecker();
        healthChecker.statuses.add(Boolean.TRUE);
        healthChecker.statuses.add(Boolean.FALSE);
        FakeCommandRunner commandRunner = new FakeCommandRunner();
        ModelRuntimeService service = new ModelRuntimeService(properties(), healthChecker, commandRunner);

        ModelRuntimeOperationResult result = service.stop("qwen-small");

        Assertions.assertEquals("/Users/lijunwei/ai-scripts/check-chat-model-resource.sh", commandRunner.commands.get(1));
        Assertions.assertEquals("rss=0", result.getResourceCheckOutput());
    }

    @Test
    public void rejectsModelWithoutRuntimeConfiguration() {
        ModelRuntimeService service = new ModelRuntimeService(properties(), new FakeHealthChecker(), new FakeCommandRunner());

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.start("gpt-5")
        );

        Assertions.assertTrue(exception.getMessage().contains("Runtime is not configured"));
    }

    private GatewayProperties properties() {
        GatewayProperties properties = new GatewayProperties();
        properties.getAdmin().setCommandTimeoutMillis(1234);

        GatewayProperties.ProviderConfig local = new GatewayProperties.ProviderConfig();
        local.setType("openai-compatible");
        properties.getProviders().put("local-compatible", local);

        GatewayProperties.ProviderConfig cloud = new GatewayProperties.ProviderConfig();
        cloud.setType("openai-compatible");
        properties.getProviders().put("cloud-openai", cloud);

        GatewayProperties.RouteConfig qwenSmall = new GatewayProperties.RouteConfig();
        qwenSmall.setCapability("chat");
        qwenSmall.setProvider("local-compatible");
        qwenSmall.setModel("qwen-small");
        properties.getRoutes().put("qwen-small", qwenSmall);

        GatewayProperties.RouteConfig gpt5 = new GatewayProperties.RouteConfig();
        gpt5.setCapability("chat");
        gpt5.setProvider("cloud-openai");
        gpt5.setModel("gpt-5");
        properties.getRoutes().put("gpt-5", gpt5);

        GatewayProperties.RuntimeConfig runtime = new GatewayProperties.RuntimeConfig();
        runtime.setHealthUrl("http://127.0.0.1:4000/health");
        runtime.setStartCommand("/Users/lijunwei/ai-scripts/start-qwen-small.sh");
        runtime.setStopCommand("/Users/lijunwei/ai-scripts/stop-chat-model.sh");
        runtime.setResourceCheckCommand("/Users/lijunwei/ai-scripts/check-chat-model-resource.sh");
        properties.getRuntimes().put("qwen-small", runtime);

        return properties;
    }

    private static class FakeHealthChecker implements ModelRuntimeHealthChecker {
        private boolean online;
        private List<Boolean> statuses = new ArrayList<Boolean>();

        @Override
        public boolean isOnline(String healthUrl) {
            if (!statuses.isEmpty()) {
                return statuses.remove(0);
            }
            return online;
        }
    }

    private static class FakeCommandRunner implements ModelRuntimeCommandRunner {
        private List<String> commands = new ArrayList<String>();

        @Override
        public CommandResult run(String command, long timeoutMillis) {
            this.commands.add(command);
            CommandResult result = CommandResult.success("executed");
            if (command.contains("check")) {
                result.setOutput("rss=0");
            }
            return result;
        }
    }
}
