package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.service.GraphDataService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 图数据管理API
 *
 * @author April.Chen
 * @date 2025/8/11 11:00
 */
@SuppressWarnings("unchecked")
@RestController
@RequestMapping("/api/graph/data")
public class GraphDataController {

    @Autowired
    private GraphDataService graphDataService;

    @Autowired
    private GraphSchemaService graphSchemaService;

    @PostMapping("/importVertices")
    public Result<ImportResult> importVertexData(
            @RequestParam(required = false) Long graphId,
            @RequestParam(required = false) Long vertexTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestParam(required = false) String label,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("CSV文件不能为空");
        }
        if (!((connectionId != null && graphCode != null && label != null) || (graphId != null && vertexTypeId != null))) {
            return Result.error("需要提供 (graphId + vertexTypeId) 或 (connectionId + graphCode + label)");
        }
        ImportResult result = graphDataService.importVertexData(graphId, vertexTypeId, connectionId, graphCode, label, file);
        return Result.success(result);
    }

    @PostMapping("/importEdges")
    public Result<ImportResult> importEdgeData(
            @RequestParam(required = false) Long graphId,
            @RequestParam(required = false) Long edgeTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestParam(required = false) String label,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("CSV文件不能为空");
        }
        if (!((connectionId != null && graphCode != null && label != null) || (graphId != null && edgeTypeId != null))) {
            return Result.error("需要提供 (graphId + edgeTypeId) 或 (connectionId + graphCode + label)");
        }
        ImportResult result = graphDataService.importEdgeData(graphId, edgeTypeId, connectionId, graphCode, label, file);
        return Result.success(result);
    }

    @GetMapping("/vertices")
    public Result<PageResult<GraphVertex>> getVertexDataList(
            @RequestParam Long graphId,
            @RequestParam Long vertexTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String label = null;
        if (vertexTypeId != null && vertexTypeId < 0) {
            if (connectionId != null && graphCode != null) {
                List<GraphVertexDef> vertexDefs = graphSchemaService.discoverVertexDefs(graphId, connectionId, graphCode);
                for (GraphVertexDef vd : vertexDefs) {
                    if (vd.getId().equals(vertexTypeId)) {
                        label = vd.getLabel();
                        break;
                    }
                }
            }
            if (label == null) {
                return Result.success(PageResult.empty(page, size));
            }
        }

        PageResult<GraphVertex> data = graphDataService.queryVertexDataList(graphId, vertexTypeId, label, page, size, connectionId, graphCode);
        return Result.success(data);
    }

    @GetMapping("/edges")
    public Result<PageResult<GraphEdge>> getEdgeDataList(
            @RequestParam Long graphId,
            @RequestParam Long edgeTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String label = null;
        if (edgeTypeId != null && edgeTypeId < 0) {
            if (connectionId != null && graphCode != null) {
                List<GraphEdgeDef> edgeDefs = graphSchemaService.discoverEdgeDefs(graphId, connectionId, graphCode);
                for (GraphEdgeDef ed : edgeDefs) {
                    if (ed.getId().equals(edgeTypeId)) {
                        label = ed.getLabel();
                        break;
                    }
                }
            }
            if (label == null) {
                return Result.success(PageResult.empty(page, size));
            }
        }

        PageResult<GraphEdge> data = graphDataService.queryEdgeDataList(graphId, edgeTypeId, label, page, size, connectionId, graphCode);
        return Result.success(data);
    }

    @PostMapping("/vertex")
    public Result<Boolean> addVertexData(
            @RequestParam Long graphId,
            @RequestParam Long vertexTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody Map<String, Object> data) {
        boolean result = graphDataService.addVertexData(graphId, vertexTypeId, connectionId, graphCode, data);
        if (!result) {
            return Result.error("新增顶点数据失败");
        }
        return Result.success(true);
    }

    @PostMapping("/edge")
    public Result<Boolean> addEdgeData(
            @RequestParam Long graphId,
            @RequestParam Long edgeTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody Map<String, Object> data) {
        boolean result = graphDataService.addEdgeData(graphId, edgeTypeId, connectionId, graphCode, data);
        if (!result) {
            return Result.error("新增边数据失败");
        }
        return Result.success(true);
    }

    @PutMapping("/vertex")
    public Result<Boolean> updateVertexData(
            @RequestParam Long graphId,
            @RequestParam String vertexId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody Map<String, Object> data) {
        boolean result = graphDataService.updateVertexData(graphId, vertexId, connectionId, graphCode, data);
        if (!result) {
            return Result.error("更新顶点数据失败");
        }
        return Result.success(true);
    }

    @PutMapping("/edge")
    public Result<Boolean> updateEdgeData(
            @RequestParam Long graphId,
            @RequestParam String edgeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody Map<String, Object> data) {
        boolean result = graphDataService.updateEdgeData(graphId, edgeId, connectionId, graphCode, data);
        if (!result) {
            return Result.error("更新边数据失败");
        }
        return Result.success(true);
    }

    @DeleteMapping("/vertex")
    public Result<Boolean> deleteVertex(
            @RequestParam Long graphId,
            @RequestParam String vertexId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        boolean result = graphDataService.deleteVertex(graphId, vertexId, label, connectionId, graphCode);
        if (!result) {
            return Result.error("删除顶点失败");
        }
        return Result.success(true);
    }

    @DeleteMapping("/edge")
    public Result<Boolean> deleteEdge(
            @RequestParam Long graphId,
            @RequestParam String edgeId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        boolean result = graphDataService.deleteEdge(graphId, edgeId, label, connectionId, graphCode);
        if (!result) {
            return Result.error("删除边失败");
        }
        return Result.success(true);
    }

    @GetMapping("/summary")
    public Result<GraphSummary> getGraphSummary(
            @RequestParam Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        GraphSummary summary = graphDataService.getGraphSummary(graphId, connectionId, graphCode);
        return Result.success(summary);
    }
}