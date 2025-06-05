package com.example.ratelimiter.limiter;

import lombok.Data;

@Data
public class RateLimitConfig {
    private long maxTokens;
    private long refillIntervalMs;
}
