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
    private final RateLimitPolicyResolver policyResolver;

    public RateLimitResult consume(String userId, String role, String route) {

        RateLimitConfig config = policyResolver.resolve(role, route);
        long maxTokens = config.getMaxTokens();
        long refillInterval = config.getRefillIntervalMs();

        String tokenKey = "rate:bucket:" + userId + ":" + route;
        String lastRefillKey = tokenKey + ":lastRefill";
        long now = System.currentTimeMillis();

        List<String> keys = Arrays.asList(tokenKey, lastRefillKey);

        List<String> args = Arrays.asList(
                String.valueOf(now),
                String.valueOf(refillInterval),
                String.valueOf(maxTokens),
                String.valueOf(maxTokens)
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
