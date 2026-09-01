package com.tenx.ai.gateway.runtime;

public class ModelRuntimeOperationResult {

    private String model;
    private String action;
    private boolean success;
    private String statusBefore;
    private String statusAfter;
    private String expectedStatus;
    private boolean statusVerified;
    private CommandResult command;
    private String resourceCheckOutput;

    public static ModelRuntimeOperationResult from(String model, String action, String statusBefore,
                                                   String statusAfter, CommandResult command,
                                                   String resourceCheckOutput) {
        ModelRuntimeOperationResult result = new ModelRuntimeOperationResult();
        result.setModel(model);
        result.setAction(action);
        result.setStatusBefore(statusBefore);
        result.setStatusAfter(statusAfter);
        result.setExpectedStatus("start".equals(action) ? "online" : "offline");
        result.setStatusVerified(result.getExpectedStatus().equals(statusAfter));
        result.setCommand(command);
        result.setResourceCheckOutput(resourceCheckOutput);
        result.setSuccess((command == null || command.isSuccess()) && result.isStatusVerified());
        return result;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatusBefore() {
        return statusBefore;
    }

    public void setStatusBefore(String statusBefore) {
        this.statusBefore = statusBefore;
    }

    public String getStatusAfter() {
        return statusAfter;
    }

    public void setStatusAfter(String statusAfter) {
        this.statusAfter = statusAfter;
    }

    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public boolean isStatusVerified() {
        return statusVerified;
    }

    public void setStatusVerified(boolean statusVerified) {
        this.statusVerified = statusVerified;
    }

    public CommandResult getCommand() {
        return command;
    }

    public void setCommand(CommandResult command) {
        this.command = command;
    }

    public String getResourceCheckOutput() {
        return resourceCheckOutput;
    }

    public void setResourceCheckOutput(String resourceCheckOutput) {
        this.resourceCheckOutput = resourceCheckOutput;
    }
}
