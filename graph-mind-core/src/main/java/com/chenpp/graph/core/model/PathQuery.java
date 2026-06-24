package com.chenpp.graph.core.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 路径查询参数
 * 用于封装图数据库中的路径查询条件
 *
 * @author April.Chen
 * @date 2026/6/23
 */
@Data
public class PathQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 起始顶点ID（直接指定顶点）
     */
    private String startVertexId;

    /**
     * 终点顶点ID（直接指定顶点）
     */
    private String endVertexId;

    /**
     * 最大路径深度（默认5）
     */
    private Integer maxDepth = 5;

    /**
     * 起点查找条件 - 标签（类型）
     */
    private String startLabel;

    /**
     * 起点查找条件 - 属性名
     */
    private String startProp;

    /**
     * 起点查找条件 - 属性值
     */
    private String startValue;

    /**
     * 终点查找条件 - 标签（类型）
     */
    private String endLabel;

    /**
     * 终点查找条件 - 属性名
     */
    private String endProp;

    /**
     * 终点查找条件 - 属性值
     */
    private String endValue;


    private int limit;
    private Condition startProperty;
    private Condition endProperty;

    @Data
    public static class Condition {
        private String label;
        private String property;
        private String value;

        public Condition(String label, String property, String value) {
            this.label = label;
            this.property = property;
            this.value = value;
        }
    }
}
