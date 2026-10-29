package com.example.algorithm.controller;

import com.example.algorithm.model.dto.*;
import com.example.algorithm.service.AlgorithmService;
import com.example.algorithm.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for algorithm demonstration APIs.
 */
@RestController
@RequestMapping("/api/algorithm")
public class AlgorithmController {

    private static final Logger LOG = LoggerFactory.getLogger(AlgorithmController.class);

    private final AlgorithmService algorithmService;
    private final TrackingService trackingService;

    public AlgorithmController(AlgorithmService algorithmService, TrackingService trackingService) {
        this.algorithmService = algorithmService;
        this.trackingService = trackingService;
    }

    @PostMapping("/helloworld")
    public ApiResponse<HelloWorldResponse> helloWorld(@Valid @RequestBody HelloWorldRequest request,
                                                        HttpServletRequest httpRequest) {
        long start = System.currentTimeMillis();
        try {
            String message = algorithmService.helloWorld(request);
            trackingService.recordCall("/api/algorithm/helloworld", "HELLOWORLD",
                    request.getName(), "SUCCESS", (int) (System.currentTimeMillis() - start));
            return ApiResponse.ok(new HelloWorldResponse(message));
        } catch (Exception e) {
            trackingService.recordCall("/api/algorithm/helloworld", "HELLOWORLD",
                    request.getName(), "FAILED", (int) (System.currentTimeMillis() - start));
            throw e;
        }
    }

    @PostMapping("/hash")
    public ApiResponse<HashResponse> computeHash(@Valid @RequestBody HashRequest request,
                                                  HttpServletRequest httpRequest) throws NoSuchAlgorithmException {
        long start = System.currentTimeMillis();
        try {
            String[] result = algorithmService.computeHash(request);
            trackingService.recordCall("/api/algorithm/hash", "HASH",
                    request.getContent(), "SUCCESS", (int) (System.currentTimeMillis() - start));
            return ApiResponse.ok(new HashResponse(result[0], result[1]));
        } catch (NoSuchAlgorithmException e) {
            trackingService.recordCall("/api/algorithm/hash", "HASH",
                    request.getContent(), "FAILED", (int) (System.currentTimeMillis() - start));
            throw e;
        }
    }

    @PostMapping("/bubbleSort")
    public ApiResponse<BubbleSortResponse> bubbleSort(@Valid @RequestBody BubbleSortRequest request,
                                                       HttpServletRequest httpRequest) {
        long start = System.currentTimeMillis();
        try {
            var sorted = algorithmService.bubbleSort(request);
            Map<String, Object> params = new HashMap<>();
            params.put("order", request.getOrder());
            trackingService.recordCall("/api/algorithm/bubbleSort", "BUBBLE_SORT",
                    params.toString(), "SUCCESS", (int) (System.currentTimeMillis() - start));
            return ApiResponse.ok(new BubbleSortResponse(sorted, request.getOrder()));
        } catch (Exception e) {
            trackingService.recordCall("/api/algorithm/bubbleSort", "BUBBLE_SORT",
                    request.getOrder(), "FAILED", (int) (System.currentTimeMillis() - start));
            throw e;
        }
    }
}
