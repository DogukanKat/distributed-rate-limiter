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

/**
 * Entegrasyon testi:
 *  - Resmi Testcontainers GenericContainer ile Redis ayağa kaldırılıyor
 *  - Spring Boot context’i başlatılıyor
 *  - MockMvc ile controller’a HTTP istekleri yollanıyor
 */
@Testcontainers
@SpringBootTest(properties = {
        // Testcontainers’ın ayağa kaldırdığı Redis host/port bilgisi
        "spring.redis.host=${testcontainers.redis.host}",
        "spring.redis.port=${testcontainers.redis.port}"
})
@AutoConfigureMockMvc
class DemoControllerIntegrationTest {

    // GenericContainer kullanarak resmi Redis imajını ayağa kaldırıyoruz
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
        // Oluşturulan konteynerin host ve port bilgisini Spring'e aktarıyoruz
        System.setProperty("testcontainers.redis.host", redisContainer.getHost());
        System.setProperty("testcontainers.redis.port",
                redisContainer.getMappedPort(6379).toString());
    }

    @BeforeEach
    void cleanRedis() {
        // Her test öncesi Redis’i temizliyoruz
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
        // “basic” rolü için maxTokens=10 olduğundan 11. istek 429 dönecek
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
        // “premium” rolü ve /api/checkout için maxTokens=30 olduğundan 31. istek 429 dönecek
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
