package com.chenpp.graph.admin.service;

import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
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

    /**
     * 从图数据库发现Schema（获取图数据库中实际的点边类型）
     *
     * @param connectionId 图数据库连接ID
     * @param graphCode    图编码（图数据库中的空间/图名）
     * @return 图Schema
     */
    GraphSchema discoverSchema(Long connectionId, String graphCode);

    /**
     * 导出图Schema（节点定义和边定义）
     *
     * @param graphId 图id
     * @return 导出的Schema数据
     */
    SchemaExportDTO exportSchema(Long graphId);

    /**
     * 导入图Schema
     *
     * @param graphId  图id
     * @param importDTO 导入数据
     */
    void importSchema(Long graphId, SchemaImportDTO importDTO);
}
