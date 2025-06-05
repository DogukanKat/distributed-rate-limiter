package com.example.ratelimiter.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        "spring.redis.host=${testcontainers.redis.host}",
        "spring.redis.port=${testcontainers.redis.port}"
})
@AutoConfigureMockMvc
class DemoControllerIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

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
    void whenRequestUnderLimit_thenReturns200WithHeader() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/request")
                            .header("X-User-Id", "intUser")
                            .header("X-User-Role", "basic"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }
    }

    @Test
    void whenRequestOverLimit_thenReturns429() throws Exception {
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/request")
                            .header("X-User-Id", "intUser")
                            .header("X-User-Role", "basic"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/request")
                        .header("X-User-Id", "intUser")
                        .header("X-User-Role", "basic"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    @Test
    void whenMissingHeaders_thenReturns400() throws Exception {
        mockMvc.perform(post("/api/request"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenCheckoutUnderLimit_thenReturns200() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/checkout")
                            .header("X-User-Id", "intUser")
                            .header("X-User-Role", "premium"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }
    }

    @Test
    void whenCheckoutOverLimit_thenReturns429() throws Exception {
        for (int i = 1; i <= 50; i++) {
            mockMvc.perform(post("/api/checkout")
                            .header("X-User-Id", "intUser")
                            .header("X-User-Role", "premium"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/checkout")
                        .header("X-User-Id", "intUser")
                        .header("X-User-Role", "premium"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }
}
