package com.manyu.algodemo.demo.service;

import com.manyu.algodemo.demo.model.dto.HashVO;
import com.manyu.algodemo.demo.model.dto.HelloWorldVO;
import com.manyu.algodemo.demo.model.dto.SortVO;

import java.util.List;

/**
 * 示例接口服务：helloworld / 哈希算法 / 冒泡排序。
 */
public interface DemoService {

    /**
     * helloworld 执行。
     *
     * @param name 问候对象（可为空）
     * @return 问候结果
     */
    HelloWorldVO hello(String name);

    /**
     * 哈希算法执行。
     *
     * @param text      待哈希文本
     * @param algorithm 算法标识，空则默认 SHA256
     * @return 哈希结果
     */
    HashVO hash(String text, String algorithm);

    /**
     * 冒泡排序执行。
     *
     * @param data      待排序数值数组
     * @param order     排序方向，空则默认 ASC
     * @param optimized 是否启用优化版，空则默认 true
     * @return 排序结果
     */
    SortVO bubbleSort(List<Double> data, String order, Boolean optimized);
}
