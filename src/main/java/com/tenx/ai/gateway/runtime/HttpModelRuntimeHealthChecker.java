package com.tenx.ai.gateway.runtime;

import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.stereotype.Component;

@Component
public class HttpModelRuntimeHealthChecker implements ModelRuntimeHealthChecker {

    @Override
    public boolean isOnline(String healthUrl) {
        if (healthUrl == null || healthUrl.trim().length() == 0) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(healthUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            int statusCode = connection.getResponseCode();
            return statusCode >= 200 && statusCode < 300;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
