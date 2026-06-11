package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
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
            if (discovered != null && !discovered.isEmpty()) {
                // 按 label 建立本地类型索引
                Set<String> localLabels = vertexDefs.stream()
                        .map(GraphVertexDef::getLabel)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // 补充本地缺失的类型（图库原有的，平台未创建）
                for (GraphVertexDef d : discovered) {
                    if (d.getLabel() != null && !localLabels.contains(d.getLabel())) {
                        vertexDefs.add(d);
                        localLabels.add(d.getLabel());
                    }
                }

                // 合并属性：对本地已有类型，补全图库中存在的属性
                Map<String, List<GraphPropertyDef>> discoveredProps = discovered.stream()
                        .filter(d -> d.getLabel() != null && d.getProperties() != null)
                        .collect(Collectors.toMap(GraphVertexDef::getLabel, GraphVertexDef::getProperties));
                for (GraphVertexDef vd : vertexDefs) {
                    if (vd.getLabel() != null && discoveredProps.containsKey(vd.getLabel())) {
                        List<GraphPropertyDef> existing = vd.getProperties() != null
                                ? vd.getProperties() : new ArrayList<>();
                        Set<String> existingCodes = existing.stream()
                                .map(GraphPropertyDef::getCode)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        for (GraphPropertyDef p : discoveredProps.get(vd.getLabel())) {
                            if (p.getCode() != null && !existingCodes.contains(p.getCode())) {
                                existing.add(p);
                            }
                        }
                        vd.setProperties(existing);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从图数据库发现节点类型失败: {}", e.getMessage());
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
            graphSchemaService.publishSchema(graphId);
            return Result.success("新增节点定义成功");
        }
        return Result.error("新增节点定义失败");
    }

    /**
     * 更新节点定义
     */
    @PutMapping("/vertex")
    public Result<String> updateVertexDef(@RequestParam Long graphId, @RequestParam Long vertexId, @RequestBody GraphVertexDef vertexDef) {
        vertexDef.setId(vertexId);
        vertexDef.setGraphId(graphId);
        boolean success = vertexDefService.updateVertexDefWithProperties(vertexDef);
        if (success) {
            graphSchemaService.publishSchema(graphId);
            return Result.success("更新节点定义成功");
        }
        return Result.error("更新节点定义失败");
    }

    /**
     * 删除节点定义
     */
    @DeleteMapping("/vertex")
    public Result<String> deleteVertexDef(@RequestParam Long graphId, @RequestParam Long vertexId) {
        boolean success = vertexDefService.deleteVertexDefWithProperties(vertexId);
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
            if (discovered != null && !discovered.isEmpty()) {
                // 按 label 建立本地类型索引
                Set<String> localLabels = edgeDefs.stream()
                        .map(GraphEdgeDef::getLabel)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // 补充本地缺失的类型（图库原有的，平台未创建）
                for (GraphEdgeDef d : discovered) {
                    if (d.getLabel() != null && !localLabels.contains(d.getLabel())) {
                        edgeDefs.add(d);
                        localLabels.add(d.getLabel());
                    }
                }

                // 合并属性：对本地已有类型，补全图库中存在的属性
                Map<String, List<GraphPropertyDef>> discoveredProps = discovered.stream()
                        .filter(d -> d.getLabel() != null && d.getProperties() != null)
                        .collect(Collectors.toMap(GraphEdgeDef::getLabel, GraphEdgeDef::getProperties));
                for (GraphEdgeDef ed : edgeDefs) {
                    if (ed.getLabel() != null && discoveredProps.containsKey(ed.getLabel())) {
                        List<GraphPropertyDef> existing = ed.getProperties() != null
                                ? ed.getProperties() : new ArrayList<>();
                        Set<String> existingCodes = existing.stream()
                                .map(GraphPropertyDef::getCode)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        for (GraphPropertyDef p : discoveredProps.get(ed.getLabel())) {
                            if (p.getCode() != null && !existingCodes.contains(p.getCode())) {
                                existing.add(p);
                            }
                        }
                        ed.setProperties(existing);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从图数据库发现边类型失败: {}", e.getMessage());
        }

        return Result.success(edgeDefs);
    }

    /**
     * 新增边定义
     */
    @PostMapping("/edges")
    public Result<String> addEdgeDef(@RequestParam Long graphId, @RequestBody GraphEdgeDef edgeDef) {
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
    @PutMapping("/edge")
    public Result<String> updateEdgeDef(@RequestParam Long graphId, @RequestParam Long edgeId, @RequestBody GraphEdgeDef edgeDef) {
        edgeDef.setId(edgeId);
        edgeDef.setGraphId(graphId);
        boolean success = edgeDefService.updateEdgeDefWithProperties(edgeDef);
        if (success) {
            graphSchemaService.publishSchema(graphId);
            return Result.success("更新边定义成功");
        }
        return Result.error("更新边定义失败");
    }

    /**
     * 删除边定义
     */
    @DeleteMapping("/edge")
    public Result<String> deleteEdgeDef(@RequestParam Long graphId, @RequestParam Long edgeId) {
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
    public Result<String> publishSchema(@RequestParam Long graphId) {
        try {
            graphSchemaService.publishSchema(graphId);
            return Result.success("发布成功");
        } catch (Exception e) {
            log.error("发布图Schema失败，graphId={}", graphId, e);
            return Result.error(500, "发布Schema失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/schema")
    public Result<GraphSchema> getSchema(@RequestParam Long graphId) {
        return Result.success(graphSchemaService.getGraphSchema(graphId));
    }

    /**
     * 导出图Schema
     */
    @GetMapping("/schema/export")
    public Result<SchemaExportDTO> exportSchema(@RequestParam Long graphId) {
        return Result.success(graphSchemaService.exportSchema(graphId));
    }

    /**
     * 导入图Schema
     */
    @PostMapping("/schema/import")
    public Result<String> importSchema(@RequestParam Long graphId, @RequestBody SchemaImportDTO importDTO) {
        graphSchemaService.importSchema(graphId, importDTO);
        return Result.success("导入成功");
    }
}
