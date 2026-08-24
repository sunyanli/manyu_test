package com.example.algorithmdemo.service;

import com.example.algorithmdemo.model.vo.BubbleSortVO;
import com.example.algorithmdemo.model.vo.HashVO;
import com.example.algorithmdemo.model.vo.HelloWorldVO;

/**
 * 算法服务接口
 */
public interface AlgorithmService {

    /**
     * HelloWorld 服务
     *
     * @param name 用户名称
     * @return 问候信息
     */
    HelloWorldVO helloWorld(String name);

    /**
     * 哈希计算服务
     *
     * @param input     输入字符串
     * @param algorithm 哈希算法 (MD5/SHA-256/SHA-512)
     * @return 哈希计算结果
     */
    HashVO hash(String input, String algorithm);

    /**
     * 冒泡排序服务
     *
     * @param array 待排序数组
     * @param order 排序顺序 (asc/desc)
     * @return 排序结果
     */
    BubbleSortVO bubbleSort(int[] array, String order);
}