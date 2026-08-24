package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hashSha256() throws Exception {
        mockMvc.perform(post("/api/hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": \"hello\", \"algorithm\": \"SHA-256\"}")
                        .header("X-Caller-Name", "李四")
                        .header("X-Person-Type", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.data.hash").value("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
    }

    @Test
    void hashMd5() throws Exception {
        mockMvc.perform(post("/api/hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": \"hello\", \"algorithm\": \"MD5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.algorithm").value("MD5"));
    }
}