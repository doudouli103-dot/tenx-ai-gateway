package com.tenx.ai.gateway.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tenx.ai.gateway")
public class GatewayProperties {

    private List<String> apiKeys = new ArrayList<String>();
    private AdminConfig admin = new AdminConfig();
    private HttpConfig http = new HttpConfig();
    private Map<String, ProviderConfig> providers = new LinkedHashMap<String, ProviderConfig>();
    private Map<String, RouteConfig> routes = new LinkedHashMap<String, RouteConfig>();
    private Map<String, RuntimeConfig> runtimes = new LinkedHashMap<String, RuntimeConfig>();

    public List<String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<String> apiKeys) {
        this.apiKeys = apiKeys;
    }

    public AdminConfig getAdmin() {
        return admin;
    }

    public void setAdmin(AdminConfig admin) {
        this.admin = admin;
    }

    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }

    public Map<String, RouteConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, RouteConfig> routes) {
        this.routes = routes;
    }

    public Map<String, RuntimeConfig> getRuntimes() {
        return runtimes;
    }

    public void setRuntimes(Map<String, RuntimeConfig> runtimes) {
        this.runtimes = runtimes;
    }

    public static class AdminConfig {
        private boolean enabled = true;
        private long commandTimeoutMillis = 60000;
        private String commandShell = "/bin/sh";
        private List<String> corsAllowedOrigins = new ArrayList<String>();

        public AdminConfig() {
            corsAllowedOrigins.add("http://127.0.0.1:5173");
            corsAllowedOrigins.add("http://localhost:5173");
            corsAllowedOrigins.add("http://macstudio.tentest.cn:5173");
            corsAllowedOrigins.add("http://192.168.1.102:5173");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getCommandTimeoutMillis() {
            return commandTimeoutMillis;
        }

        public void setCommandTimeoutMillis(long commandTimeoutMillis) {
            this.commandTimeoutMillis = commandTimeoutMillis;
        }

        public String getCommandShell() {
            return commandShell;
        }

        public void setCommandShell(String commandShell) {
            this.commandShell = commandShell;
        }

        public List<String> getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }
    }

    public static class ProviderConfig {
        private String type;
        private String baseUrl;
        private String apiKey;
        private Integer responseTimeoutMillis;
        private Integer readTimeoutMillis;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Integer getResponseTimeoutMillis() {
            return responseTimeoutMillis;
        }

        public void setResponseTimeoutMillis(Integer responseTimeoutMillis) {
            this.responseTimeoutMillis = responseTimeoutMillis;
        }

        public Integer getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(Integer readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }
    }

    public static class HttpConfig {
        private int maxConnections = 200;
        private long pendingAcquireTimeoutMillis = 30000;
        private int connectTimeoutMillis = 5000;
        private long responseTimeoutMillis = 120000;
        private long readTimeoutMillis = 120000;
        private long writeTimeoutMillis = 120000;
        private int maxInMemorySizeBytes = 16777216;

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public long getPendingAcquireTimeoutMillis() {
            return pendingAcquireTimeoutMillis;
        }

        public void setPendingAcquireTimeoutMillis(long pendingAcquireTimeoutMillis) {
            this.pendingAcquireTimeoutMillis = pendingAcquireTimeoutMillis;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public long getResponseTimeoutMillis() {
            return responseTimeoutMillis;
        }

        public void setResponseTimeoutMillis(long responseTimeoutMillis) {
            this.responseTimeoutMillis = responseTimeoutMillis;
        }

        public long getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(long readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        public long getWriteTimeoutMillis() {
            return writeTimeoutMillis;
        }

        public void setWriteTimeoutMillis(long writeTimeoutMillis) {
            this.writeTimeoutMillis = writeTimeoutMillis;
        }

        public int getMaxInMemorySizeBytes() {
            return maxInMemorySizeBytes;
        }

        public void setMaxInMemorySizeBytes(int maxInMemorySizeBytes) {
            this.maxInMemorySizeBytes = maxInMemorySizeBytes;
        }
    }

    public static class RouteConfig {
        private String capability = "chat";
        private String provider;
        private String model;
        private String fallbackProvider;
        private String fallbackModel;
        private Integer defaultDurationSeconds;
        private Integer maxDurationSeconds;

        public String getCapability() {
            return capability;
        }

        public void setCapability(String capability) {
            this.capability = capability;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getFallbackProvider() {
            return fallbackProvider;
        }

        public void setFallbackProvider(String fallbackProvider) {
            this.fallbackProvider = fallbackProvider;
        }

        public String getFallbackModel() {
            return fallbackModel;
        }

        public void setFallbackModel(String fallbackModel) {
            this.fallbackModel = fallbackModel;
        }

        public Integer getDefaultDurationSeconds() {
            return defaultDurationSeconds;
        }

        public void setDefaultDurationSeconds(Integer defaultDurationSeconds) {
            this.defaultDurationSeconds = defaultDurationSeconds;
        }

        public Integer getMaxDurationSeconds() {
            return maxDurationSeconds;
        }

        public void setMaxDurationSeconds(Integer maxDurationSeconds) {
            this.maxDurationSeconds = maxDurationSeconds;
        }
    }

    public static class RuntimeConfig {
        private String healthUrl;
        private String startCommand;
        private String stopCommand;
        private String resourceCheckCommand;

        public String getHealthUrl() {
            return healthUrl;
        }

        public void setHealthUrl(String healthUrl) {
            this.healthUrl = healthUrl;
        }

        public String getStartCommand() {
            return startCommand;
        }

        public void setStartCommand(String startCommand) {
            this.startCommand = startCommand;
        }

        public String getStopCommand() {
            return stopCommand;
        }

        public void setStopCommand(String stopCommand) {
            this.stopCommand = stopCommand;
        }

        public String getResourceCheckCommand() {
            return resourceCheckCommand;
        }

        public void setResourceCheckCommand(String resourceCheckCommand) {
            this.resourceCheckCommand = resourceCheckCommand;
        }
    }

}
