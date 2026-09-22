package com.antdigital.sort;

import java.util.List;

/**
 * 排序算法统一抽象接口。
 *
 * <p>所有排序算法实现本接口，对外提供统一的原地排序能力。
 * 排序方向由具体实现类决定（如 {@code BubbleSort} 升序、{@code DescendingBubbleSort} 降序）。
 *
 * @author AiWork
 * @date 2026/09/22
 */
public interface SortingAlgorithm {

    /**
     * 对列表进行原地排序，并返回原列表引用（便于链式调用）。
     *
     * <p>约束：<br>
     * 1. null 入参抛出 {@link IllegalArgumentException}；<br>
     * 2. 空列表或单元素列表直接原样返回，不进入循环；<br>
     * 3. 元素不可比较（{@code compareTo} 抛 {@link ClassCastException}）时向上传播。
     *
     * @param list 待排序列表，元素须实现 {@link Comparable}
     * @param <T>  元素类型
     * @return 已排序的同一列表引用（原地排序）
     */
    <T extends Comparable<? super T>> List<T> sort(List<T> list);
}