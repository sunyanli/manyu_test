package com.manyu.algodemo.demo.service.impl;

import com.manyu.algodemo.common.exception.BizException;
import com.manyu.algodemo.common.exception.ErrorCode;
import com.manyu.algodemo.demo.model.dto.HashVO;
import com.manyu.algodemo.demo.model.dto.HelloWorldVO;
import com.manyu.algodemo.demo.model.dto.SortVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * demo 服务实现测试。
 */
class DemoServiceImplTest {

    private DemoServiceImpl demoService;

    @BeforeEach
    void setUp() {
        demoService = new DemoServiceImpl(10000, 4096);
    }

    @Test
    @DisplayName("helloworld 默认问候 World")
    void should_helloWithDefaultName() {
        HelloWorldVO vo = demoService.hello(null);
        assertThat(vo.getMessage()).isEqualTo("Hello, World!");
        assertThat(vo.getServerTime()).isNotBlank();
        assertThat(vo.getRequestId()).isNotBlank();
    }

    @Test
    @DisplayName("helloworld 带姓名问候")
    void should_helloWithName() {
        HelloWorldVO vo = demoService.hello("Alice");
        assertThat(vo.getMessage()).isEqualTo("Hello, Alice!");
    }

    @Test
    @DisplayName("哈希默认 SHA256")
    void should_hashWithDefaultAlgorithm() {
        HashVO vo = demoService.hash("hello world", null);
        assertThat(vo.getAlgorithm()).isEqualTo("SHA256");
        assertThat(vo.getHash()).isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
    }

    @Test
    @DisplayName("哈希算法大小写不敏感")
    void should_hashIgnoreCase() {
        HashVO vo = demoService.hash("hello world", "md5");
        assertThat(vo.getAlgorithm()).isEqualTo("MD5");
    }

    @Test
    @DisplayName("哈希文本超字节上限抛 DEMO_001")
    void should_throw_whenTextTooLong() {
        String text = "a".repeat(5000);
        assertThatThrownBy(() -> demoService.hash(text, "SHA256"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.DEMO_001.getCode());
    }

    @Test
    @DisplayName("非法哈希算法抛 DEMO_002")
    void should_throw_whenUnsupportedAlgorithm() {
        assertThatThrownBy(() -> demoService.hash("hello", "CRC32"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.DEMO_002.getCode());
    }

    @Test
    @DisplayName("冒泡排序升序返回前100元素")
    void should_bubbleSortAscending() {
        List<Double> input = List.of(5.0, 3.0, 8.0, 4.0, 2.0);
        SortVO vo = demoService.bubbleSort(input, null, null);
        assertThat(vo.getSorted()).containsExactly(2.0, 3.0, 4.0, 5.0, 8.0);
        assertThat(vo.getOriginalSize()).isEqualTo(5);
        assertThat(vo.getSwaps()).isEqualTo(6);
        assertThat(vo.getAlgorithmVersion()).isEqualTo("v1.0-optimized");
    }

    @Test
    @DisplayName("冒泡排序降序")
    void should_bubbleSortDescending() {
        SortVO vo = demoService.bubbleSort(List.of(3.0, 1.0, 4.0, 1.0, 5.0), "DESC", true);
        assertThat(vo.getSorted()).containsExactly(5.0, 4.0, 3.0, 1.0, 1.0);
    }

    @Test
    @DisplayName("排序数组超上限抛 DEMO_003")
    void should_throw_whenArrayTooLarge() {
        List<Double> input = new java.util.ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            input.add((double) i);
        }
        assertThatThrownBy(() -> demoService.bubbleSort(input, "ASC", true))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.DEMO_003.getCode());
    }

    @Test
    @DisplayName("数组含非有限数值抛 DEMO_001")
    void should_throw_whenNonFiniteValue() {
        assertThatThrownBy(() -> demoService.bubbleSort(List.of(1.0, Double.NaN), "ASC", true))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.DEMO_001.getCode());
    }

    @Test
    @DisplayName("非法排序方向抛 DEMO_001")
    void should_throw_whenInvalidOrder() {
        assertThatThrownBy(() -> demoService.bubbleSort(List.of(1.0, 2.0), "SIDE", true))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.DEMO_001.getCode());
    }
}
