package com.chenpp.graph.admin.model;

import lombok.Data;

/**
 * 导入结果数据模型
 */
@Data
public class ImportResult {
    /**
     * 成功导入的条数
     */
    private int successCount;

    /**
     * 导入失败的条数
     */
    private int failureCount;

    /**
     * 总条数
     */
    private int totalCount;

    /**
     * 错误信息列表
     */
    private String[] errorMessages;

    public ImportResult() {
    }

    public ImportResult(int successCount, int failureCount, int totalCount, String[] errorMessages) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.totalCount = totalCount;
        this.errorMessages = errorMessages;
    }
}