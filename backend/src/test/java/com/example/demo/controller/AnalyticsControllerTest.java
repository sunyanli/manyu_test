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
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyticsByPersonType() throws Exception {
        mockMvc.perform(get("/api/analytics/summary")
                        .param("dimension", "personType"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dimension").value("personType"))
                .andExpect(jsonPath("$.data.totalCalls").isNumber());
    }

    @Test
    void analyticsByDepartment() throws Exception {
        mockMvc.perform(get("/api/analytics/summary")
                        .param("dimension", "department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dimension").value("department"));
    }
}