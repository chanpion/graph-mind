package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphEntityDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图Schema管理控制器
 *
 * @author April.Chen
 * @date 2025/8/4 16:10
 */
@Slf4j
@RestController
@RequestMapping("/api/graphs/schema")
public class GraphSchemaController {

    @Autowired
    private GraphVertexDefService vertexDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphSchemaService graphSchemaService;

    @GetMapping("/vertices")
    public Result<List<GraphVertexDef>> getVertexDefs(
            @RequestParam Long graphId, Integer status,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        List<GraphVertexDef> vertexDefs = vertexDefService.getVertexDefsByGraphId(graphId, status);
        if (vertexDefs == null) {
            vertexDefs = new ArrayList<>();
        }

        try {
            List<GraphVertexDef> discovered = graphSchemaService.discoverVertexDefs(graphId, connectionId, graphCode);
            mergeDiscoveredDefs(vertexDefs, discovered);
        } catch (Exception e) {
            log.warn("从图数据库发现节点类型失败", e);
        }

        return Result.success(vertexDefs);
    }

    /**
     * 新增节点定义
     */
    @PostMapping("/vertices")
    public Result<String> addVertexDef(@RequestParam Long graphId, @RequestBody GraphVertexDef vertexDef) {
        vertexDef.setGraphId(graphId);
        boolean success = vertexDefService.saveVertexDefWithProperties(vertexDef);
        if (success) {
            graphSchemaService.publishSchema(graphId, null, null);
            return Result.success("新增节点定义成功");
        }
        return Result.error("新增节点定义失败");
    }

    /**
     * 新增或更新节点定义
     */
    @PutMapping("/vertex")
    public Result<String> updateVertexDef(
            @RequestParam Long vertexId,
            @RequestParam(required = false) Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody GraphVertexDef vertexDef) {
        boolean success;
        if (vertexId != null && vertexId > 0) {
            vertexDef.setId(vertexId);
            success = vertexDefService.updateVertexDefWithProperties(vertexDef);
        } else {
            vertexDef.setId(null);
            success = vertexDefService.saveVertexDefWithProperties(vertexDef);
        }

        if (success) {
            Long targetGraphId = vertexDef.getGraphId() != null ? vertexDef.getGraphId() : graphId;
            graphSchemaService.publishSchema(targetGraphId, connectionId, graphCode);
            return Result.success(vertexId != null && vertexId > 0 ? "更新节点定义成功" : "新增节点定义成功");
        }
        return Result.error(vertexId != null && vertexId > 0 ? "更新节点定义失败" : "新增节点定义失败");
    }

    @DeleteMapping("/vertex")
    public Result<String> deleteVertexDef(@RequestParam Long graphId, @RequestParam Long vertexId) {
        boolean success = graphSchemaService.deleteVertexDef(graphId, vertexId);
        if (success) {
            return Result.success("删除节点定义成功");
        }
        return Result.error("删除节点定义失败");
    }

    @GetMapping("/edges")
    public Result<List<GraphEdgeDef>> getEdgeDefs(
            @RequestParam Long graphId, Integer status,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        List<GraphEdgeDef> edgeDefs = edgeDefService.getEdgeDefsByGraphId(graphId, status);
        if (edgeDefs == null) {
            edgeDefs = new ArrayList<>();
        }

        try {
            List<GraphEdgeDef> discovered = graphSchemaService.discoverEdgeDefs(graphId, connectionId, graphCode);
            mergeDiscoveredDefs(edgeDefs, discovered);
        } catch (Exception e) {
            log.warn("从图数据库发现边类型失败", e);
        }

        return Result.success(edgeDefs);
    }

    @PostMapping("/edges")
    public Result<String> addEdgeDef(@RequestParam Long graphId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.saveEdgeDefWithProperties(edgeDef);
        if (success) {
            graphSchemaService.publishSchema(graphId, null, null);
            return Result.success("新增边定义成功");
        }
        return Result.error("新增边定义失败");
    }

    @PutMapping("/edge")
    public Result<String> updateEdgeDef(@RequestParam Long graphId, @RequestParam Long edgeId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setId(edgeId);
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.updateEdgeDefWithProperties(edgeDef);
        if (success) {
            graphSchemaService.publishSchema(graphId, null, null);
            return Result.success("更新边定义成功");
        }
        return Result.error("更新边定义失败");
    }

    @DeleteMapping("/edge")
    public Result<String> deleteEdgeDef(@RequestParam Long graphId, @RequestParam Long edgeId) {
        boolean success = graphSchemaService.deleteEdgeDef(graphId, edgeId);
        if (success) {
            return Result.success("删除边定义成功");
        }
        return Result.error("删除边定义失败");
    }

    @PostMapping("/publish")
    public Result<String> publishSchema(@RequestParam Long graphId) {
        try {
            graphSchemaService.publishSchema(graphId, null, null);
            return Result.success("发布成功");
        } catch (Exception e) {
            log.error("发布图Schema失败，graphId={}", graphId, e);
            return Result.error(500, "发布Schema失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/export")
    public Result<SchemaExportDTO> exportSchema(@RequestParam Long graphId) {
        return Result.success(graphSchemaService.exportSchema(graphId));
    }

    @PostMapping("/import")
    public Result<String> importSchema(@RequestParam Long graphId, @RequestBody SchemaImportDTO importDTO) {
        graphSchemaService.importSchema(graphId, importDTO);
        return Result.success("导入成功");
    }

    /**
     * 合并发现的实体定义到本地列表，包括新增类型和补全属性
     */
    private <T extends GraphEntityDef> void mergeDiscoveredDefs(List<T> localDefs, List<T> discovered) {
        if (discovered == null || discovered.isEmpty()) {
            return;
        }

        Set<String> localLabels = localDefs.stream()
                .map(GraphEntityDef::getLabel)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 补充图库原有的，平台未创建的类型
        for (T d : discovered) {
            if (d.getLabel() != null && !localLabels.contains(d.getLabel())) {
                localDefs.add(d);
                localLabels.add(d.getLabel());
            }
        }

        // 合并属性：对本地已有类型，补全图库中存在的属性
        Map<String, List<GraphPropertyDef>> discoveredProps = discovered.stream()
                .filter(d -> d.getLabel() != null && d.getProperties() != null)
                .collect(Collectors.toMap(GraphEntityDef::getLabel, GraphEntityDef::getProperties, (a, b) -> a));
        for (T def : localDefs) {
            if (def.getLabel() == null || !discoveredProps.containsKey(def.getLabel())) {
                continue;
            }
            List<GraphPropertyDef> existing = def.getProperties() != null
                    ? new ArrayList<>(def.getProperties()) : new ArrayList<>();
            Set<String> existingCodes = existing.stream()
                    .map(GraphPropertyDef::getCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (GraphPropertyDef p : discoveredProps.get(def.getLabel())) {
                if (p.getCode() != null && !existingCodes.contains(p.getCode())) {
                    existing.add(p);
                }
            }
            def.setProperties(existing);
        }
    }
}
