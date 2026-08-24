package com.example.algorithmdemo.service.impl;

import com.example.algorithmdemo.common.exception.BusinessException;
import com.example.algorithmdemo.model.vo.BubbleSortVO;
import com.example.algorithmdemo.model.vo.HashVO;
import com.example.algorithmdemo.model.vo.HelloWorldVO;
import com.example.algorithmdemo.service.AlgorithmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * 算法服务实现
 */
@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    private static final Logger log = LoggerFactory.getLogger(AlgorithmServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public HelloWorldVO helloWorld(String name) {
        if (name == null || name.isBlank()) {
            name = "World";
        }
        if (name.length() > 100) {
            throw BusinessException.paramInvalid("name 参数长度不能超过100字符");
        }
        String message = "Hello, " + name + "!";
        return new HelloWorldVO(message, now());
    }

    @Override
    public HashVO hash(String input, String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            algorithm = "SHA-256";
        }
        String normalizedAlgo = algorithm.toUpperCase().replace("-", "");
        String javaAlgo;
        switch (normalizedAlgo) {
            case "MD5":
                javaAlgo = "MD5";
                break;
            case "SHA256":
                javaAlgo = "SHA-256";
                break;
            case "SHA512":
                javaAlgo = "SHA-512";
                break;
            default:
                throw BusinessException.unsupportedAlgorithm();
        }
        try {
            MessageDigest md = MessageDigest.getInstance(javaAlgo);
            byte[] digest = md.digest(input.getBytes());
            String hashValue = HexFormat.of().formatHex(digest);
            return new HashVO(input, algorithm, hashValue, now());
        } catch (NoSuchAlgorithmException e) {
            log.error("不支持的哈希算法: {}", algorithm, e);
            throw BusinessException.unsupportedAlgorithm();
        }
    }

    @Override
    public BubbleSortVO bubbleSort(int[] array, String order) {
        if (array == null || array.length == 0) {
            throw BusinessException.paramInvalid("数组不能为空");
        }
        if (array.length > 1000) {
            throw BusinessException.paramInvalid("数组长度不能超过1000");
        }
        if (order == null || order.isBlank()) {
            order = "asc";
        }
        boolean asc = "asc".equalsIgnoreCase(order);

        int[] sorted = array.clone();
        long startTime = System.nanoTime();

        int n = sorted.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (asc ? sorted[j] > sorted[j + 1] : sorted[j] < sorted[j + 1]) {
                    int temp = sorted[j];
                    sorted[j] = sorted[j + 1];
                    sorted[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }

        long sortTime = (System.nanoTime() - startTime) / 1_000_000;
        return new BubbleSortVO(array, sorted, order, sortTime, now());
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}