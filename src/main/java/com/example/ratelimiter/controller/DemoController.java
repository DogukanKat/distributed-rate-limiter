package com.example.ratelimiter.controller;

import com.example.ratelimiter.annotation.RateLimited;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DemoController {

    @RateLimited
    @PostMapping("/request")
    public ResponseEntity<String> handleRequest() {
        return ResponseEntity.ok("Allowed!");
    }

    @RateLimited(route = "/api/checkout")
    @PostMapping("/checkout")
    public ResponseEntity<String> handleCheckout() {
        return ResponseEntity.ok("Checkout Allowed!");
    }
}
