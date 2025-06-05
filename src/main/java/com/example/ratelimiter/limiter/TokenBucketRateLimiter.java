package com.example.ratelimiter.limiter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiter {
    private final StringRedisTemplate redis;

    private final long MAX_TOKENS = 10;
    private final long REFILL_INTERVAL_MS = 60000;
    private final long REFILL_AMOUNT = 10;

    public RateLimitResult consume(String userId) {
        String key = "rate:bucket:" + userId;

        redis.opsForValue().setIfAbsent(key, String.valueOf(MAX_TOKENS), REFILL_INTERVAL_MS, TimeUnit.MILLISECONDS);

        Long tokensLeft = redis.opsForValue().decrement(key);

        if (tokensLeft == null || tokensLeft < 0) {
            return new RateLimitResult(false, 0);
        }

        return new RateLimitResult(true, tokensLeft);
    }
}
