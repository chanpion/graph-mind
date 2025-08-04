package com.chenpp.graph.admin.model;

import lombok.Data;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 10:50
 */
@Data
public class PageResult<T> {
    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页记录数
     */
    private Integer pageSize;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 构造空分页结果
     *
     * @param pageNum  当前页码
     * @param pageSize 每页记录数
     * @param <T>      数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        return new PageResult<>(List.of(), 0L, pageNum, pageSize);
    }
}