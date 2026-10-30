package com.example.algorithm.service;

import com.example.algorithm.model.dto.BubbleSortRequest;
import com.example.algorithm.model.dto.HashRequest;
import com.example.algorithm.model.dto.HelloWorldRequest;
import com.example.algorithm.service.impl.AlgorithmServiceImpl;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for AlgorithmService.
 */
class AlgorithmServiceTest {

    private final AlgorithmService algorithmService = new AlgorithmServiceImpl();

    @Test
    void should_returnDefaultMessage_when_nameIsNull() {
        HelloWorldRequest request = new HelloWorldRequest();
        request.setName(null);
        String result = algorithmService.helloWorld(request);
        assertEquals("Hello, World!", result);
    }

    @Test
    void should_returnPersonalizedMessage_when_nameProvided() {
        HelloWorldRequest request = new HelloWorldRequest();
        request.setName("Manyu");
        String result = algorithmService.helloWorld(request);
        assertEquals("Hello, Manyu!", result);
    }

    @Test
    void should_returnMd5Hash_when_md5Algorithm() throws NoSuchAlgorithmException {
        HashRequest request = new HashRequest();
        request.setContent("hello world");
        request.setAlgorithm("MD5");
        String[] result = algorithmService.computeHash(request);
        assertEquals("MD5", result[1]);
        assertEquals(32, result[0].length());
    }

    @Test
    void should_returnSha256Hash_when_defaultAlgorithm() throws NoSuchAlgorithmException {
        HashRequest request = new HashRequest();
        request.setContent("hello world");
        request.setAlgorithm(null);
        String[] result = algorithmService.computeHash(request);
        assertEquals("SHA-256", result[1]);
        assertEquals(64, result[0].length());
    }

    @Test
    void should_sortAscending_when_ascOrder() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(5, 3, 8, 4, 2));
        request.setOrder("ASC");
        List<Integer> result = algorithmService.bubbleSort(request);
        assertEquals(Arrays.asList(2, 3, 4, 5, 8), result);
    }

    @Test
    void should_sortDescending_when_descOrder() {
        BubbleSortRequest request = new BubbleSortRequest();
        request.setArray(Arrays.asList(5, 3, 8, 4, 2));
        request.setOrder("DESC");
        List<Integer> result = algorithmService.bubbleSort(request);
        assertEquals(Arrays.asList(8, 5, 4, 3, 2), result);
    }
}
