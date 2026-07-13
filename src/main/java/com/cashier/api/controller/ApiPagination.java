package com.cashier.api.controller;

import com.cashier.model.PageResult;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API 分页参数和响应工具。
 */
final class ApiPagination {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 500;

    private ApiPagination() {
    }

    static PageRequest from(Context ctx) {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(DEFAULT_PAGE);
        int pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(DEFAULT_PAGE_SIZE);
        return new PageRequest(Math.max(1, page), Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE)));
    }

    static <T> Map<String, Object> success(PageResult<T> pageResult) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", pageResult.getData());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getPageNum());
        result.put("pageSize", pageResult.getPageSize());
        result.put("pages", pageResult.getPages());
        result.put("hasMore", pageResult.hasNextPage());
        return result;
    }

    record PageRequest(int page, int pageSize) {
    }
}
