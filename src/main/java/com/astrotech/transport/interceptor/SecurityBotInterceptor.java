package com.astrotech.transport.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public class SecurityBotInterceptor implements HandlerInterceptor {
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY_LOGGER");

    private static final List<String> SUSPICIOUS_PATHS = List.of("/wp-admin",
            "/.env", "/phpmyadmin", "/actuator", "/.git",
            "/v2/_catalog", "/api/v1/debug", "/etc/passwd",
            "/config.json");

    private static final Set<String> KNOWN_BAD_USER_AGENTS = Set.of(
            "python-requests", "scrapy", "curl", "wget", "go-http-client",
            "nikto", "sqlmap", "zgrab", "masscan", "censys", "nmap"
    );
    private final Map<String, AtomicInteger> ipRequestCounts = new ConcurrentHashMap<>();
    private static final int RATE_THRESHOLD = 100;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();

        // 1. Detect missing or blank User-Agent
        if (userAgent == null || userAgent.isBlank()) {
            SECURITY_LOGGER.warn("SECURITY_EVENT | TYPE=MISSING_USER_AGENT | IP={} | URI={}", clientIp, uri);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        String lowerUserAgent = userAgent.toLowerCase();


        if (KNOWN_BAD_USER_AGENTS.stream().anyMatch(lowerUserAgent::contains)) {
            SECURITY_LOGGER.warn("SECURITY_EVENT | TYPE=KNOWN_BOT_DETECTED | IP={} | UA={} | URI={}",
                    clientIp, userAgent, uri);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }


        if (SUSPICIOUS_PATHS.stream().anyMatch(uri.toLowerCase()::contains)) {
            SECURITY_LOGGER.error("SECURITY_ALERT | TYPE=VULNERABILITY_PROBE | IP={} | UA={} | URI={}",
                    clientIp, userAgent, uri);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }


        int currentCount = ipRequestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
        if (currentCount > RATE_THRESHOLD) {
            SECURITY_LOGGER.error("SECURITY_ALERT | TYPE=RATE_EXCEEDED_POSSIBLE_DDOS | IP={} | REQUEST_COUNT={}",
                    clientIp, currentCount);
            response.setStatus(429);
            return false;
        }

        return true;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "CF-Connecting-IP",
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {

                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }


    public void resetRateCounters() {
        ipRequestCounts.clear();
    }
}
