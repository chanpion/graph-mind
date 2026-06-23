package com.chenpp.graph.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 图展开请求参数
 *
 * @author April.Chen
 * @date 2026/6/22
 */
@Data
public class GraphExpandRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图ID
     */
    private Long graphId;

    /**
     * 顶点ID（查询值）
     */
    private String vertexId;

    /**
     * 展开深度（默认为1）
     */
    private Integer depth = 1;

    /**
     * 标签名称
     */
    private String label;

    /**
     * 属性名称
     */
    private String property;

    /**
     * 连接ID（可选）
     */
    private Long connectionId;

    /**
     * 图标识
     */
    private String graphCode;
}