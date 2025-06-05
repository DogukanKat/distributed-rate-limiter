package com.example.ratelimiter.interceptor;

import com.example.ratelimiter.annotation.RateLimited;
import com.example.ratelimiter.limiter.RateLimitResult;
import com.example.ratelimiter.limiter.TokenBucketRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter rateLimiter;

    public RateLimitingInterceptor(TokenBucketRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        RateLimited annotation = method.getAnnotation(RateLimited.class);
        if (annotation == null) {
            return true;
        }

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        if (userId == null || userRole == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing X-User-Id or X-User-Role header");
            return false;
        }

        String route = annotation.route().isEmpty()
                ? request.getRequestURI()
                : annotation.route();

        RateLimitResult result = rateLimiter.consume(userId, userRole, route);

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("X-RateLimit-Remaining", "0");
            response.getWriter().write("Too Many Requests");
            return false;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.tokensLeft()));
        return true;
    }
}
