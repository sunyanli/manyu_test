package com.example.demo.algorithm.controller;

import com.example.demo.algorithm.model.request.BubbleSortRequest;
import com.example.demo.algorithm.model.request.HashRequest;
import com.example.demo.algorithm.service.AlgorithmService;
import com.example.demo.common.model.ApiResponse;
import com.example.demo.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 算法控制器
 *
 * @author AiWork
 */
@RestController
@RequestMapping("/api/algorithm")
public class AlgorithmController {

    private static final Logger logger = LoggerFactory.getLogger(AlgorithmController.class);

    private final AlgorithmService algorithmService;
    private final TrackingService trackingService;

    public AlgorithmController(AlgorithmService algorithmService, TrackingService trackingService) {
        this.algorithmService = algorithmService;
        this.trackingService = trackingService;
    }

    /**
     * HelloWorld 接口
     */
    @GetMapping("/helloworld")
    public ApiResponse<?> helloWorld(HttpServletRequest httpRequest) {
        ApiResponse<?> result = ApiResponse.success(algorithmService.helloWorld());
        trackingService.recordCall("helloworld", getUserId(httpRequest));
        return result;
    }

    /**
     * 哈希算法接口
     */
    @PostMapping("/hash")
    public ApiResponse<?> hash(@Valid @RequestBody HashRequest request, HttpServletRequest httpRequest) {
        ApiResponse<?> result = ApiResponse.success(algorithmService.computeHash(request));
        trackingService.recordCall("hash", getUserId(httpRequest));
        return result;
    }

    /**
     * 冒泡排序接口
     */
    @PostMapping("/bubble-sort")
    public ApiResponse<?> bubbleSort(@Valid @RequestBody BubbleSortRequest request, HttpServletRequest httpRequest) {
        ApiResponse<?> result = ApiResponse.success(algorithmService.bubbleSort(request));
        trackingService.recordCall("bubble-sort", getUserId(httpRequest));
        return result;
    }

    /**
     * 从请求中获取当前用户 ID
     */
    private String getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.trim().isEmpty()) {
            userId = "anonymous";
        }
        return userId;
    }
}