package com.example.ratelimiter.controller;

import com.example.ratelimiter.limiter.RateLimitResult;
import com.example.ratelimiter.limiter.TokenBucketRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DemoController {

    private final TokenBucketRateLimiter rateLimiter;

    @PostMapping("/request")
    public ResponseEntity<String> handleRequest(@RequestHeader("X-User-Id") String userId) {
        RateLimitResult result = rateLimiter.consume(userId);

        if (!result.allowed()) {
            return ResponseEntity.status(429).body("Too Many Requests");
        }

        return ResponseEntity.ok("Allowed! Tokens left: " + result.tokensLeft());
    }
}
