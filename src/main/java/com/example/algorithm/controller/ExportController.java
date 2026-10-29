package com.example.algorithm.controller;

import com.example.algorithm.model.dto.ApiResponse;
import com.example.algorithm.model.dto.ExportRequest;
import com.example.algorithm.model.dto.ExportStatusResponse;
import com.example.algorithm.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
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

    @GetMapping("/download/{taskId}")
    public ResponseEntity<InputStreamResource> downloadExportFile(@PathVariable String taskId) {
        InputStream inputStream = exportService.downloadExportFile(taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export_" + taskId + ".csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }
}
