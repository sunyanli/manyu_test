package com.alipay.xchain.web.demo.service.impl;

import com.alipay.xchain.web.demo.common.DemoException;
import com.alipay.xchain.web.demo.model.ApiCallRecordDO;
import com.alipay.xchain.web.demo.request.HashRequest;
import com.alipay.xchain.web.demo.request.SortRequest;
import com.alipay.xchain.web.demo.response.DemoResultVO;
import com.alipay.xchain.web.demo.service.ApiCallRecordService;
import com.alipay.xchain.web.demo.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示接口服务实现
 *
 * @author AiWork
 * @since 2026-10-30
 */
@Service
public class DemoServiceImpl implements DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DEFAULT_ALGORITHM = "SHA-256";
    private static final String DEFAULT_ORDER = "asc";

    @Autowired
    private ApiCallRecordService apiCallRecordService;

    @Override
    public DemoResultVO hello(String userId, String userName) {
        DemoResultVO result = new DemoResultVO();
        result.setType("hello");
        result.setMessage("Hello World! 接口演示中心 v1.0");
        result.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(ISO_FORMATTER));

        // 异步埋点
        recordTrack("hello", userId, userName, "success");

        if (logger.isInfoEnabled()) {
            logger.info("HelloWorld 调用完成, userId: {}", userId);
        }
        return result;
    }

    @Override
    public DemoResultVO hash(HashRequest request, String userId, String userName) {
        String input = request.getInput();
        if (input == null || input.trim().isEmpty()) {
            throw new DemoException("DEMO_011", "输入参数不能为空");
        }

        String algorithm = request.getAlgorithm();
        if (algorithm == null || algorithm.trim().isEmpty()) {
            algorithm = DEFAULT_ALGORITHM;
        }
        algorithm = algorithm.toUpperCase();

        // 校验算法
        if (!"MD5".equals(algorithm) && !"SHA-1".equals(algorithm) && !"SHA-256".equals(algorithm)) {
            throw new DemoException("DEMO_010", "算法不支持: " + algorithm + "，可选 MD5/SHA-1/SHA-256");
        }

        String output = computeHash(input, algorithm);

        DemoResultVO result = new DemoResultVO();
        result.setType("hash");
        result.setAlgorithm(algorithm);
        result.setInput(input);
        result.setOutput(output);
        result.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(ISO_FORMATTER));

        recordTrack("hash", userId, userName, "success");

        if (logger.isInfoEnabled()) {
            logger.info("哈希计算完成, algorithm: {}, userId: {}", algorithm, userId);
        }
        return result;
    }

    @Override
    public DemoResultVO sort(SortRequest request, String userId, String userName) {
        List<Integer> numbers = request.getNumbers();
        if (numbers == null || numbers.isEmpty()) {
            throw new DemoException("DEMO_020", "排序输入不能为空");
        }

        String order = request.getOrder();
        if (order == null || order.trim().isEmpty()) {
            order = DEFAULT_ORDER;
        }
        order = order.toLowerCase();

        if (!"asc".equals(order) && !"desc".equals(order)) {
            throw new DemoException("DEMO_021", "排序方向非法: " + order + "，可选 asc/desc");
        }

        // 拷贝数组避免影响调用方引用
        int[] original = numbers.stream().mapToInt(Integer::intValue).toArray();
        int[] copy = numbers.stream().mapToInt(Integer::intValue).toArray();

        BubbleSortResult sortResult = bubbleSort(copy, "desc".equals(order));

        DemoResultVO result = new DemoResultVO();
        result.setType("sort");
        result.setOriginal(original);
        result.setSorted(sortResult.sorted);
        result.setOrder(order);
        result.setSwapCount(sortResult.swapCount);
        result.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(ISO_FORMATTER));

        recordTrack("sort", userId, userName, "success");

        if (logger.isInfoEnabled()) {
            logger.info("冒泡排序完成, order: {}, swapCount: {}, userId: {}", order, sortResult.swapCount, userId);
        }
        return result;
    }

    /**
     * 计算哈希
     */
    private String computeHash(String input, String algorithm) {
        try {
            // 映射算法名到 Java 标准名
            String javaAlgorithm;
            switch (algorithm) {
                case "MD5":
                    javaAlgorithm = "MD5";
                    break;
                case "SHA-1":
                    javaAlgorithm = "SHA-1";
                    break;
                case "SHA-256":
                default:
                    javaAlgorithm = "SHA-256";
                    break;
            }
            MessageDigest md = MessageDigest.getInstance(javaAlgorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("哈希算法不可用: {}", algorithm, e);
            throw new DemoException("DEMO_010", "算法不支持: " + algorithm);
        }
    }

    /**
     * 冒泡排序（带交换次数统计）
     */
    private BubbleSortResult bubbleSort(int[] arr, boolean descending) {
        int n = arr.length;
        int swapCount = 0;
        for (int i = 0; i < n; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                boolean needSwap = descending ? arr[j] < arr[j + 1] : arr[j] > arr[j + 1];
                if (needSwap) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapCount++;
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
        return new BubbleSortResult(arr, swapCount);
    }

    /**
     * 异步记录埋点
     */
    private void recordTrack(String apiName, String userId, String userName, String status) {
        try {
            ApiCallRecordDO record = new ApiCallRecordDO();
            record.setApiName(apiName);
            record.setCallerId(userId != null ? userId : "anonymous");
            record.setCallerName(userName != null ? maskName(userName) : "匿名用户");
            record.setPersonType("");
            record.setPersonLevel("");
            record.setPersonDept("");
            record.setCallTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            record.setResultStatus(status);
            record.setGmtCreate(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            record.setGmtModified(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            apiCallRecordService.record(record);
        } catch (RuntimeException e) {
            logger.error("埋点记录失败, apiName: {}, userId: {}", apiName, userId, e);
        }
    }

    /**
     * 姓名脱敏：姓 + *
     */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "匿名用户";
        }
        if (name.length() == 1) {
            return name;
        }
        return name.substring(0, 1) + "*";
    }

    /**
     * 冒泡排序结果
     */
    private static class BubbleSortResult {
        final int[] sorted;
        final int swapCount;

        BubbleSortResult(int[] sorted, int swapCount) {
            this.sorted = sorted;
            this.swapCount = swapCount;
        }
    }
}