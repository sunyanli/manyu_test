package com.example.algorithm.controller;

import com.example.algorithm.model.dto.ApiResponse;
import com.example.algorithm.model.dto.ExportRequest;
import com.example.algorithm.model.dto.ExportStatusResponse;
import com.example.algorithm.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for export APIs.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/result")
    public ApiResponse<Map<String, String>> createExportTask(@Valid @RequestBody ExportRequest request,
                                                                HttpServletRequest httpRequest) {
        String operatorId = httpRequest.getHeader("X-User-Id");
        String taskId = exportService.createExportTask(request.getAlgorithmType(), request.getParams(), operatorId);
        Map<String, String> data = Map.of("taskId", taskId);
        return ApiResponse.ok(data);
    }

    @GetMapping("/status/{taskId}")
    public ApiResponse<ExportStatusResponse> queryExportStatus(@PathVariable String taskId) {
        ExportStatusResponse status = exportService.queryExportStatus(taskId);
        return ApiResponse.ok(status);
    }
}
