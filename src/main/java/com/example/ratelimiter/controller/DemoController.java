package com.example.ratelimiter.controller;

import com.example.ratelimiter.annotation.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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
