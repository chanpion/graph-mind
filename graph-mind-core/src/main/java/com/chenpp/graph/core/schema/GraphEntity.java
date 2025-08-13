package com.chenpp.graph.core.schema;

import lombok.Data;

import java.util.List;

/**
 * 实体定义
 * @author April.Chen
 * @date 2024/3/28 11:37
 */
@Data
public class GraphEntity {
    /**
     * 实体标识
     */
    private String label;

    /**
     * 属性列表
     */
    private List<GraphProperty> properties;

    /**
     * 是否已发布
     */
    private Boolean deployed;
}
