package com.chenpp.graph.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 图路径查询请求参数
 *
 * @author April.Chen
 * @date 2026/6/22
 */
@Data
public class GraphPathRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图ID
     */
    private Long graphId;

    /**
     * 连接ID（可选）
     */
    private Long connectionId;

    /**
     * 图代码（可选）
     */
    private String graphCode;

    /**
     * 起始顶点ID
     */
    private String startVertexId;

    /**
     * 终点顶点ID
     */
    private String endVertexId;

    /**
     * 最大深度（默认5）
     */
    private Integer maxDepth = 5;

    /**
     * 起点查找条件 - 属性值
     */
    private String startValue;

    /**
     * 起点查找条件 - 标签
     */
    private String startLabel;

    /**
     * 起点查找条件 - 属性名
     */
    private String startProp;

    /**
     * 终点查找条件 - 属性值
     */
    private String endValue;

    /**
     * 终点查找条件 - 标签
     */
    private String endLabel;

    /**
     * 终点查找条件 - 属性名
     */
    private String endProp;
}