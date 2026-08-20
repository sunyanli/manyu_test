package com.manyu.algodemo.demo.controller;

import com.manyu.algodemo.common.web.CommonResponse;
import com.manyu.algodemo.demo.model.dto.BubbleSortRequest;
import com.manyu.algodemo.demo.model.dto.HashRequest;
import com.manyu.algodemo.demo.model.dto.HashVO;
import com.manyu.algodemo.demo.model.dto.HelloWorldRequest;
import com.manyu.algodemo.demo.model.dto.HelloWorldVO;
import com.manyu.algodemo.demo.model.dto.SortVO;
import com.manyu.algodemo.demo.service.DemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo 模块控制器：W01 helloworld / W02 哈希 / W03 冒泡排序。
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    /**
     * 构造器注入。
     *
     * @param demoService demo 服务
     */
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * W01 helloworld 执行。
     *
     * @param request 入参
     * @return 问候结果
     */
    @PostMapping("/helloworld")
    public CommonResponse<HelloWorldVO> helloworld(@Valid @RequestBody HelloWorldRequest request) {
        return CommonResponse.ok(demoService.hello(request.getName()));
    }

    /**
     * W02 哈希算法执行。
     *
     * @param request 入参
     * @return 哈希结果
     */
    @PostMapping("/hash")
    public CommonResponse<HashVO> hash(@Valid @RequestBody HashRequest request) {
        return CommonResponse.ok(demoService.hash(request.getText(), request.getAlgorithm()));
    }

    /**
     * W03 冒泡排序执行。
     *
     * @param request 入参
     * @return 排序结果
     */
    @PostMapping("/bubble-sort")
    public CommonResponse<SortVO> bubbleSort(@Valid @RequestBody BubbleSortRequest request) {
        return CommonResponse.ok(demoService.bubbleSort(request.getData(), request.getOrder(), request.getOptimized()));
    }
}
