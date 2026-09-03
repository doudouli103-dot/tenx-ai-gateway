package com.tenx.ai.gateway.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关的全局配置绑定对象，对应 application.yml 中的 {@code tenx.ai.gateway} 前缀。
 *
 * <p>所有配置都支持通过环境变量覆盖，环境变量名统一使用 {@code TENX_AI_GATEWAY_*} 前缀。
 * 配置分为五块：
 * <ul>
 *   <li>{@code api-keys}：网关自身鉴权用的 API Key 白名单（调用方需携带其中任意一个）。</li>
 *   <li>{@code admin}：管理员运行时控制相关配置（命令执行超时、shell、CORS 来源）。</li>
 *   <li>{@code http}：全局 HTTP 客户端调优参数（连接池、各类超时、内存上限）。</li>
 *   <li>{@code providers}：上游服务定义（llama.cpp / image-adapter / video-adapter / 云端）。</li>
 *   <li>{@code routes}：请求模型名到上游服务的路由映射。</li>
 *   <li>{@code runtimes}：每个模型运行时的健康检查与启停命令（供 admin 接口使用）。</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "tenx.ai.gateway")
public class GatewayProperties {

    /** 允许访问网关的 API Key 白名单。 */
    private List<String> apiKeys = new ArrayList<String>();

    /** 管理员运行时控制配置。 */
    private AdminConfig admin = new AdminConfig();

    /** 全局 HTTP 客户端调优参数。 */
    private HttpConfig http = new HttpConfig();

    /** 上游服务（provider）定义，key 为 provider 名。 */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<String, ProviderConfig>();

    /** 模型路由定义，key 为调用方使用的模型名。 */
    private Map<String, RouteConfig> routes = new LinkedHashMap<String, RouteConfig>();

    /** 模型运行时控制定义，key 为模型名。 */
    private Map<String, RuntimeConfig> runtimes = new LinkedHashMap<String, RuntimeConfig>();

    /** 返回 API Key 白名单。 */
    public List<String> getApiKeys() {
        return apiKeys;
    }

    /** 设置 API Key 白名单。 */
    public void setApiKeys(List<String> apiKeys) {
        this.apiKeys = apiKeys;
    }

    /** 返回管理员运行时控制配置。 */
    public AdminConfig getAdmin() {
        return admin;
    }

    /** 设置管理员运行时控制配置。 */
    public void setAdmin(AdminConfig admin) {
        this.admin = admin;
    }

    /** 返回全局 HTTP 客户端调优参数。 */
    public HttpConfig getHttp() {
        return http;
    }

