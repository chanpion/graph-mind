package com.chenpp.graph.admin.service;

import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.core.schema.GraphSchema;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/12 15:45
 */
public interface GraphSchemaService {

    /**
     * 发布图Schema到图数据库（支持 discovered graph）
     */
    void publishSchema(Long graphId, Long connectionId, String graphCode);

    /**
     * 获取图Schema
     */
    GraphSchema getGraphSchema(Long graphId);

    /**
     * 从图数据库发现Schema（获取图数据库中实际的点边类型）
     */
    GraphSchema discoverSchema(Long connectionId, String graphCode, Long graphId);

    /**
     * 从图数据库发现节点定义列表，转换为 GraphVertexDef
     */
    List<GraphVertexDef> discoverVertexDefs(Long graphId, Long connectionId, String graphCode);

    /**
     * 从图数据库发现边定义列表，转换为 GraphEdgeDef
     */
    List<GraphEdgeDef> discoverEdgeDefs(Long graphId, Long connectionId, String graphCode);

    /**
     * 合并从图数据库发现的节点属性到已有的节点定义中
     */
    void mergeDiscoveredVertexProperties(List<GraphVertexDef> vertexDefs, Long graphId, Long connectionId, String graphCode);

    /**
     * 合并从图数据库发现的边属性到已有的边定义中
     */
    void mergeDiscoveredEdgeProperties(List<GraphEdgeDef> edgeDefs, Long graphId, Long connectionId, String graphCode);

    /**
     * 导出图Schema
     */
    SchemaExportDTO exportSchema(Long graphId);

    /**
     * 导入图Schema
     */
    void importSchema(Long graphId, SchemaImportDTO importDTO);

    /**
     * 在图数据库中创建图（不发布Schema）
     */
    Long createGraphInDatabase(GraphInfo graphInfo);

    /**
     * 删除节点定义（同时删除图数据库中的Schema）
     */
    boolean deleteVertexDef(Long graphId, Long vertexId);

    /**
     * 删除边定义（同时删除图数据库中的Schema）
     */
    boolean deleteEdgeDef(Long graphId, Long edgeId);
}
