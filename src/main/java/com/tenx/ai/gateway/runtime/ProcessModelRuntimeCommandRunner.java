package com.tenx.ai.gateway.runtime;

import com.tenx.ai.gateway.config.GatewayProperties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * 通过本机 shell 执行运行时命令（start/stop 脚本）的命令执行器。
 *
 * <p>使用配置的 shell（默认 /bin/sh，兼容容器）以 {@code -lc} 方式执行完整命令字符串。
 * 执行有超时限制，超时后强制终止进程；命令输出截断到 8000 字符，避免撑爆响应。
 */
@Component
public class ProcessModelRuntimeCommandRunner implements ModelRuntimeCommandRunner {

    /** 执行命令用的 shell（由配置注入，默认 /bin/sh）。 */
    private final String commandShell;

    /** 构造命令执行器，从配置读取 shell。 */
    public ProcessModelRuntimeCommandRunner(GatewayProperties properties) {
        this.commandShell = resolveShell(properties.getAdmin().getCommandShell());
    }

    /** 未配置或为空时回落到 /bin/sh，保证容器等无 zsh 的环境也能执行。 */
    private static String resolveShell(String configuredShell) {
        if (configuredShell == null || configuredShell.trim().length() == 0) {
            return "/bin/sh";
        }
        return configuredShell.trim();
    }

    /** 执行命令，超时强制终止，返回执行结果。 */
    @Override
    public CommandResult run(String command, long timeoutMillis) {
        if (command == null || command.trim().length() == 0) {
            throw new IllegalArgumentException("Runtime command is not configured");
        }
        Process process = null;
        try {
            process = new ProcessBuilder(commandShell, "-lc", command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return CommandResult.failure("Command timed out", -1, "");
            }
            String output = readOutput(process);
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                CommandResult result = CommandResult.success("Command executed");
                result.setOutput(output);
                return result;
            }
            return CommandResult.failure("Command failed", exitCode, output);
        } catch (Exception exception) {
            return CommandResult.failure(exception.getMessage(), -1, "");
        }
    }

    /** 读取命令输出，最多保留 8000 字符（防止超长输出撑爆响应）。 */
    private String readOutput(Process process) throws Exception {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null && output.length() < 8000) {
            output.append(line).append('\n');
        }
        return output.toString();
    }
}
