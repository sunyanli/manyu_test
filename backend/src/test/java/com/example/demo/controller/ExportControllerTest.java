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
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportHelloCsv() throws Exception {
        mockMvc.perform(get("/api/export")
                        .param("type", "hello")
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hello_export.csv\""))
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"));
    }

    @Test
    void exportHashXlsx() throws Exception {
        mockMvc.perform(get("/api/export")
                        .param("type", "hash")
                        .param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hash_export.xlsx\""))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}