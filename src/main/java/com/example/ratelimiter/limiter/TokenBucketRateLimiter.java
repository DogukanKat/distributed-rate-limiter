package com.example.ratelimiter.limiter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> tokenBucketScript;

    private final long MAX_TOKENS = 10;
    private final long REFILL_INTERVAL_MS = 60000;
    private final long REFILL_AMOUNT = 10;

    public RateLimitResult consume(String userId) {
        String tokenKey = "rate:bucket:" + userId;
        String lastRefillKey = tokenKey + ":lastRefill";
        long now = System.currentTimeMillis();

        List<String> keys = Arrays.asList(tokenKey, lastRefillKey);

        List<String> args = Arrays.asList(
                String.valueOf(now),
                String.valueOf(REFILL_INTERVAL_MS),
                String.valueOf(MAX_TOKENS),
                String.valueOf(REFILL_AMOUNT)
        );

        @SuppressWarnings("unchecked")
        List<Long> result = (List<Long>) redis.execute(tokenBucketScript, keys, args.toArray(new String[0]));

        if (result == null || result.size() < 2) {
            return new RateLimitResult(false, 0);
        }

        boolean allowed = result.get(0) == 1L;
        long tokensLeft = result.get(1);

        return new RateLimitResult(allowed, tokensLeft);
    }
}
