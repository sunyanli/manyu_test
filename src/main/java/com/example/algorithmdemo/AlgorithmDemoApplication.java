package com.example.algorithmdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 算法演示与可视化平台 启动类
 */
@SpringBootApplication
@EnableAsync
public class AlgorithmDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlgorithmDemoApplication.class, args);
    }
}