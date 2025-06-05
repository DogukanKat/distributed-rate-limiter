package com.example.ratelimiter.limiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenBucketRateLimiterUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private DefaultRedisScript<List> tokenBucketScript;

    @Mock
    private RateLimitPolicyResolver policyResolver;

    @InjectMocks
    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenScriptReturnsAllowed_thenAllowedIsTrueAndTokensLeftMatches() {
        String userId = "user1";
        String role = "basic";
        String route = "/api/test";

        RateLimitConfig config = new RateLimitConfig();
        config.setMaxTokens(5);
        config.setRefillIntervalMs(60000);
        when(policyResolver.resolve(role, route)).thenReturn(config);

        List<Long> scriptResult = Arrays.asList(1L, 4L);
        when(redisTemplate.execute(eq(tokenBucketScript),
                anyList(),
                any(String[].class)))
                .thenReturn(scriptResult);

        RateLimitResult result = rateLimiter.consume(userId, role, route);

        assertTrue(result.allowed());
        assertEquals(4, result.tokensLeft());

        verify(redisTemplate, times(1))
                .execute(eq(tokenBucketScript),
                        anyList(),
                        any(String[].class));
    }

    @Test
    void whenScriptReturnsBlocked_thenAllowedIsFalseAndTokensLeftZero() {
        String userId = "user2";
        String role = "premium";
        String route = "/api/checkout";

        RateLimitConfig config = new RateLimitConfig();
        config.setMaxTokens(2);
        config.setRefillIntervalMs(60000);
        when(policyResolver.resolve(role, route)).thenReturn(config);

        List<Long> scriptResult = Arrays.asList(0L, 0L);
        when(redisTemplate.execute(eq(tokenBucketScript),
                anyList(),
                any(String[].class)))
                .thenReturn(scriptResult);

        RateLimitResult result = rateLimiter.consume(userId, role, route);

        assertFalse(result.allowed());
        assertEquals(0, result.tokensLeft());
    }

    @Test
    void whenScriptReturnsNull_thenBlocked() {
        String userId = "user3";
        String role = "basic";
        String route = "/api/test";

        RateLimitConfig config = new RateLimitConfig();
        config.setMaxTokens(3);
        config.setRefillIntervalMs(60000);
        when(policyResolver.resolve(role, route)).thenReturn(config);

        when(redisTemplate.execute(eq(tokenBucketScript),
                anyList(),
                any(String[].class)))
                .thenReturn(null);

        RateLimitResult result = rateLimiter.consume(userId, role, route);

        assertFalse(result.allowed());
        assertEquals(0, result.tokensLeft());
    }
}
