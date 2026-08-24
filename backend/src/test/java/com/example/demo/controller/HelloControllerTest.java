package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloWithName() throws Exception {
        mockMvc.perform(get("/api/hello")
                        .param("name", "张三")
                        .header("X-Caller-Name", "张三")
                        .header("X-Person-Type", "研发")
                        .header("X-Person-Level", "高级")
                        .header("X-Department", "技术部"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.greeting").value("Hello, 张三! Welcome to DTCoder Demo."));
    }

    @Test
    void helloDefault() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.greeting").value("Hello, World! Welcome to DTCoder Demo."));
    }
}