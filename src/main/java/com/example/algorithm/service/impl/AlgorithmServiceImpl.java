package com.example.algorithm.service.impl;

import com.example.algorithm.model.dto.BubbleSortRequest;
import com.example.algorithm.model.dto.HashRequest;
import com.example.algorithm.model.dto.HelloWorldRequest;
import com.example.algorithm.service.AlgorithmService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of algorithm demonstration service.
 */
@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    @Override
    public String helloWorld(HelloWorldRequest request) {
        String name = request.getName();
        if (name == null || name.isBlank()) {
            return "Hello, World!";
        }
        return "Hello, " + name + "!";
    }

    @Override
    public String[] computeHash(HashRequest request) throws NoSuchAlgorithmException {
        String content = request.getContent();
        String algorithm = request.getAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            algorithm = "SHA-256";
        }
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return new String[]{sb.toString(), algorithm};
    }

    @Override
    public List<Integer> bubbleSort(BubbleSortRequest request) {
        List<Integer> arr = new ArrayList<>(request.getArray());
        int n = arr.size();
        for (int i = 0; i < n; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                boolean condition = "DESC".equalsIgnoreCase(request.getOrder())
                        ? arr.get(j) < arr.get(j + 1)
                        : arr.get(j) > arr.get(j + 1);
                if (condition) {
                    int temp = arr.get(j);
                    arr.set(j, arr.get(j + 1));
                    arr.set(j + 1, temp);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
        return arr;
    }
}
