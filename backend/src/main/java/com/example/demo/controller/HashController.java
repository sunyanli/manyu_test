package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.HashResult;
import com.example.demo.service.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HashController {

    @Autowired
    private HashService hashService;

    @PostMapping("/hash")
    @Traceable(apiName = "hash")
    public ApiResponse<HashResult> hash(@RequestBody Map<String, String> body) {
        String input = body.get("input");
        String algorithm = body.get("algorithm");
        HashResult result = hashService.computeHash(input, algorithm);
        return ApiResponse.success(result);
    }
}