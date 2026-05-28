package com.chenpp.graph.admin.service;

import com.chenpp.graph.core.schema.GraphSchema;

/**
 * @author April.Chen
 * @date 2025/8/12 15:45
 */
public interface GraphSchemaService {

    /**
     * 发布图Schema到图数据库
     *
     * @param graphId 图id
     */
    void publishSchema(Long graphId);

    /**
     * 获取图Schema
     *
     * @param graphId 图id
     * @return 图Schema
     */
    GraphSchema getGraphSchema(Long graphId);

    /**
     * 从图数据库发现Schema（获取图数据库中实际的点边类型）
     *
     * @param graphId 图id
     * @return 图Schema
     */
    GraphSchema discoverSchema(Long graphId);
}
