package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 图Schema管理控制器
 *
 * @author April.Chen
 * @date 2025/8/4 16:10
 */
@Slf4j
@RestController
@RequestMapping("/api/graphs/{graphId}")
public class GraphSchemaController {

    @Autowired
    private GraphVertexDefService vertexDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphSchemaService graphSchemaService;

    /**
     * 获取节点定义列表
     * 如果元数据为空，自动从图数据库发现已有的点类型
     *
     * @param graphId 图ID
     * @param status  节点状态
     * @return 节点定义列表
     */
    @GetMapping("/vertices")
    public Result<List<GraphVertexDef>> getVertexDefs(
            @PathVariable Long graphId, Integer status,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        List<GraphVertexDef> vertexDefs = vertexDefService.getVertexDefsByGraphId(graphId, status);
        // 元数据为空时，从图数据库发现
        if (vertexDefs == null || vertexDefs.isEmpty()) {
            try {
                vertexDefs = discoverVertexDefs(graphId, connectionId, graphCode);
            } catch (Exception e) {
                log.warn("从图数据库发现节点定义失败: {}", e.getMessage());
                vertexDefs = new ArrayList<>();
            }
        } else {
            // 有定义但属性为空时，尝试从图数据库发现并合并属性
            boolean hasEmptyProperties = vertexDefs.stream()
                    .anyMatch(n -> n.getProperties() == null || n.getProperties().isEmpty());
            if (hasEmptyProperties) {
                try {
                    mergeDiscoveredVertexProperties(vertexDefs, graphId);
                } catch (Exception e) {
                    log.warn("合并从图数据库发现的节点属性失败: {}", e.getMessage());
                }
            }
        }
        return Result.success(vertexDefs);
    }

    /**
     * 新增节点定义
     *
     * @param graphId   图ID
     * @param vertexDef 节点定义信息
     * @return 是否成功
     */
    @PostMapping("/vertices")
    public Result<String> addVertexDef(@PathVariable Long graphId, @RequestBody GraphVertexDef vertexDef) {
        vertexDef.setGraphId(graphId);
        boolean success = vertexDefService.saveVertexDefWithProperties(vertexDef);
        if (success) {
            // 自动发布到图数据库，失败时抛异常让前端感知
            graphSchemaService.publishSchema(graphId);
            return Result.success("新增节点定义成功");
        } else {
            return Result.error("新增节点定义失败");
        }
    }

    /**
     * 更新节点定义
     *
     * @param graphId   图ID
     * @param vertexId  节点定义ID
     * @param vertexDef 节点定义信息
     * @return 是否成功
     */
    @PutMapping("/vertices/{vertexId}")
    public Result<String> updateVertexDef(@PathVariable Long graphId, @PathVariable Long vertexId, @RequestBody GraphVertexDef vertexDef) {
        vertexDef.setId(vertexId);
        vertexDef.setGraphId(graphId);
        boolean success = vertexDefService.updateVertexDefWithProperties(vertexDef);
        if (success) {
            return Result.success("更新节点定义成功");
        } else {
            return Result.error("更新节点定义失败");
        }
    }

    /**
     * 删除节点定义
     *
     * @param graphId  图ID
     * @param vertexId 节点定义ID
     * @return 是否成功
     */
    @DeleteMapping("/vertices/{vertexId}")
    public Result<String> deleteVertexDef(@PathVariable Long graphId, @PathVariable Long vertexId) {
        boolean success = vertexDefService.deleteVertexDefWithProperties(vertexId);
        if (success) {
            return Result.success("删除节点定义成功");
        } else {
            return Result.error("删除节点定义失败");
        }
    }

    /**
     * 获取边定义列表
     *
     * @param graphId 图ID
     * @param status  边状态
     * @return 边定义列表
     */
    @GetMapping("/edges")
    public Result<List<GraphEdgeDef>> getEdgeDefs(
            @PathVariable Long graphId, Integer status,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        List<GraphEdgeDef> edgeDefs = edgeDefService.getEdgeDefsByGraphId(graphId, status);
        // 元数据为空时，从图数据库发现
        if (edgeDefs == null || edgeDefs.isEmpty()) {
            try {
                edgeDefs = discoverEdgeDefs(graphId, connectionId, graphCode);
            } catch (Exception e) {
                log.warn("从图数据库发现边定义失败: {}", e.getMessage());
                edgeDefs = new ArrayList<>();
            }
        } else {
            // 有定义但属性为空时，尝试从图数据库发现并合并属性
            boolean hasEmptyProperties = edgeDefs.stream()
                    .anyMatch(e -> e.getProperties() == null || e.getProperties().isEmpty());
            if (hasEmptyProperties) {
                try {
                    mergeDiscoveredEdgeProperties(edgeDefs, graphId);
                } catch (Exception e) {
                    log.warn("合并从图数据库发现的边属性失败: {}", e.getMessage());
                }
            }
        }
        return Result.success(edgeDefs);
    }

