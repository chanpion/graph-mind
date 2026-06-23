package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.service.GraphDataService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.core.exception.BusinessException;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.GraphEdge;
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
            @RequestParam Long graphId,
            @RequestParam Long vertexTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("config") String config) {
        ImportResult result = graphDataService.importVertexData(graphId, vertexTypeId, file, config);
        return Result.success(result);
    }

    @PostMapping("/importEdges")
    public Result<ImportResult> importEdgeData(
            @RequestParam Long graphId,
            @RequestParam Long edgeTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("config") String config) {
        ImportResult result = graphDataService.importEdgeData(graphId, edgeTypeId, file, config);
        return Result.success(result);
    }

    /**
     * 查询顶点数据列表
     * 对于发现的图（vertexTypeId < 0），需通过 connectionId + graphCode 发现 label
     */
    @GetMapping("/vertices")
    public Result<PageResult<GraphVertex>> getVertexDataList(
            @RequestParam Long graphId,
            @RequestParam Long vertexTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String label = null;
        // 对于发现的图（负ID），从 schema discovery 获取 label
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

    /**
     * 查询边数据列表
     * 对于发现的图（edgeTypeId < 0），需通过 connectionId + graphCode 发现 label
     */
    @GetMapping("/edges")
    public Result<PageResult<Map<String, Object>>> getEdgeDataList(
            @RequestParam Long graphId,
            @RequestParam Long edgeTypeId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String label = null;
        // 对于发现的图（负ID），从 schema discovery 获取 label
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

        PageResult<Map<String, Object>> data = graphDataService.queryEdgeDataList(graphId, edgeTypeId, label, page, size, connectionId, graphCode);
        return Result.success(data);
    }

    @GetMapping("/vertex")
    public Result<GraphVertex> getVertexData(
            @RequestParam Long graphId,
            @RequestParam String vertexId) {
        GraphVertex data = graphDataService.getVertexData(graphId, vertexId);
        return Result.success(data);
    }

    @GetMapping("/edge")
    public Result<GraphEdge> getEdgeData(
            @RequestParam Long graphId,
            @RequestParam String edgeId) {
        GraphEdge data = graphDataService.getEdgeData(graphId, edgeId);
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "新增顶点数据失败");
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "新增边数据失败");
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "更新顶点数据失败");
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "更新边数据失败");
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "删除顶点失败");
        }
        return Result.success(true);
    }

    /**
     * 批量删除顶点
     */
    @DeleteMapping("/vertices")
    public Result<Boolean> deleteVertices(
            @RequestParam Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody Map<String, Object> request) {
        List<String> vertexIds = (List<String>) request.get("vertexIds");
        if (vertexIds == null || vertexIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "顶点ID列表不能为空");
        }
        boolean result = graphDataService.deleteVertices(graphId, vertexIds, request.get("label").toString(), connectionId, graphCode);
        if (!result) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "批量删除顶点失败");
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "删除边失败");
        }
        return Result.success(true);
    }

    /**
     * 批量删除边
     */
    @DeleteMapping("/edges")
    public Result<Boolean> deleteEdges(
            @RequestParam Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode,
            @RequestBody Map<String, Object> request) {
        List<String> edgeIds = (List<String>) request.get("edgeIds");
        if (edgeIds == null || edgeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "边ID列表不能为空");
        }
        boolean result = graphDataService.deleteEdges(graphId, edgeIds, request.get("label").toString(), connectionId, graphCode);
        if (!result) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "批量删除边失败");
        }
        return Result.success(true);
    }

    /**
     * 获取图统计信息
     */
    @GetMapping("/summary")
    public Result<GraphSummary> getGraphSummary(
            @RequestParam Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        GraphSummary summary = graphDataService.getGraphSummary(graphId, connectionId, graphCode);
        return Result.success(summary);
    }
}