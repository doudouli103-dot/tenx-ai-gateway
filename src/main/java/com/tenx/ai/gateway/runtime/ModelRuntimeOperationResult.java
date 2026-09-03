package com.tenx.ai.gateway.runtime;

/**
 * 一次 start/stop 操作的结果：包含操作前后状态、命令结果、资源检查输出，以及整体是否成功。
 *
 * <p>整体 success 的判定：命令执行成功，且操作后的状态达到了预期
 * （start 预期 online，stop 预期 offline）。
 */
public class ModelRuntimeOperationResult {

    /** 被操作的模型名。 */
    private String model;

    /** 操作类型：start 或 stop。 */
    private String action;

    /** 整体是否成功（命令成功且状态验证通过）。 */
    private boolean success;

    /** 操作前的状态。 */
    private String statusBefore;

    /** 操作后的状态。 */
    private String statusAfter;

    /** 操作后期望达到的状态。 */
    private String expectedStatus;

    /** 操作后状态是否符合预期。 */
    private boolean statusVerified;

    /** 命令执行结果。 */
    private CommandResult command;

    /** 资源检查命令的输出（可为空）。 */
    private String resourceCheckOutput;

    /** 根据各组成要素构造并计算整体结果。 */
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

    /** 返回模型名。 */
    public String getModel() {
        return model;
    }

    /** 设置模型名。 */
    public void setModel(String model) {
        this.model = model;
    }

    /** 返回操作类型。 */
    public String getAction() {
        return action;
    }

    /** 设置操作类型。 */
    public void setAction(String action) {
        this.action = action;
    }

    /** 返回整体是否成功。 */
    public boolean isSuccess() {
        return success;
    }

    /** 设置整体是否成功。 */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /** 返回操作前状态。 */
    public String getStatusBefore() {
        return statusBefore;
    }

    /** 设置操作前状态。 */
    public void setStatusBefore(String statusBefore) {
        this.statusBefore = statusBefore;
    }

    /** 返回操作后状态。 */
    public String getStatusAfter() {
        return statusAfter;
    }

    /** 设置操作后状态。 */
    public void setStatusAfter(String statusAfter) {
        this.statusAfter = statusAfter;
    }

    /** 返回期望状态。 */
    public String getExpectedStatus() {
        return expectedStatus;
    }

    /** 设置期望状态。 */
    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    /** 返回状态是否验证通过。 */
    public boolean isStatusVerified() {
        return statusVerified;
    }

    /** 设置状态是否验证通过。 */
    public void setStatusVerified(boolean statusVerified) {
        this.statusVerified = statusVerified;
    }

    /** 返回命令执行结果。 */
    public CommandResult getCommand() {
        return command;
    }

    /** 设置命令执行结果。 */
    public void setCommand(CommandResult command) {
        this.command = command;
    }

    /** 返回资源检查输出。 */
    public String getResourceCheckOutput() {
        return resourceCheckOutput;
    }

    /** 设置资源检查输出。 */
    public void setResourceCheckOutput(String resourceCheckOutput) {
        this.resourceCheckOutput = resourceCheckOutput;
    }
}
