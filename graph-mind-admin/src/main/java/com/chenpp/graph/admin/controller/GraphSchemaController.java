package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphNodeDefService;
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
    private GraphNodeDefService nodeDefService;

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
    @GetMapping("/nodes")
    public Result<List<GraphNodeDef>> getNodeDefs(@PathVariable Long graphId, Integer status) {
        List<GraphNodeDef> nodeDefs = nodeDefService.getNodeDefsByGraphId(graphId, status);
        // 元数据为空时，从图数据库发现
        if (nodeDefs == null || nodeDefs.isEmpty()) {
            try {
                nodeDefs = discoverNodeDefs(graphId);
            } catch (Exception e) {
                log.warn("从图数据库发现节点定义失败: {}", e.getMessage());
                nodeDefs = new ArrayList<>();
            }
        } else {
            // 有定义但属性为空时，尝试从图数据库发现并合并属性
            boolean hasEmptyProperties = nodeDefs.stream()
                .anyMatch(n -> n.getProperties() == null || n.getProperties().isEmpty());
            if (hasEmptyProperties) {
                try {
                    mergeDiscoveredNodeProperties(nodeDefs, graphId);
                } catch (Exception e) {
                    log.warn("合并从图数据库发现的节点属性失败: {}", e.getMessage());
                }
            }
        }
        return Result.success(nodeDefs);
    }

    /**
     * 新增节点定义
     *
     * @param graphId 图ID
     * @param nodeDef 节点定义信息
     * @return 是否成功
     */
    @PostMapping("/nodes")
    public Result<String> addNodeDef(@PathVariable Long graphId, @RequestBody GraphNodeDef nodeDef) {
        nodeDef.setGraphId(graphId);
        boolean success = nodeDefService.saveNodeDefWithProperties(nodeDef);
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
     * @param graphId 图ID
     * @param nodeId  节点定义ID
     * @param nodeDef 节点定义信息
     * @return 是否成功
     */
    @PutMapping("/nodes/{nodeId}")
    public Result<String> updateNodeDef(@PathVariable Long graphId, @PathVariable Long nodeId, @RequestBody GraphNodeDef nodeDef) {
        nodeDef.setId(nodeId);
        nodeDef.setGraphId(graphId);
        boolean success = nodeDefService.updateNodeDefWithProperties(nodeDef);
        if (success) {
            return Result.success("更新节点定义成功");
        } else {
            return Result.error("更新节点定义失败");
        }
    }

    /**
     * 删除节点定义
     *
     * @param graphId 图ID
     * @param nodeId  节点定义ID
     * @return 是否成功
     */
    @DeleteMapping("/nodes/{nodeId}")
    public Result<String> deleteNodeDef(@PathVariable Long graphId, @PathVariable Long nodeId) {
        boolean success = nodeDefService.deleteNodeDefWithProperties(nodeId);
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
    public Result<List<GraphEdgeDef>> getEdgeDefs(@PathVariable Long graphId, Integer status) {
        List<GraphEdgeDef> edgeDefs = edgeDefService.getEdgeDefsByGraphId(graphId, status);
        // 元数据为空时，从图数据库发现
        if (edgeDefs == null || edgeDefs.isEmpty()) {
            try {
                edgeDefs = discoverEdgeDefs(graphId);
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
    private List<GraphNodeDef> discoverNodeDefs(Long graphId) {
        GraphSchema schema = graphSchemaService.discoverSchema(graphId);
        if (schema == null || schema.getEntities() == null || schema.getEntities().isEmpty()) {
            return new ArrayList<>();
        }
        AtomicLong idCounter = new AtomicLong(-1);
        return schema.getEntities().stream().map(entity -> {
            GraphNodeDef nodeDef = new GraphNodeDef();
            nodeDef.setId(idCounter.decrementAndGet());
            nodeDef.setGraphId(graphId);
            nodeDef.setLabel(entity.getLabel());
            nodeDef.setName(entity.getLabel());
            nodeDef.setDescription("从图数据库发现");
            nodeDef.setStatus(1);
            if (entity.getProperties() != null) {
                List<GraphPropertyDef> props = entity.getProperties().stream()
                    .map(this::buildPropertyDef)
                    .collect(Collectors.toList());
                nodeDef.setProperties(props);
            }
            return nodeDef;
        }).collect(Collectors.toList());
    }

    /**
     * 从图数据库发现边定义
     */
    private List<GraphEdgeDef> discoverEdgeDefs(Long graphId) {
        GraphSchema schema = graphSchemaService.discoverSchema(graphId);
        if (schema == null || schema.getRelations() == null || schema.getRelations().isEmpty()) {
            return new ArrayList<>();
        }
        // 构建节点标签→ID映射（使用负ID，与discoverNodeDefs一致）
        Map<String, Long> labelIdMap = new HashMap<>();
        if (schema.getEntities() != null) {
            AtomicLong counter = new AtomicLong(-1);
            for (GraphEntity entity : schema.getEntities()) {
                labelIdMap.put(entity.getLabel(), counter.decrementAndGet());
            }
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
            // 通过标签映射设置起点/终点节点ID
            Long fromId = labelIdMap.get(relation.getSourceLabel());
            edgeDef.setFrom(fromId != null ? String.valueOf(fromId) : relation.getSourceLabel());
            Long toId = labelIdMap.get(relation.getTargetLabel());
            edgeDef.setTo(toId != null ? String.valueOf(toId) : relation.getTargetLabel());
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
    private void mergeDiscoveredNodeProperties(List<GraphNodeDef> nodeDefs, Long graphId) {
        GraphSchema schema = graphSchemaService.discoverSchema(graphId);
        if (schema == null || schema.getEntities() == null) {
            return;
        }
        Map<String, List<GraphProperty>> labelPropsMap = schema.getEntities().stream()
            .collect(Collectors.toMap(GraphEntity::getLabel, GraphEntity::getProperties, (a, b) -> a));
        for (GraphNodeDef nodeDef : nodeDefs) {
            if (nodeDef.getLabel() != null) {
                List<GraphProperty> discovered = labelPropsMap.get(nodeDef.getLabel());
                if (discovered != null && !discovered.isEmpty()) {
                    List<GraphPropertyDef> existing = nodeDef.getProperties() != null
                        ? nodeDef.getProperties() : new ArrayList<>();
                    for (GraphProperty p : discovered) {
                        boolean exists = existing.stream()
                            .anyMatch(e -> p.getCode().equals(e.getCode()));
                        if (!exists) {
                            existing.add(buildPropertyDef(p));
                        }
                    }
                    nodeDef.setProperties(existing);
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