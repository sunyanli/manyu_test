package com.example.demo.algorithm.service.impl;

import com.example.demo.algorithm.model.request.BubbleSortRequest;
import com.example.demo.algorithm.model.request.HashRequest;
import com.example.demo.algorithm.model.vo.BubbleSortVO;
import com.example.demo.algorithm.model.vo.HashVO;
import com.example.demo.algorithm.model.vo.HelloWorldVO;
import com.example.demo.algorithm.service.AlgorithmService;
import com.example.demo.common.constant.ErrorCodeEnum;
import com.example.demo.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 算法服务实现
 *
 * @author AiWork
 */
@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    private static final Logger logger = LoggerFactory.getLogger(AlgorithmServiceImpl.class);

    private static final String ALGORITHM_SHA256 = "SHA-256";
    private static final String SORT_ASC = "asc";
    private static final String SORT_DESC = "desc";

    @Override
    public HelloWorldVO helloWorld() {
        return new HelloWorldVO("Hello, World!");
    }

    @Override
    public HashVO computeHash(HashRequest request) {
        if (request == null || request.getInput() == null || request.getInput().trim().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ALG_002.getCode(), ErrorCodeEnum.ALG_002.getMessage());
        }

        String input = request.getInput();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String hash = hexString.toString();
            logger.info("哈希计算完成, input length: {}, hash: {}", input.length(), hash);
            return new HashVO(input, ALGORITHM_SHA256, hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 算法不可用", e);
            throw new BusinessException(ErrorCodeEnum.ALG_001.getCode(), ErrorCodeEnum.ALG_001.getMessage(), e);
        }
    }

    @Override
    public BubbleSortVO bubbleSort(BubbleSortRequest request) {
        if (request == null || request.getArray() == null || request.getArray().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ALG_003.getCode(), ErrorCodeEnum.ALG_003.getMessage());
        }

        String order = request.getOrder();
        if (order == null || order.trim().isEmpty()) {
            order = SORT_ASC;
        }
        if (!SORT_ASC.equalsIgnoreCase(order) && !SORT_DESC.equalsIgnoreCase(order)) {
            throw new BusinessException(ErrorCodeEnum.ALG_004.getCode(), ErrorCodeEnum.ALG_004.getMessage());
        }

        boolean ascending = SORT_ASC.equalsIgnoreCase(order);
        List<Integer> original = new ArrayList<Integer>(request.getArray());
        List<Integer> sorted = new ArrayList<Integer>(original);

        long startTime = System.currentTimeMillis();

        // 冒泡排序
        int n = sorted.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                boolean shouldSwap = ascending ? sorted.get(j) > sorted.get(j + 1)
                        : sorted.get(j) < sorted.get(j + 1);
                if (shouldSwap) {
                    int temp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, temp);
                }
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        logger.info("冒泡排序完成, size: {}, order: {}, duration: {}ms", original.size(), order, durationMs);

        return new BubbleSortVO(original, sorted, order.toLowerCase(), durationMs);
    }
}