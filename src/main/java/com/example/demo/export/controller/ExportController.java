package com.example.demo.export.controller;

import com.example.demo.common.constant.ErrorCodeEnum;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.model.ApiResponse;
import com.example.demo.export.model.request.ExportRequest;
import com.example.demo.export.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 导出控制器
 *
 * @author AiWork
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 导出数据为 Excel
     */
    @PostMapping("/data")
    public ResponseEntity<?> exportData(@Valid @RequestBody ExportRequest request) {
        try {
            byte[] data = exportService.exportData(request);

            String fileName = URLEncoder.encode(request.getExportType() + "_export.xlsx",
                    StandardCharsets.UTF_8.toString());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + fileName)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);

        } catch (BusinessException e) {
            logger.warn("导出业务异常: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            logger.error("导出异常", e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCodeEnum.EXP_003.getCode(),
                    ErrorCodeEnum.EXP_003.getMessage()));
        }
    }
}