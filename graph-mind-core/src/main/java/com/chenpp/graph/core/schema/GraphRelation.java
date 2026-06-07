package com.chenpp.graph.core.schema;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 关系定义
 *
 * @author April.Chen
 * @date 2024/3/28 11:38
 */
@Data
public class GraphRelation {
    /**
     * 关系标识
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

    /**
     * 源实体类型
     */
    private String startLabel;
    /**
     * 目标实体类型
     */
    private String endLabel;
    /**
     * 是否有向
     */
    private Boolean directed;
    /**
     * 是否多边
     */
    private Boolean multiple;

    public GraphRelation() {
        properties = new ArrayList<>();
    }

    public GraphRelation(String label) {
        this.label = label;
        this.properties = new ArrayList<>();
    }

    public GraphRelation(String label, List<GraphProperty> properties) {
        this.label = label;
        this.properties = properties;
    }
}
