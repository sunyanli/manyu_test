package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.HelloResult;
import com.example.demo.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    private HelloService helloService;

    @GetMapping("/hello")
    @Traceable(apiName = "hello")
    public ApiResponse<HelloResult> hello(@RequestParam(defaultValue = "World") String name) {
        HelloResult result = helloService.greet(name);
        return ApiResponse.success(result);
    }
}