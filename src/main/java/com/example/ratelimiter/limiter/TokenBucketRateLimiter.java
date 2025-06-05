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

    /**
     * @param userId Benzersiz kullanıcı/istemci kimliği
     * @param role   Kullanıcı rolü (basic, premium, admin)
     * @param route  İstek yapılan URI (örneğin: "/api/request")
     * @return RateLimitResult → {allowed, tokensLeft}
     */
    public RateLimitResult consume(String userId, String role, String route) {
        // Önce policyResolver’dan ilgili konfigürasyonu al
        RateLimitConfig config = policyResolver.resolve(role, route);
        long maxTokens = config.getMaxTokens();
        long refillInterval = config.getRefillIntervalMs();

        // Redis anahtarlarını oluştur
        String tokenKey = "rate:bucket:" + userId + ":" + route;
        String lastRefillKey = tokenKey + ":lastRefill";
        long now = System.currentTimeMillis();

        // Lua script’e verilecek KEYS listesi
        List<String> keys = Arrays.asList(tokenKey, lastRefillKey);

        // Lua script’e verilecek ARGV listesi: now, refillInterval, maxTokens, refillAmount (maxTokens kullanıyoruz)
        List<String> args = Arrays.asList(
                String.valueOf(now),
                String.valueOf(refillInterval),
                String.valueOf(maxTokens),
                String.valueOf(maxTokens)
        );

        // Lua script’i çalıştır; ham List dönecek
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
