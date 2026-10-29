package com.example.algorithm.service;

import com.example.algorithm.model.dto.BubbleSortRequest;
import com.example.algorithm.model.dto.HashRequest;
import com.example.algorithm.model.dto.HelloWorldRequest;

import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Algorithm demonstration service.
 */
public interface AlgorithmService {

    /**
     * Execute HelloWorld.
     *
     * @param request the request
     * @return the hello message
     */
    String helloWorld(HelloWorldRequest request);

    /**
     * Compute hash value.
     *
     * @param request the request
     * @return hash result as [hashValue, algorithm]
     * @throws NoSuchAlgorithmException if algorithm not supported
     */
    String[] computeHash(HashRequest request) throws NoSuchAlgorithmException;

    /**
     * Execute bubble sort.
     *
     * @param request the request
     * @return sorted array
     */
    List<Integer> bubbleSort(BubbleSortRequest request);
}
