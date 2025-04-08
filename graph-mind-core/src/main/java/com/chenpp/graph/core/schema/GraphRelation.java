package com.chenpp.graph.core.schema;

import lombok.Data;

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
    private String name;

    /**
     * 属性列表
     */
    private List<GraphProperty> props;

    /**
     * 是否已发布
     */
    private Boolean deployed;

    /**
     * 源实体类型
     */
    private String sourceLabel;
    /**
     * 目标实体类型
     */
    private String targetLabel;
    /**
     * 是否有向
     */
    private Boolean directed;
    /**
     * 是否多边
     */
    private Boolean multiple;
    /**
     * 是否写图库
     */
    private Boolean sinkGraph;
}
