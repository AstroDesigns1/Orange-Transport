package com.astrotech.transport.controllers;

import com.astrotech.transport.ratelimit.redisRatelimit.Ratelimit;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "for application health checks")
@Slf4j
public class HealthCheckController {
    @GetMapping
    @Ratelimit
    public Map<String, String> healthCheck() {
        log.info("Website is healthy");
        return Map.of(
                "status", "UP",
                "message", "Website is healthy"
        );
    }
}