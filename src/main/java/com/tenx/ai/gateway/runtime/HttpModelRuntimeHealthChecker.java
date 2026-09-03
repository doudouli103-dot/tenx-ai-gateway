package com.tenx.ai.gateway.runtime;

import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.stereotype.Component;

/**
 * 通过 HTTP GET 判断运行时是否在线：health-url 返回 2xx 视为在线。
 * 连接/读取超时各 1.5 秒，任何异常均视为离线。
 */
@Component
public class HttpModelRuntimeHealthChecker implements ModelRuntimeHealthChecker {

    /** 判断 health-url 对应的运行时是否在线。 */
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
