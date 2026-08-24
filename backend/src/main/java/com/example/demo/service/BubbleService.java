package com.example.demo.service;

import com.example.demo.model.BubbleResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BubbleService {

    public BubbleResult sort(List<Integer> array) {
        if (array == null || array.isEmpty()) {
            return new BubbleResult(new ArrayList<>(), new ArrayList<>(), 0, 0);
        }

        List<Integer> original = new ArrayList<>(array);
        List<Integer> sorted = new ArrayList<>(array);
        int n = sorted.size();
        int swapCount = 0;
        int comparisonCount = 0;

        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                comparisonCount++;
                if (sorted.get(j) > sorted.get(j + 1)) {
                    // 交换
                    int temp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, temp);
                    swapCount++;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }

        return new BubbleResult(original, sorted, swapCount, comparisonCount);
    }
}