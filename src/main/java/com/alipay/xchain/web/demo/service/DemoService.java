package com.alipay.xchain.web.demo.service;

import com.alipay.xchain.web.demo.request.HashRequest;
import com.alipay.xchain.web.demo.request.SortRequest;
import com.alipay.xchain.web.demo.response.DemoResultVO;

/**
 * 演示接口服务
 *
 * @author AiWork
 * @since 2026-10-30
 */
public interface DemoService {

    /**
     * HelloWorld 接口 - 返回欢迎消息与时间戳
     *
     * @param userId   调用人工号
     * @param userName 调用人姓名
     * @return 演示结果
     */
    DemoResultVO hello(String userId, String userName);

    /**
     * 哈希算法接口 - 对输入字符串计算哈希摘要
     *
     * @param request  请求体
     * @param userId   调用人工号
     * @param userName 调用人姓名
     * @return 演示结果
     */
    DemoResultVO hash(HashRequest request, String userId, String userName);

    /**
     * 冒泡排序接口 - 对数字数组执行冒泡排序
     *
     * @param request  请求体
     * @param userId   调用人工号
     * @param userName 调用人姓名
     * @return 演示结果
     */
    DemoResultVO sort(SortRequest request, String userId, String userName);
}