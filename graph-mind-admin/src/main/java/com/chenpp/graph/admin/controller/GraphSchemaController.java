package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 获取节点定义列表，元数据为空时自动从图数据库发现
     */
    @GetMapping("/vertices")
    public Result<List<GraphVertexDef>> getVertexDefs(
            @PathVariable Long graphId, Integer status,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        List<GraphVertexDef> vertexDefs = vertexDefService.getVertexDefsByGraphId(graphId, status);
        if (vertexDefs == null || vertexDefs.isEmpty()) {
            vertexDefs = graphSchemaService.discoverVertexDefs(graphId, connectionId, graphCode);
        } else {
            boolean hasEmptyProperties = vertexDefs.stream()
                    .anyMatch(n -> n.getProperties() == null || n.getProperties().isEmpty());
            if (hasEmptyProperties && connectionId != null && graphCode != null) {
                try {
                    graphSchemaService.mergeDiscoveredVertexProperties(vertexDefs, graphId, connectionId, graphCode);
                } catch (Exception e) {
                    log.warn("合并节点属性失败: {}", e.getMessage());
                }
            }
        }
        return Result.success(vertexDefs);
    }

    /**
     * 新增节点定义
     */
    @PostMapping("/vertices")
    public Result<String> addVertexDef(@PathVariable Long graphId, @RequestBody GraphVertexDef vertexDef) {
        vertexDef.setGraphId(graphId);
        boolean success = vertexDefService.saveVertexDefWithProperties(vertexDef);
        if (success) {
            graphSchemaService.publishSchema(graphId);
            return Result.success("新增节点定义成功");
        }
        return Result.error("新增节点定义失败");
    }

    /**
     * 更新节点定义
     */
    @PutMapping("/vertices/{vertexId}")
    public Result<String> updateVertexDef(@PathVariable Long graphId, @PathVariable Long vertexId, @RequestBody GraphVertexDef vertexDef) {
        vertexDef.setId(vertexId);
        vertexDef.setGraphId(graphId);
        boolean success = vertexDefService.updateVertexDefWithProperties(vertexDef);
        if (success) {
            return Result.success("更新节点定义成功");
        }
        return Result.error("更新节点定义失败");
    }

    /**
     * 删除节点定义
     */
    @DeleteMapping("/vertices/{vertexId}")
    public Result<String> deleteVertexDef(@PathVariable Long graphId, @PathVariable Long vertexId) {
        boolean success = vertexDefService.deleteVertexDefWithProperties(vertexId);
        if (success) {
            return Result.success("删除节点定义成功");
        }
        return Result.error("删除节点定义失败");
    }

    /**
     * 获取边定义列表，元数据为空时自动从图数据库发现
     */
    @GetMapping("/edges")
    public Result<List<GraphEdgeDef>> getEdgeDefs(
            @PathVariable Long graphId, Integer status,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        List<GraphEdgeDef> edgeDefs = edgeDefService.getEdgeDefsByGraphId(graphId, status);
        if (edgeDefs == null || edgeDefs.isEmpty()) {
            edgeDefs = graphSchemaService.discoverEdgeDefs(graphId, connectionId, graphCode);
        } else {
            boolean hasEmptyProperties = edgeDefs.stream()
                    .anyMatch(e -> e.getProperties() == null || e.getProperties().isEmpty());
            if (hasEmptyProperties && connectionId != null && graphCode != null) {
                try {
                    graphSchemaService.mergeDiscoveredEdgeProperties(edgeDefs, graphId, connectionId, graphCode);
                } catch (Exception e) {
                    log.warn("合并边属性失败: {}", e.getMessage());
                }
            }
        }
        return Result.success(edgeDefs);
    }

    /**
     * 新增边定义
     */
    @PostMapping("/edges")
    public Result<String> addEdgeDef(@PathVariable Long graphId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.saveEdgeDefWithProperties(edgeDef);
        if (success) {
            graphSchemaService.publishSchema(graphId);
            return Result.success("新增边定义成功");
        }
        return Result.error("新增边定义失败");
    }

    /**
     * 更新边定义
     */
    @PutMapping("/edges/{edgeId}")
    public Result<String> updateEdgeDef(@PathVariable Long graphId, @PathVariable Long edgeId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setId(edgeId);
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.updateEdgeDefWithProperties(edgeDef);
        if (success) {
            return Result.success("更新边定义成功");
        }
        return Result.error("更新边定义失败");
    }

    /**
     * 删除边定义
     */
    @DeleteMapping("/edges/{edgeId}")
    public Result<String> deleteEdgeDef(@PathVariable Long graphId, @PathVariable Long edgeId) {
        boolean success = edgeDefService.deleteEdgeDefWithProperties(edgeId);
        if (success) {
            return Result.success("删除边定义成功");
        }
        return Result.error("删除边定义失败");
    }

    /**
     * 发布图Schema到图数据库
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
     * 导出图Schema
     */
    @GetMapping("/schema/export")
    public Result<SchemaExportDTO> exportSchema(@PathVariable Long graphId) {
        return Result.success(graphSchemaService.exportSchema(graphId));
    }

    /**
     * 导入图Schema
     */
    @PostMapping("/schema/import")
    public Result<String> importSchema(@PathVariable Long graphId, @RequestBody SchemaImportDTO importDTO) {
        graphSchemaService.importSchema(graphId, importDTO);
        return Result.success("导入成功");
    }
}
