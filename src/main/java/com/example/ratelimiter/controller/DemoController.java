package com.example.ratelimiter.controller;

import com.example.ratelimiter.limiter.RateLimitResult;
import com.example.ratelimiter.limiter.TokenBucketRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DemoController {

    private final TokenBucketRateLimiter rateLimiter;

    @PostMapping("/request")
    public ResponseEntity<String> handleRequest(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            HttpServletRequest request) {                 // ← jakarta.servlet.http.HttpServletRequest

        String route = request.getRequestURI();
        RateLimitResult result = rateLimiter.consume(userId, userRole, route);

        if (!result.allowed()) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .body("Too Many Requests");
        }

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(result.tokensLeft()))
                .body("Allowed! Tokens left: " + result.tokensLeft());
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> handleCheckout(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            HttpServletRequest request) {

        String route = request.getRequestURI();
        RateLimitResult result = rateLimiter.consume(userId, userRole, route);

        if (!result.allowed()) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .body("Too Many Requests");
        }

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(result.tokensLeft()))
                .body("Checkout Allowed! Tokens left: " + result.tokensLeft());
    }
}
