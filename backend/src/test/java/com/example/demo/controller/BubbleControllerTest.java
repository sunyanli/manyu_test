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
class BubbleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bubbleSortNormal() throws Exception {
        mockMvc.perform(post("/api/bubble-sort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"array\": [5, 3, 8, 1, 2]}")
                        .header("X-Caller-Name", "王五")
                        .header("X-Person-Type", "产品")
                        .header("X-Person-Level", "中级")
                        .header("X-Department", "产品部"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originalArray[0]").value(5))
                .andExpect(jsonPath("$.data.sortedArray[0]").value(1))
                .andExpect(jsonPath("$.data.swapCount").isNumber())
                .andExpect(jsonPath("$.data.comparisonCount").isNumber());
    }
}