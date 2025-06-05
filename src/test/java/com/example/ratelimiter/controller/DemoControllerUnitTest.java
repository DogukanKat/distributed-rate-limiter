package com.example.ratelimiter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoControllerUnitTest {

    private MockMvc mockMvc;

    private DemoController demoController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        demoController = new DemoController();
        mockMvc = MockMvcBuilders.standaloneSetup(demoController).build();
    }

    @Test
    void whenRequestEndpointCalled_thenReturns200WithBody() throws Exception {
        mockMvc.perform(post("/api/request"))
                .andExpect(status().isOk())
                .andExpect(content().string("Allowed!"));
    }

    @Test
    void whenCheckoutEndpointCalled_thenReturns200WithBody() throws Exception {
        mockMvc.perform(post("/api/checkout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Checkout Allowed!"));
    }
}
