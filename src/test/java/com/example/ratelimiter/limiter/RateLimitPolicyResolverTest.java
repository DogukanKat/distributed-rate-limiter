package com.example.ratelimiter.limiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "rate-limiter.roles.basic.max-tokens=2",
        "rate-limiter.roles.basic.refill-interval-ms=60000",
        "rate-limiter.roles.premium.max-tokens=5",
        "rate-limiter.roles.premium.refill-interval-ms=60000",
        "rate-limiter.routes[/api/test].basic.max-tokens=1",
        "rate-limiter.routes[/api/test].basic.refill-interval-ms=60000"
})
class RateLimitPolicyResolverTest {

    @Autowired
    private RateLimitPolicyResolver resolver;

    @Test
    void testRouteSpecificPolicy() {
        RateLimitConfig config = resolver.resolve("basic", "/api/test");
        assertEquals(1, config.getMaxTokens());
        assertEquals(60000, config.getRefillIntervalMs());
    }

    @Test
    void testRoleOnlyPolicy() {
        RateLimitConfig config = resolver.resolve("premium", "/api/other");
        assertEquals(5, config.getMaxTokens());
        assertEquals(60000, config.getRefillIntervalMs());
    }

    @Test
    void testFallbackToBasic() {
        RateLimitConfig config = resolver.resolve("unknown", "/api/unknown");
        assertEquals(2, config.getMaxTokens());
        assertEquals(60000, config.getRefillIntervalMs());
    }
}