    /**
     * 新增边定义
     *
     * @param graphId 图ID
     * @param edgeDef 边定义信息
     * @return 是否成功
     */
    @PostMapping("/edges")
    public Result<String> addEdgeDef(@PathVariable Long graphId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.saveEdgeDefWithProperties(edgeDef);
        if (success) {
            // 自动发布到图数据库，失败时抛异常让前端感知
            graphSchemaService.publishSchema(graphId);
            return Result.success("新增边定义成功");
        } else {
            return Result.error("新增边定义失败");
        }
    }

    /**
     * 更新边定义
     *
     * @param graphId 图ID
     * @param edgeId  边定义ID
     * @param edgeDef 边定义信息
     * @return 是否成功
     */
    @PutMapping("/edges/{edgeId}")
    public Result<String> updateEdgeDef(@PathVariable Long graphId, @PathVariable Long edgeId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setId(edgeId);
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.updateEdgeDefWithProperties(edgeDef);
        if (success) {
            return Result.success("更新边定义成功");
        } else {
            return Result.error("更新边定义失败");
        }
    }

    /**
     * 删除边定义
     *
     * @param graphId 图ID
     * @param edgeId  边定义ID
     * @return 是否成功
     */
    @DeleteMapping("/edges/{edgeId}")
    public Result<String> deleteEdgeDef(@PathVariable Long graphId, @PathVariable Long edgeId) {
        boolean success = edgeDefService.deleteEdgeDefWithProperties(edgeId);
        if (success) {
            return Result.success("删除边定义成功");
        } else {
            return Result.error("删除边定义失败");
        }
    }

    /**
     * 发布图Schema到图数据库
     *
     * @param graphId 图ID
     * @return 发布结果
     */
    @PostMapping("/publish")
    public Result<String> publishSchema(@PathVariable Long graphId) {
        graphSchemaService.publishSchema(graphId);
        return Result.success(null);
    }

    @GetMapping("/schema")
    public Result<GraphSchema> getSchema(@PathVariable Long graphId) {
        return Result.success(graphSchemaService.getGraphSchema(graphId));
    }

    /**
     * 导出图Schema（节点定义和边定义）
     *
     * @param graphId 图ID
     * @return Schema导出数据
     */
    @GetMapping("/schema/export")
    public Result<SchemaExportDTO> exportSchema(@PathVariable Long graphId) {
        return Result.success(graphSchemaService.exportSchema(graphId));
    }

    /**
     * 导入图Schema
     *
     * @param graphId   图ID
     * @param importDTO 导入数据
     * @return 导入结果
     */
    @PostMapping("/schema/import")
    public Result<String> importSchema(@PathVariable Long graphId, @RequestBody SchemaImportDTO importDTO) {
        graphSchemaService.importSchema(graphId, importDTO);
        return Result.success("导入成功");
    }

    /**
     * 从图数据库发现节点定义
     */
    private List<GraphVertexDef> discoverVertexDefs(Long graphId, Long connectionId, String graphCode) {
        GraphSchema schema;
        if (connectionId != null && graphCode != null) {
            schema = graphSchemaService.discoverSchema(connectionId, graphCode);
        } else {
            schema = graphSchemaService.discoverSchema(graphId);
        }
        if (schema == null || schema.getEntities() == null || schema.getEntities().isEmpty()) {
            return new ArrayList<>();
        }
        AtomicLong idCounter = new AtomicLong(-1);
        return schema.getEntities().stream().map(entity -> {
            GraphVertexDef vertexDef = new GraphVertexDef();
            vertexDef.setId(idCounter.decrementAndGet());
            vertexDef.setGraphId(graphId);
            vertexDef.setLabel(entity.getLabel());
            vertexDef.setName(entity.getLabel());
            vertexDef.setDescription("从图数据库发现");
            vertexDef.setStatus(1);
            if (entity.getProperties() != null) {
                List<GraphPropertyDef> props = entity.getProperties().stream()
                        .map(this::buildPropertyDef)
                        .collect(Collectors.toList());
                vertexDef.setProperties(props);
            }
            return vertexDef;
        }).collect(Collectors.toList());
    }

