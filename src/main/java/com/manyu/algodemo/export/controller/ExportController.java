package com.manyu.algodemo.export.controller;

import com.manyu.algodemo.export.model.dto.ExportRequest;
import com.manyu.algodemo.export.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * export 模块控制器：W04 页面结果导出。
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    /**
     * 构造器注入。
     *
     * @param exportService 导出服务
     */
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * W04 按页面导出展示结果（文件流下载）。
     *
     * @param request 导出请求
     * @return 文件流响应
     */
    @PostMapping
    public ResponseEntity<byte[]> export(@Valid @RequestBody ExportRequest request) {
        ExportService.ExportFile file = exportService.export(request);
        String encoded = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
