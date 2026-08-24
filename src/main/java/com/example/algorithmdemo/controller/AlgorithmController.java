package com.example.algorithmdemo.controller;

import com.example.algorithmdemo.common.constant.ApiNameConstant;
import com.example.algorithmdemo.common.exception.GlobalExceptionHandler;
import com.example.algorithmdemo.model.dto.BubbleSortRequest;
import com.example.algorithmdemo.model.dto.HashRequest;
import com.example.algorithmdemo.model.vo.BubbleSortVO;
import com.example.algorithmdemo.model.vo.HashVO;
import com.example.algorithmdemo.model.vo.HelloWorldVO;
import com.example.algorithmdemo.service.AlgorithmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 算法模块控制器
 */
@RestController
@RequestMapping("/api")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * W01 - HelloWorld 接口
     */
    @GetMapping("/hello")
    public Map<String, Object> hello(@RequestParam(required = false) String name) {
        HelloWorldVO data = algorithmService.helloWorld(name);
        return GlobalExceptionHandler.buildResult("SUCCESS", "操作成功", data);
    }

    /**
     * W02 - 哈希算法接口
     */
    @PostMapping("/hash")
    public Map<String, Object> hash(@Valid @RequestBody HashRequest request) {
        HashVO data = algorithmService.hash(request.getInput(), request.getAlgorithm());
        return GlobalExceptionHandler.buildResult("SUCCESS", "操作成功", data);
    }

    /**
     * W03 - 冒泡排序接口
     */
    @PostMapping("/bubble-sort")
    public Map<String, Object> bubbleSort(@Valid @RequestBody BubbleSortRequest request) {
        BubbleSortVO data = algorithmService.bubbleSort(request.getArray(), request.getOrder());
        return GlobalExceptionHandler.buildResult("SUCCESS", "操作成功", data);
    }
}