package com.chenpp.graph.admin.enums;

/**
 * 图数据库连接状态枚举
 *
 * @author April.Chen
 * @date 2026/6/11 09:31
 */
public enum ConnectionStatusEnum {
    /***/
    UNCHECKED(0, "未检测"),
    CONNECTED(1, "通过"),
    FAILED(2, "失败");

    private final int code;
    private final String description;

    ConnectionStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}