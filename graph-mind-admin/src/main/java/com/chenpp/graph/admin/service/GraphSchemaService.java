package com.chenpp.graph.admin.service;

/**
 * @author April.Chen
 * @date 2025/8/12 15:45
 */
public interface GraphSchemaService {

    /**
     * 发布图Schema到图数据库
     *
     * @param graphId
     */
    void publishSchema(Long graphId);
}
