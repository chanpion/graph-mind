package com.chenpp.graph.core.schema;

import lombok.Data;

/**
 * 图谱定义
 *
 * @author April.Chen
 * @date 2024/3/28 11:37
 */
@Data
public class Graph {
    /**
     * 唯一ID
     */
    public String uid;
    /**
     * 标识
     */
    private String code;
    /**
     * 名称
     */
    private String name;

    /**
     * 状态：停止、运行中、被删除、启动中
     */
    private String status;

    /**
     * 规模：万、百万、千万、亿、十亿、百亿
     */
    private String scale;
}
