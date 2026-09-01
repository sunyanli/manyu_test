package com.example.demo.algorithm.service.impl;

import com.example.demo.algorithm.model.request.BubbleSortRequest;
import com.example.demo.algorithm.model.request.HashRequest;
import com.example.demo.algorithm.model.vo.BubbleSortVO;
import com.example.demo.algorithm.model.vo.HashVO;
import com.example.demo.algorithm.model.vo.HelloWorldVO;
import com.example.demo.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 算法服务单元测试
 *
 * @author AiWork
 */
@DisplayName("算法服务测试")
class AlgorithmServiceImplTest {

    private AlgorithmServiceImpl algorithmService;

    @BeforeEach
    void setUp() {
        algorithmService = new AlgorithmServiceImpl();
    }

    // ==================== HelloWorld ====================

    @Test
    @DisplayName("helloworld 应返回正确问候语")
    void shouldReturnHelloWorld_whenCalled() {
        HelloWorldVO result = algorithmService.helloWorld();
        assertNotNull(result);
        assertEquals("Hello, World!", result.getMessage());
    }

    // ==================== Hash ====================

    @Test
    @DisplayName("哈希计算：正常输入应返回 SHA-256 哈希值")
    void shouldReturnHash_whenValidInput() {
        HashRequest request = new HashRequest();
        request.setInput("hello");

        HashVO result = algorithmService.computeHash(request);

        assertNotNull(result);
        assertEquals("hello", result.getInput());
        assertEquals("SHA-256", result.getAlgorithm());
        assertNotNull(result.getHash());
        assertEquals(64, result.getHash().length());
    }

    @Test
    @DisplayName("哈希计算：空输入应抛出异常")
    void shouldThrowException_whenInputEmpty() {
        HashRequest request = new HashRequest();
        request.setInput("");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> algorithmService.computeHash(request));
        assertEquals("ALG_002", exception.getErrorCode());
    }

    @Test
    @DisplayName("哈希计算：null 输入应抛出异常")
    void shouldThrowException_whenInputNull() {
        HashRequest request = new HashRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> algorithmService.computeHash(request));
        assertEquals("ALG_002", exception.getErrorCode());
    }

    @Test
    @DisplayName("哈希计算：request 为 null 应抛出异常")
    void shouldThrowException_whenRequestNull() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> algorithmService.computeHash(null));
        assertEquals("ALG_002", exception.getErrorCode());
    }

    @Test
    @DisplayName("哈希计算：相同输入应产生相同哈希值")
    void shouldProduceSameHash_whenSameInput() {
        HashRequest request1 = new HashRequest();
        request1.setInput("test");
        HashRequest request2 = new HashRequest();
        request2.setInput("test");

        HashVO result1 = algorithmService.computeHash(request1);
        HashVO result2 = algorithmService.computeHash(request2);

        assertEquals(result1.getHash(), result2.getHash());
    }

    // ==================== BubbleSort ====================

    @Test
    @DisplayName("冒泡排序：正常升序排序")
    void shouldSortAscending_whenValidInput() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(5, 3, 8, 4, 2));
        request.setOrder("asc");

        BubbleSortVO result = algorithmService.bubbleSort(request);

        assertNotNull(result);
        assertEquals(Arrays.asList(5, 3, 8, 4, 2), result.getOriginal());
        assertEquals(Arrays.asList(2, 3, 4, 5, 8), result.getSorted());
        assertEquals("asc", result.getOrder());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    @DisplayName("冒泡排序：正常降序排序")
    void shouldSortDescending_whenOrderDesc() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(1, 2, 3, 4, 5));
        request.setOrder("desc");

        BubbleSortVO result = algorithmService.bubbleSort(request);

        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result.getSorted());
        assertEquals("desc", result.getOrder());
    }

    @Test
    @DisplayName("冒泡排序：默认升序")
    void shouldSortAscending_whenOrderNotSpecified() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(3, 1, 2));

        BubbleSortVO result = algorithmService.bubbleSort(request);

        assertEquals(Arrays.asList(1, 2, 3), result.getSorted());
        assertEquals("asc", result.getOrder());
    }

    @Test
    @DisplayName("冒泡排序：空数组应抛出异常")
    void shouldThrowException_whenArrayEmpty() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Collections.<Integer>emptyList());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> algorithmService.bubbleSort(request));
        assertEquals("ALG_003", exception.getErrorCode());
    }

    @Test
    @DisplayName("冒泡排序：非法排序方向应抛出异常")
    void shouldThrowException_whenOrderInvalid() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(1, 2, 3));
        request.setOrder("invalid");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> algorithmService.bubbleSort(request));
        assertEquals("ALG_004", exception.getErrorCode());
    }

    @Test
    @DisplayName("冒泡排序：单元素数组")
    void shouldReturnSame_whenSingleElement() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Collections.singletonList(42));

        BubbleSortVO result = algorithmService.bubbleSort(request);

        assertEquals(Collections.singletonList(42), result.getSorted());
    }

    @Test
    @DisplayName("冒泡排序：已排序数组保持有序")
    void shouldKeepOrder_whenAlreadySorted() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(1, 2, 3, 4, 5));
        request.setOrder("asc");

        BubbleSortVO result = algorithmService.bubbleSort(request);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.getSorted());
    }
}