package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.BubbleResult;
import com.example.demo.service.BubbleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BubbleController {

    @Autowired
    private BubbleService bubbleService;

    @PostMapping("/bubble-sort")
    @Traceable(apiName = "bubble-sort")
    public ApiResponse<BubbleResult> bubbleSort(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> array = body.get("array");
        BubbleResult result = bubbleService.sort(array);
        return ApiResponse.success(result);
    }
}