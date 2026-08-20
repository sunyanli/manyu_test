package com.manyu.algodemo.demo.service.impl;

import com.manyu.algodemo.common.exception.BizException;
import com.manyu.algodemo.common.exception.ErrorCode;
import com.manyu.algodemo.demo.algorithm.BubbleSortAlgorithm;
import com.manyu.algodemo.demo.algorithm.HashUtils;
import com.manyu.algodemo.demo.model.dto.HashVO;
import com.manyu.algodemo.demo.model.dto.HelloWorldVO;
import com.manyu.algodemo.demo.model.dto.SortVO;
import com.manyu.algodemo.demo.model.enums.HashAlgorithm;
import com.manyu.algodemo.demo.model.enums.SortOrder;
import com.manyu.algodemo.demo.service.DemoService;
import com.manyu.algodemo.tracking.annotation.TrackCall;
import com.manyu.algodemo.tracking.model.enums.BizType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 示例接口服务实现。
 */
@Service
public class DemoServiceImpl implements DemoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoServiceImpl.class);

    private static final String DEFAULT_NAME = "World";
    private static final String DEFAULT_ALGORITHM = HashAlgorithm.SHA256.name();
    private static final String DEFAULT_ORDER = SortOrder.ASC.name();

    /** 冒泡排序结果返回数组元素上限。 */
    private static final int SORT_RESULT_LIMIT = 100;

    private final int maxSortSize;
    private final int maxTextBytes;

    /**
     * 构造器注入配置。
     *
     * @param maxSortSize  排序规模上界
     * @param maxTextBytes 哈希文本字节上界
     */
    public DemoServiceImpl(
            @Value("${demo.sort.max-size:10000}") int maxSortSize,
            @Value("${demo.hash.max-text-bytes:4096}") int maxTextBytes) {
        this.maxSortSize = maxSortSize;
        this.maxTextBytes = maxTextBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TrackCall(type = BizType.HELLO_WORLD)
    public HelloWorldVO hello(String name) {
        long start = System.currentTimeMillis();
        String target = StringUtils.hasText(name) ? name.trim() : DEFAULT_NAME;
        HelloWorldVO vo = new HelloWorldVO();
        vo.setMessage("Hello, " + target + "!");
        vo.setServerTime(Instant.now().toString());
        vo.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        vo.setCostTimeMs(System.currentTimeMillis() - start);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TrackCall(type = BizType.HASH)
    public HashVO hash(String text, String algorithm) {
        long start = System.currentTimeMillis();
        String alg = StringUtils.hasText(algorithm) ? algorithm.trim().toUpperCase() : DEFAULT_ALGORITHM;
        parseAlgorithm(alg);
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        if (input.length > maxTextBytes) {
            throw new BizException(ErrorCode.DEMO_001, "text 超上限 " + maxTextBytes + " 字节");
        }
        HashVO vo = new HashVO();
        vo.setAlgorithm(alg);
        vo.setHash(HashUtils.hash(text, alg));
        vo.setInputLength(input.length);
        vo.setCostTimeMs(System.currentTimeMillis() - start);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TrackCall(type = BizType.BUBBLE_SORT)
    public SortVO bubbleSort(List<Double> data, String order, Boolean optimized) {
        long start = System.currentTimeMillis();
        if (data.size() > maxSortSize) {
            throw new BizException(ErrorCode.DEMO_003, "数组数量超上限 " + maxSortSize);
        }
        SortOrder sortOrder = StringUtils.hasText(order) ? parseOrder(order.trim().toUpperCase()) : SortOrder.ASC;
        boolean optimizedFlag = optimized == null || optimized;
        BubbleSortAlgorithm.SortResult result =
                BubbleSortAlgorithm.sort(sanitize(data), sortOrder == SortOrder.ASC, optimizedFlag);
        SortVO vo = new SortVO();
        vo.setOriginalSize(data.size());
        vo.setSorted(limit(result.getSorted(), SORT_RESULT_LIMIT));
        vo.setSwaps(result.getSwaps());
        vo.setCostTimeMs(System.currentTimeMillis() - start);
        vo.setAlgorithmVersion(result.getAlgorithmVersion());
        return vo;
    }

    private List<Double> sanitize(List<Double> data) {
        for (Double value : data) {
            if (value == null || !Double.isFinite(value)) {
                throw new BizException(ErrorCode.DEMO_001, "数组含非有限数值（NaN/Infinity）或空元素");
            }
        }
        return data;
    }

    private List<Double> limit(List<Double> sorted, int limit) {
        return sorted.size() <= limit ? sorted : sorted.subList(0, limit);
    }

    private HashAlgorithm parseAlgorithm(String alg) {
        try {
            return HashAlgorithm.valueOf(alg);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("不支持的哈希算法: {}", alg);
            throw new BizException(ErrorCode.DEMO_002, "不支持的哈希算法: " + alg);
        }
    }

    private SortOrder parseOrder(String order) {
        try {
            return SortOrder.valueOf(order);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("不支持的排序方向: {}", order);
            throw new BizException(ErrorCode.DEMO_001, "不支持的排序方向: " + order);
        }
    }
}
