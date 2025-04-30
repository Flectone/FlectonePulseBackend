package net.flectone.pulse.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Component
public class HttpUtils {

    private final String IP_API_URL = "http://ip-api.com/line/<ip>?fields=country";

    private final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public String getClientIpAddressIfServletRequestExist() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return "0.0.0.0";
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        for (String header: IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(",")[0];
            }
        }

        return request.getRemoteAddr();
    }

    public String getClientLocationFromIp() {
        return getClientLocationFromIp(getClientIpAddressIfServletRequestExist());
    }

    private String getClientLocationFromIp(String ip) {
        String url = IP_API_URL.replace("<ip>", ip);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader((new URL(url)).openStream()))){
            return reader.readLine();

        } catch (IOException ignored) {}

        return "Unknown";
    }
}