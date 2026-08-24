package com.example.demo.service;

import com.example.demo.model.HashResult;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class HashService {

    public HashResult computeHash(String input, String algorithm) {
        if (input == null) {
            throw new IllegalArgumentException("input 不能为空");
        }
        String safeAlgorithm = (algorithm != null && !algorithm.isBlank()) ? algorithm : "SHA-256";
        try {
            MessageDigest digest = MessageDigest.getInstance(safeAlgorithm);
            byte[] hashBytes = digest.digest(input.getBytes());
            String hashHex = HexFormat.of().formatHex(hashBytes);
            return new HashResult(input, safeAlgorithm, hashHex);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("不支持的算法: " + safeAlgorithm + "，支持: SHA-256, MD5, SHA-512");
        }
    }
}