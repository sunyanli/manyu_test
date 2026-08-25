package com.example.org.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic paginated result wrapper.
 *
 * @param <T> the type of items in the list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private int page;
    private int pageSize;
    private List<T> list;

    /**
     * Create a paginated result.
     *
     * @param <T>      the type of items
     * @param total    total number of records
     * @param page     current page number
     * @param pageSize number of items per page
     * @param list     the items on this page
     * @return PageResult populated with the given values
     */
    public static <T> PageResult<T> of(long total, int page, int pageSize, List<T> list) {
        return new PageResult<>(total, page, pageSize, list);
    }
}