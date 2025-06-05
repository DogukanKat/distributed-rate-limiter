package com.example.ratelimiter.limiter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private Map<String, RateLimitConfig> roles;

    private Map<String, Map<String, RateLimitConfig>> routes;
}
