package com.example.ratelimiter.controller;

import com.example.ratelimiter.limiter.RateLimitResult;
import com.example.ratelimiter.limiter.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoControllerUnitTest {

    @Mock
    private TokenBucketRateLimiter rateLimiter;

    @InjectMocks
    private DemoController demoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(demoController).build();
    }

    @Test
    void whenAllowed_thenReturns200AndRemainingHeader() throws Exception {
        // RateLimiter mock ayar
        when(rateLimiter.consume(eq("user1"), eq("basic"), eq("/api/request")))
                .thenReturn(new RateLimitResult(true, 7));

        mockMvc.perform(post("/api/request")
                        .header("X-User-Id", "user1")
                        .header("X-User-Role", "basic")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "7"))
                .andExpect(content().string("Allowed! Tokens left: 7"));

        verify(rateLimiter, times(1)).consume("user1", "basic", "/api/request");
    }

    @Test
    void whenBlocked_thenReturns429() throws Exception {
        when(rateLimiter.consume(eq("user2"), eq("basic"), eq("/api/request")))
                .thenReturn(new RateLimitResult(false, 0));

        mockMvc.perform(post("/api/request")
                        .header("X-User-Id", "user2")
                        .header("X-User-Role", "basic")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(content().string("Too Many Requests"));

        verify(rateLimiter, times(1)).consume("user2", "basic", "/api/request");
    }

    @Test
    void whenMissingHeaders_thenReturns400() throws Exception {
        // X-User-Id veya X-User-Role header'ı eksik olduğunda Spring otomatik 400 verir
        mockMvc.perform(post("/api/request"))
                .andExpect(status().isBadRequest());
    }
}