    /** 设置全局 HTTP 客户端调优参数。 */
    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    /** 返回上游服务定义。 */
    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    /** 设置上游服务定义。 */
    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }

    /** 返回模型路由定义。 */
    public Map<String, RouteConfig> getRoutes() {
        return routes;
    }

    /** 设置模型路由定义。 */
    public void setRoutes(Map<String, RouteConfig> routes) {
        this.routes = routes;
    }

    /** 返回模型运行时控制定义。 */
    public Map<String, RuntimeConfig> getRuntimes() {
        return runtimes;
    }

    /** 设置模型运行时控制定义。 */
    public void setRuntimes(Map<String, RuntimeConfig> runtimes) {
        this.runtimes = runtimes;
    }

    /**
     * 管理员运行时控制配置。{@code /admin/models/*} 接口用于查看模型运行时状态，
     * 以及手动执行配置好的 start/stop 脚本。
     */
    public static class AdminConfig {
        /** 是否启用 admin 运行时控制功能。 */
        private boolean enabled = true;

        /** 单条 start/stop 命令最多允许执行多久（毫秒），超时会被强杀。 */
        private long commandTimeoutMillis = 60000;

        /** 执行命令用的 shell。默认 /bin/sh，兼容 Docker 基础镜像；宿主机若脚本依赖 zsh 语法可覆盖为 /bin/zsh。 */
        private String commandShell = "/bin/sh";

        /** 允许跨域访问 admin 接口的前端来源（对应 admin UI）。 */
        private List<String> corsAllowedOrigins = new ArrayList<String>();

        /** 无参构造，填充默认的 CORS 来源。 */
        public AdminConfig() {
            corsAllowedOrigins.add("http://127.0.0.1:5173");
            corsAllowedOrigins.add("http://localhost:5173");
            corsAllowedOrigins.add("http://macstudio.tentest.cn:5173");
            corsAllowedOrigins.add("http://192.168.1.102:5173");
        }

        /** 返回是否启用 admin 功能。 */
        public boolean isEnabled() {
            return enabled;
        }

        /** 设置是否启用 admin 功能。 */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /** 返回命令执行超时（毫秒）。 */
        public long getCommandTimeoutMillis() {
            return commandTimeoutMillis;
        }

        /** 设置命令执行超时（毫秒）。 */
        public void setCommandTimeoutMillis(long commandTimeoutMillis) {
            this.commandTimeoutMillis = commandTimeoutMillis;
        }

        /** 返回命令执行用的 shell。 */
        public String getCommandShell() {
            return commandShell;
        }

        /** 设置命令执行用的 shell。 */
        public void setCommandShell(String commandShell) {
            this.commandShell = commandShell;
        }

        /** 返回允许跨域的来源列表。 */
        public List<String> getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        /** 设置允许跨域的来源列表。 */
        public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }
    }

    /**
     * 上游服务（provider）定义。路由（{@link RouteConfig}）通过 provider 名字引用这里。
     * <ul>
     *   <li>{@code type}：决定用哪个 provider 实现（如 openai-compatible / openai-video-compatible）。</li>
     *   <li>{@code baseUrl}：上游服务的根地址。</li>
     *   <li>{@code apiKey}：访问上游服务时附加的鉴权 key（可为空）。</li>
     *   <li>{@code responseTimeoutMillis} / {@code readTimeoutMillis}：可选，仅针对该上游的超时覆盖；
     *       未设置时回落到全局 {@link HttpConfig} 的默认值。</li>
     * </ul>
     */
    public static class ProviderConfig {
        /** 上游服务类型，决定使用哪个 provider 实现。 */
        private String type;

        /** 上游服务根地址。 */
        private String baseUrl;

        /** 访问上游服务用的 API Key（可为空）。 */
        private String apiKey;

        /** 该上游的整体响应超时（毫秒，可选，覆盖全局默认）。 */
        private Integer responseTimeoutMillis;

        /** 该上游的读空闲超时（毫秒，可选，覆盖全局默认）。 */
        private Integer readTimeoutMillis;

        /** 返回上游服务类型。 */
        public String getType() {
            return type;
        }

        /** 设置上游服务类型。 */
        public void setType(String type) {
            this.type = type;
        }

        /** 返回上游服务根地址。 */
        public String getBaseUrl() {
            return baseUrl;
        }

        /** 设置上游服务根地址。 */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /** 返回上游 API Key。 */
        public String getApiKey() {
            return apiKey;
        }

        /** 设置上游 API Key。 */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /** 返回该上游的整体响应超时（毫秒）。 */
        public Integer getResponseTimeoutMillis() {
            return responseTimeoutMillis;
        }

        /** 设置该上游的整体响应超时（毫秒）。 */
        public void setResponseTimeoutMillis(Integer responseTimeoutMillis) {
            this.responseTimeoutMillis = responseTimeoutMillis;
        }

        /** 返回该上游的读空闲超时（毫秒）。 */
        public Integer getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        /** 设置该上游的读空闲超时（毫秒）。 */
        public void setReadTimeoutMillis(Integer readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }
    }

    /**
     * 全局 HTTP 客户端调优参数，作用于所有 provider 的 WebClient（连接池 + 默认超时 + 内存上限）。
     * 单条 provider 可以通过 {@link ProviderConfig} 覆盖其中的 response/read 超时。
     */
    public static class HttpConfig {
        /** 连接池最大连接数。 */
        private int maxConnections = 200;

        /** 连接池拿不到连接时的等待上限（毫秒）。 */
        private long pendingAcquireTimeoutMillis = 30000;

        /** 建立 TCP 连接的超时（毫秒）。 */
        private int connectTimeoutMillis = 5000;

        /** 从请求发出到收到完整响应的整体超时（毫秒）。 */
        private long responseTimeoutMillis = 120000;

        /** 单次读取的空闲超时（毫秒）。 */
        private long readTimeoutMillis = 120000;

        /** 单次写入的空闲超时（毫秒）。 */
        private long writeTimeoutMillis = 120000;

        /** 响应体解码时允许的最大内存占用（字节）。base64 图像体积大，默认给到 16MB。 */
        private int maxInMemorySizeBytes = 16777216;

        /** 返回连接池最大连接数。 */
        public int getMaxConnections() {
            return maxConnections;
        }

        /** 设置连接池最大连接数。 */
        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        /** 返回连接池等待上限（毫秒）。 */
        public long getPendingAcquireTimeoutMillis() {
            return pendingAcquireTimeoutMillis;
        }

        /** 设置连接池等待上限（毫秒）。 */
        public void setPendingAcquireTimeoutMillis(long pendingAcquireTimeoutMillis) {
            this.pendingAcquireTimeoutMillis = pendingAcquireTimeoutMillis;
        }

        /** 返回连接超时（毫秒）。 */
        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        /** 设置连接超时（毫秒）。 */
        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        /** 返回整体响应超时（毫秒）。 */
        public long getResponseTimeoutMillis() {
            return responseTimeoutMillis;
        }

        /** 设置整体响应超时（毫秒）。 */
        public void setResponseTimeoutMillis(long responseTimeoutMillis) {
            this.responseTimeoutMillis = responseTimeoutMillis;
        }

        /** 返回读空闲超时（毫秒）。 */
        public long getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        /** 设置读空闲超时（毫秒）。 */
        public void setReadTimeoutMillis(long readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        /** 返回写空闲超时（毫秒）。 */
        public long getWriteTimeoutMillis() {
            return writeTimeoutMillis;
        }

        /** 设置写空闲超时（毫秒）。 */
        public void setWriteTimeoutMillis(long writeTimeoutMillis) {
            this.writeTimeoutMillis = writeTimeoutMillis;
        }

        /** 返回响应体最大内存占用（字节）。 */
        public int getMaxInMemorySizeBytes() {
            return maxInMemorySizeBytes;
        }

        /** 设置响应体最大内存占用（字节）。 */
        public void setMaxInMemorySizeBytes(int maxInMemorySizeBytes) {
            this.maxInMemorySizeBytes = maxInMemorySizeBytes;
        }
    }

    /**
     * 请求模型名到上游服务的路由。Map 的 key 是调用方在请求里填的模型名（如 {@code qwen3-coder-next}），
     * value 描述该模型的能力、对应 provider、真实模型名、fallback 等。
     */
    public static class RouteConfig {
        /** 能力类型：chat / image / video，决定命中哪个 Controller 和 provider 接口。 */
        private String capability = "chat";

        /** 主 provider 名，对应 {@code providers} 的 key。 */
        private String provider;

        /** 转发给上游时使用的真实模型名（可能和 key 不同）。 */
        private String model;

        /** 主 provider 失败时的备用 provider 名（可选）。 */
        private String fallbackProvider;

        /** 备用 provider 使用的真实模型名（可选）。 */
        private String fallbackModel;

        /** 视频默认时长（秒），请求未传 duration 时使用。 */
        private Integer defaultDurationSeconds;

        /** 视频允许的最大时长（秒），超过会拒绝请求。 */
        private Integer maxDurationSeconds;

        /** 返回能力类型。 */
        public String getCapability() {
            return capability;
        }

        /** 设置能力类型。 */
        public void setCapability(String capability) {
            this.capability = capability;
        }

        /** 返回主 provider 名。 */
        public String getProvider() {
            return provider;
        }

        /** 设置主 provider 名。 */
        public void setProvider(String provider) {
            this.provider = provider;
        }

        /** 返回转发用的真实模型名。 */
        public String getModel() {
            return model;
        }

        /** 设置转发用的真实模型名。 */
        public void setModel(String model) {
            this.model = model;
        }

        /** 返回备用 provider 名。 */
        public String getFallbackProvider() {
            return fallbackProvider;
        }

        /** 设置备用 provider 名。 */
        public void setFallbackProvider(String fallbackProvider) {
            this.fallbackProvider = fallbackProvider;
        }

        /** 返回备用 provider 使用的真实模型名。 */
        public String getFallbackModel() {
            return fallbackModel;
        }

        /** 设置备用 provider 使用的真实模型名。 */
        public void setFallbackModel(String fallbackModel) {
            this.fallbackModel = fallbackModel;
        }

        /** 返回视频默认时长（秒）。 */
        public Integer getDefaultDurationSeconds() {
            return defaultDurationSeconds;
        }

        /** 设置视频默认时长（秒）。 */
        public void setDefaultDurationSeconds(Integer defaultDurationSeconds) {
            this.defaultDurationSeconds = defaultDurationSeconds;
        }

        /** 返回视频最大时长（秒）。 */
        public Integer getMaxDurationSeconds() {
            return maxDurationSeconds;
        }

        /** 设置视频最大时长（秒）。 */
        public void setMaxDurationSeconds(Integer maxDurationSeconds) {
            this.maxDurationSeconds = maxDurationSeconds;
        }
    }

    /**
     * 模型运行时控制配置，供 admin 接口使用。网关本身不启动模型运行时，
     * 只负责执行这里配置好的命令，并通过 health-url 判断运行时是否在线。
     */
    public static class RuntimeConfig {
        /** 健康检查地址，返回 2xx 视为在线。 */
        private String healthUrl;

        /** 启动模型运行时的命令（脚本）。 */
        private String startCommand;

        /** 停止模型运行时的命令（脚本）。 */
        private String stopCommand;

        /** 可选：操作后用于查看模型进程/内存的命令。 */
        private String resourceCheckCommand;

        /** 返回健康检查地址。 */
        public String getHealthUrl() {
            return healthUrl;
        }

        /** 设置健康检查地址。 */
        public void setHealthUrl(String healthUrl) {
            this.healthUrl = healthUrl;
        }

        /** 返回启动命令。 */
        public String getStartCommand() {
            return startCommand;
        }

        /** 设置启动命令。 */
        public void setStartCommand(String startCommand) {
            this.startCommand = startCommand;
        }

        /** 返回停止命令。 */
        public String getStopCommand() {
            return stopCommand;
        }

        /** 设置停止命令。 */
        public void setStopCommand(String stopCommand) {
            this.stopCommand = stopCommand;
        }

        /** 返回资源检查命令。 */
        public String getResourceCheckCommand() {
            return resourceCheckCommand;
        }

        /** 设置资源检查命令。 */
        public void setResourceCheckCommand(String resourceCheckCommand) {
            this.resourceCheckCommand = resourceCheckCommand;
        }
    }

}
