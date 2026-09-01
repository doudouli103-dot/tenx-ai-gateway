package com.tenx.ai.gateway.runtime;

public class CommandResult {

    private boolean success;
    private String message;
    private int exitCode;
    private String output;

    public static CommandResult success(String message) {
        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setExitCode(0);
        return result;
    }

    public static CommandResult failure(String message, int exitCode, String output) {
        CommandResult result = new CommandResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setExitCode(exitCode);
        result.setOutput(output);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
