package com.tenx.ai.gateway.runtime;

/**
 * 命令执行结果：是否成功、消息、退出码、输出内容。
 */
public class CommandResult {

    /** 命令是否执行成功（退出码为 0）。 */
    private boolean success;

    /** 结果描述消息。 */
    private String message;

    /** 进程退出码。 */
    private int exitCode;

    /** 命令输出内容（截断后）。 */
    private String output;

    /** 构造一个成功结果。 */
    public static CommandResult success(String message) {
        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setExitCode(0);
        return result;
    }

    /** 构造一个失败结果。 */
    public static CommandResult failure(String message, int exitCode, String output) {
        CommandResult result = new CommandResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setExitCode(exitCode);
        result.setOutput(output);
        return result;
    }

    /** 返回是否成功。 */
    public boolean isSuccess() {
        return success;
    }

    /** 设置是否成功。 */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /** 返回结果消息。 */
    public String getMessage() {
        return message;
    }

    /** 设置结果消息。 */
    public void setMessage(String message) {
        this.message = message;
    }

    /** 返回退出码。 */
    public int getExitCode() {
        return exitCode;
    }

    /** 设置退出码。 */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    /** 返回命令输出。 */
    public String getOutput() {
        return output;
    }

    /** 设置命令输出。 */
    public void setOutput(String output) {
        this.output = output;
    }
}
