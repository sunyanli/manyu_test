package com.alipay.xchain.web.demo.controller;

import com.alipay.xchain.web.demo.common.CommonResponse;
import com.alipay.xchain.web.demo.common.DemoException;
import com.alipay.xchain.web.demo.request.HashRequest;
import com.alipay.xchain.web.demo.request.SortRequest;
import com.alipay.xchain.web.demo.response.DemoResultVO;
import com.alipay.xchain.web.demo.response.StatsResultVO;
import com.alipay.xchain.web.demo.service.ApiCallRecordService;
import com.alipay.xchain.web.demo.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 接口演示控制器
 *
 * <p>提供 HelloWorld、哈希算法、冒泡排序、导出、统计共 5 个 REST 接口</p>
 *
 * @author AiWork
 * @since 2026-10-30
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private DemoService demoService;

    @Autowired
    private ApiCallRecordService apiCallRecordService;

    /**
     * W01 - HelloWorld 接口
     */
    @GetMapping("/hello")
    public CommonResponse<DemoResultVO> hello(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        DemoResultVO result = demoService.hello(userId, userName);
        return CommonResponse.ok(result);
    }

    /**
     * W02 - 哈希算法接口
     */
    @PostMapping("/hash")
    public CommonResponse<DemoResultVO> hash(
            @Valid @RequestBody HashRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        DemoResultVO result = demoService.hash(request, userId, userName);
        return CommonResponse.ok(result);
    }

    /**
     * W03 - 冒泡排序接口
     */
    @PostMapping("/sort")
    public CommonResponse<DemoResultVO> sort(
            @Valid @RequestBody SortRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        DemoResultVO result = demoService.sort(request, userId, userName);
        return CommonResponse.ok(result);
    }

    /**
     * W04 - 结果导出接口
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam("type") String type,
            @RequestParam(value = "format", required = false, defaultValue = "xlsx") String format) {

        byte[] excelBytes = apiCallRecordService.export(type);

        String fileName = "api_demo_" + type + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            encodedFileName = "export.xlsx";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    /**
     * W05 - 调用统计查询接口
     */
    @GetMapping("/stats")
    public CommonResponse<StatsResultVO> stats(
            @RequestParam("dimension") String dimension,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        StatsResultVO result = apiCallRecordService.stats(dimension, startDate, endDate);
        return CommonResponse.ok(result);
    }

    /**
     * 统一异常处理 - DemoException
     */
    @ExceptionHandler(DemoException.class)
    public CommonResponse<Void> handleDemoException(DemoException e) {
        logger.warn("业务异常: {}", e.toString());
        return CommonResponse.fail(e.getErrorCode(), e.getMessage());
    }

    /**
     * 统一异常处理 - 参数校验
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public CommonResponse<Void> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        logger.warn("参数校验失败: {}", message);
        return CommonResponse.fail("DEMO_001", message);
    }

    /**
     * 兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleException(Exception e) {
        logger.error("系统内部错误", e);
        return CommonResponse.fail("DEMO_001", "系统内部错误");
    }
}