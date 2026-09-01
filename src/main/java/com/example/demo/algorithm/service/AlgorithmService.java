package com.example.demo.algorithm.service;

import com.example.demo.algorithm.model.request.BubbleSortRequest;
import com.example.demo.algorithm.model.request.HashRequest;
import com.example.demo.algorithm.model.vo.BubbleSortVO;
import com.example.demo.algorithm.model.vo.HashVO;
import com.example.demo.algorithm.model.vo.HelloWorldVO;

/**
 * 算法服务接口
 *
 * @author AiWork
 */
public interface AlgorithmService {

    /**
     * 返回 Hello World 问候语
     *
     * @return HelloWorldVO
     */
    HelloWorldVO helloWorld();

    /**
     * 对输入字符串计算 SHA-256 哈希值
     *
     * @param request 哈希请求（含待哈希字符串）
     * @return 哈希结果
     */
    HashVO computeHash(HashRequest request);

    /**
     * 对输入数组进行冒泡排序
     *
     * @param request 排序请求（含数组和排序方向）
     * @return 排序结果
     */
    BubbleSortVO bubbleSort(BubbleSortRequest request);
}