    /**
     * 从图数据库发现边定义
     */
    private List<GraphEdgeDef> discoverEdgeDefs(Long graphId, Long connectionId, String graphCode) {
        GraphSchema schema;
        if (connectionId != null && graphCode != null) {
            schema = graphSchemaService.discoverSchema(connectionId, graphCode);
        } else {
            schema = graphSchemaService.discoverSchema(graphId);
        }
        if (schema == null || schema.getRelations() == null || schema.getRelations().isEmpty()) {
            return new ArrayList<>();
        }
        AtomicLong idCounter = new AtomicLong(-1000);
        return schema.getRelations().stream().map(relation -> {
            GraphEdgeDef edgeDef = new GraphEdgeDef();
            edgeDef.setId(idCounter.decrementAndGet());
            edgeDef.setGraphId(graphId);
            edgeDef.setLabel(relation.getLabel());
            edgeDef.setName(relation.getLabel());
            edgeDef.setDescription("从图数据库发现");
            edgeDef.setStatus(1);
            edgeDef.setMultiple(relation.getMultiple());
            // 直接存储标签名
            edgeDef.setStartLabel(relation.getStartLabel());
            edgeDef.setEndLabel(relation.getEndLabel());
            if (relation.getProperties() != null) {
                List<GraphPropertyDef> props = relation.getProperties().stream()
                        .map(p -> buildPropertyDef(p))
                        .collect(Collectors.toList());
                edgeDef.setProperties(props);
            }
            return edgeDef;
        }).collect(Collectors.toList());
    }

    /**
     * 合并从图数据库发现的节点属性到已有的节点定义中
     */
    private void mergeDiscoveredVertexProperties(List<GraphVertexDef> vertexDefs, Long graphId) {
        GraphSchema schema = graphSchemaService.discoverSchema(graphId);
        if (schema == null || schema.getEntities() == null) {
            return;
        }
        Map<String, List<GraphProperty>> labelPropsMap = schema.getEntities().stream()
                .collect(Collectors.toMap(GraphEntity::getLabel, GraphEntity::getProperties, (a, b) -> a));
        for (GraphVertexDef vertexDef : vertexDefs) {
            if (vertexDef.getLabel() != null) {
                List<GraphProperty> discovered = labelPropsMap.get(vertexDef.getLabel());
                if (discovered != null && !discovered.isEmpty()) {
                    List<GraphPropertyDef> existing = vertexDef.getProperties() != null
                            ? vertexDef.getProperties() : new ArrayList<>();
                    for (GraphProperty p : discovered) {
                        boolean exists = existing.stream()
                                .anyMatch(e -> p.getCode().equals(e.getCode()));
                        if (!exists) {
                            existing.add(buildPropertyDef(p));
                        }
                    }
                    vertexDef.setProperties(existing);
                }
            }
        }
    }

    /**
     * 合并从图数据库发现的边属性到已有的边定义中
     */
    private void mergeDiscoveredEdgeProperties(List<GraphEdgeDef> edgeDefs, Long graphId) {
        GraphSchema schema = graphSchemaService.discoverSchema(graphId);
        if (schema == null || schema.getRelations() == null) {
            return;
        }
        Map<String, List<GraphProperty>> labelPropsMap = schema.getRelations().stream()
                .collect(Collectors.toMap(GraphRelation::getLabel, GraphRelation::getProperties, (a, b) -> a));
        for (GraphEdgeDef edgeDef : edgeDefs) {
            if (edgeDef.getLabel() != null) {
                List<GraphProperty> discovered = labelPropsMap.get(edgeDef.getLabel());
                if (discovered != null && !discovered.isEmpty()) {
                    List<GraphPropertyDef> existing = edgeDef.getProperties() != null
                            ? edgeDef.getProperties() : new ArrayList<>();
                    for (GraphProperty p : discovered) {
                        boolean exists = existing.stream()
                                .anyMatch(e -> p.getCode().equals(e.getCode()));
                        if (!exists) {
                            existing.add(buildPropertyDef(p));
                        }
                    }
                    edgeDef.setProperties(existing);
                }
            }
        }
    }

    /**
     * 将 GraphProperty 转换为 GraphPropertyDef
     */
    private GraphPropertyDef buildPropertyDef(GraphProperty p) {
        GraphPropertyDef prop = new GraphPropertyDef();
        prop.setCode(p.getCode());
        prop.setName(p.getName());
        prop.setType(p.getDataType() != null ? p.getDataType().name() : "String");
        prop.setStatus(1);
        prop.setIndexed(false);
        return prop;
    }
}