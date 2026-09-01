package com.tenx.ai.gateway.runtime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ProcessModelRuntimeCommandRunner implements ModelRuntimeCommandRunner {

    @Override
    public CommandResult run(String command, long timeoutMillis) {
        if (command == null || command.trim().length() == 0) {
            throw new IllegalArgumentException("Runtime command is not configured");
        }
        Process process = null;
        try {
            process = new ProcessBuilder("/bin/zsh", "-lc", command)
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
