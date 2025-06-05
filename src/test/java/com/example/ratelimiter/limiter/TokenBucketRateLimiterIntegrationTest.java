package com.example.ratelimiter.limiter;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "spring.redis.host=${testcontainers.redis.host}",
        "spring.redis.port=${testcontainers.redis.port}"
})
class TokenBucketRateLimiterIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    @Autowired
    private TokenBucketRateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeAll
    static void setUpAll() {
        System.setProperty("testcontainers.redis.host", redisContainer.getHost());
        System.setProperty("testcontainers.redis.port",
                redisContainer.getMappedPort(6379).toString());
    }

    @BeforeEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void whenUnderLimit_multipleAllowed() {
        for (int i = 0; i < 5; i++) {
            RateLimitResult res = rateLimiter.consume("userA", "basic", "/api/test");
            assertTrue(res.allowed());
        }
    }

    @Test
    void whenOverLimit_thenBlocked() {
        for (int i = 0; i < 10; i++) {
            RateLimitResult res = rateLimiter.consume("userA", "basic", "/api/test");
            assertTrue(res.allowed());
        }
        RateLimitResult blocked = rateLimiter.consume("userA", "basic", "/api/test");
        assertFalse(blocked.allowed());
        assertEquals(0, blocked.tokensLeft());
    }

    @Test
    void whenRefillAfterInterval_thenAllowedAgain() throws InterruptedException {
        // token’ları tüket
        for (int i = 0; i < 10; i++) {
            rateLimiter.consume("userA", "basic", "/api/test");
        }
        RateLimitResult blocked = rateLimiter.consume("userA", "basic", "/api/test");
        assertFalse(blocked.allowed());

        // 60 saniyelik refill bekle (REFILL_INTERVAL_MS = 60000)
        Thread.sleep(60000 + 100);
        RateLimitResult afterRefill = rateLimiter.consume("userA", "basic", "/api/test");
        assertTrue(afterRefill.allowed());
    }
}
