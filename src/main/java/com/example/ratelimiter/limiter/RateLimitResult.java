package com.example.ratelimiter.limiter;

public record RateLimitResult(boolean allowed, long tokensLeft) {}