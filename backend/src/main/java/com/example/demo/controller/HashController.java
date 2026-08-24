package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.HashRequest;
import com.example.demo.model.HashResult;
import com.example.demo.service.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HashController {

    @Autowired
    private HashService hashService;

    @PostMapping("/hash")
    @Traceable(apiName = "hash")
    public ApiResponse<HashResult> hash(@RequestBody HashRequest request) {
        HashResult result = hashService.computeHash(request.getInput(), request.getAlgorithm());
        return ApiResponse.success(result);
    }
}