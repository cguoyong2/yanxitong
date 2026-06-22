package com.yanxitong.common;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int pageSize,
        long pages
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    public static <T> PageResult<T> of(List<T> records, long total, Integer page, Integer pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        long pages = total <= 0 ? 0 : (long) Math.ceil((double) total / normalizedPageSize);
        return new PageResult<>(records, total, normalizedPage, normalizedPageSize, pages);
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static int offset(Integer page, Integer pageSize) {
        return (normalizePage(page) - 1) * normalizePageSize(pageSize);
    }
}
