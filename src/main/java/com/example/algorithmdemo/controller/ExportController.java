package com.example.algorithmdemo.controller;

import com.example.algorithmdemo.common.exception.BusinessException;
import com.example.algorithmdemo.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 导出控制器
 */
@RestController
@RequestMapping("/api")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * W04 - 导出接口
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String apiName,
            @RequestParam(required = false, defaultValue = "xlsx") String format) {

        byte[] data = exportService.exportData(apiName, format);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "export_" + apiName + "_" + timestamp + "." + format;

        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}