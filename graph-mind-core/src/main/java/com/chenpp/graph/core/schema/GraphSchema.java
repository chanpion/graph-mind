package com.chenpp.graph.core.schema;

import lombok.Data;

import java.util.List;

/**
 * 图谱schema
 *
 * @author April.Chen
 * @date 2024/5/14 17:34
 */
@Data
public class GraphSchema {

    private String graphCode;
    /**
     * 实体列表
     */
    private List<GraphEntity> entities;
    /**
     * 关系列表
     */
    private List<GraphRelation> relations;
    /**
     * 索引列表
     */
    private List<GraphIndex> indexes;
}
