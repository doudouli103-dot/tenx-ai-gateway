package com.tenx.ai.gateway.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tenx.ai.gateway")
public class GatewayProperties {

    private List<String> apiKeys = new ArrayList<String>();
    private Map<String, ProviderConfig> providers = new LinkedHashMap<String, ProviderConfig>();
    private Map<String, RouteConfig> routes = new LinkedHashMap<String, RouteConfig>();
    private DocumentCenterConfig documentCenter = new DocumentCenterConfig();

    public List<String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<String> apiKeys) {
        this.apiKeys = apiKeys;
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

    public DocumentCenterConfig getDocumentCenter() {
        return documentCenter;
    }

    public void setDocumentCenter(DocumentCenterConfig documentCenter) {
        this.documentCenter = documentCenter;
    }

    public static class ProviderConfig {
        private String type;
        private String baseUrl;
        private String apiKey;

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

    public static class DocumentCenterConfig {
        private String baseUrl;
        private String uploadPath = "/api/files";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }
    }
}
