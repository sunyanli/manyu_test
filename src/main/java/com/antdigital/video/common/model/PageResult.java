package com.antdigital.video.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果。list 为空集合时返回空列表，避免调用方做 null 判断。
 *
 * @param <T> 元素类型
 */
public class PageResult<T> {

    private final List<T> list;
    private final long total;

    private PageResult(List<T> list, long total) {
        this.list = list == null ? new ArrayList<>() : new ArrayList<>(list);
        this.total = total;
    }

    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L);
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return "PageResult{total=" + total + ", size=" + list.size() + "}";
    }
}