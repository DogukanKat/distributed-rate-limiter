package com.example.ratelimiter.limiter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitPolicyResolver {

    private final RateLimiterProperties properties;

    public RateLimitConfig resolve(String role, String route) {
        if (properties.getRoutes() != null && properties.getRoutes().containsKey(route)) {
            var routeMap = properties.getRoutes().get(route);
            if (routeMap != null && routeMap.containsKey(role)) {
                return routeMap.get(role);
            }
        }
        if (properties.getRoles() != null && properties.getRoles().containsKey(role)) {
            return properties.getRoles().get(role);
        }
        return properties.getRoles().getOrDefault("basic", new RateLimitConfig());
    }
}
