package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.service.GraphDataService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.GraphEdge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 图数据管理控制器
 * 处理图数据的增删改查和导入相关接口
 *
 * @author April.Chen
 * @date 2025/8/11 11:00
 */
@SuppressWarnings("unchecked")
@Slf4j
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
        try {
            log.info("开始导入节点数据，graphId={}, vertexTypeId={}, config={}", graphId, vertexTypeId, config);
            ImportResult result = graphDataService.importVertexData(graphId, vertexTypeId, file, config);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入节点数据失败", e);
            return Result.error(500, "导入节点数据失败: " + e.getMessage(), null);
        }
    }

    @PostMapping("/importEdges")
    public Result<ImportResult> importEdgeData(
            @RequestParam Long graphId,
            @RequestParam Long edgeTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("config") String config) {
        try {
            log.info("开始导入边数据，graphId={}, edgeTypeId={}, config={}", graphId, edgeTypeId, config);
            ImportResult result = graphDataService.importEdgeData(graphId, edgeTypeId, file, config);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入边数据失败", e);
            return Result.error(500, "导入边数据失败: " + e.getMessage(), null);
        }
    }

    /**
     * 查询节点数据列表
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
        try {
            log.info("查询节点数据列表，graphId={}, vertexTypeId={}, page={}, size={}", graphId, vertexTypeId, page, size);

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
                    log.warn("无法从发现的图中获取节点类型 label，vertexTypeId={}", vertexTypeId);
                    return Result.success(PageResult.empty(page, size));
                }
            }

            PageResult<GraphVertex> data = graphDataService.queryVertexDataList(graphId, vertexTypeId, label, page, size, connectionId, graphCode);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询节点数据列表失败", e);
            return Result.error(500, "查询节点数据列表失败: " + e.getMessage(), null);
        }
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
        try {
            log.info("查询边数据列表，graphId={}, edgeTypeId={}, page={}, size={}", graphId, edgeTypeId, page, size);

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
                    log.warn("无法从发现的图中获取边类型 label，edgeTypeId={}", edgeTypeId);
                    return Result.success(PageResult.empty(page, size));
                }
            }

            PageResult<Map<String, Object>> data = graphDataService.queryEdgeDataList(graphId, edgeTypeId, label, page, size, connectionId, graphCode);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询边数据列表失败", e);
            return Result.error(500, "查询边数据列表失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/vertex")
    public Result<GraphVertex> getVertexData(
            @RequestParam Long graphId,
            @RequestParam String vertexId) {
        try {
            log.info("获取节点数据详情，graphId={}, vertexId={}", graphId, vertexId);

            GraphVertex data = graphDataService.getNodeData(graphId, vertexId);

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取节点数据详情失败", e);
            return Result.error(500, "获取节点数据详情失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/edge")
    public Result<GraphEdge> getEdgeData(
            @RequestParam Long graphId,
            @RequestParam String edgeId) {
        try {
            log.info("获取边数据详情，graphId={}, edgeId={}", graphId, edgeId);
            GraphEdge data = graphDataService.getEdgeData(graphId, edgeId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取边数据详情失败", e);
            return Result.error(500, "获取边数据详情失败: " + e.getMessage(), null);
        }
    }

    @PostMapping("/vertex")
    public Result<Boolean> addVertexData(
            @RequestParam Long graphId,
            @RequestParam Long vertexTypeId,
            @RequestBody Map<String, Object> data) {
        try {
            log.info("新增节点数据，graphId={}, vertexTypeId={}, data={}", graphId, vertexTypeId, data);
            boolean result = graphDataService.addNodeData(graphId, vertexTypeId, data);
            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "新增节点数据失败", false);
            }
        } catch (Exception e) {
            log.error("新增节点数据失败", e);
            return Result.error(500, "新增节点数据失败: " + e.getMessage(), false);
        }
    }

    @PostMapping("/edge")
    public Result<Boolean> addEdgeData(
            @RequestParam Long graphId,
            @RequestParam Long edgeTypeId,
            @RequestBody Map<String, Object> data) {
        try {
            log.info("新增边数据，graphId={}, edgeTypeId={}, data={}", graphId, edgeTypeId, data);
            boolean result = graphDataService.addEdgeData(graphId, edgeTypeId, data);
            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "新增边数据失败", false);
            }
        } catch (Exception e) {
            log.error("新增边数据失败", e);
            return Result.error(500, "新增边数据失败: " + e.getMessage(), false);
        }
    }

    @PutMapping("/vertex")
    public Result<Boolean> updateVertexData(
            @RequestParam Long graphId,
            @RequestParam String vertexId,
            @RequestBody Map<String, Object> data) {
        try {
            log.info("更新节点数据，graphId={}, vertexId={}, data={}", graphId, vertexId, data);
            boolean result = graphDataService.updateNodeData(graphId, vertexId, data);
            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "更新节点数据失败", false);
            }
        } catch (Exception e) {
            log.error("更新节点数据失败", e);
            return Result.error(500, "更新节点数据失败: " + e.getMessage(), false);
        }
    }

    @PutMapping("/edge")
    public Result<Boolean> updateEdgeData(
            @RequestParam Long graphId,
            @RequestParam String edgeId,
            @RequestBody Map<String, Object> data) {
        try {
            log.info("更新边数据，graphId={}, edgeId={}, data={}", graphId, edgeId, data);
            boolean result = graphDataService.updateEdgeData(graphId, edgeId, data);
            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "更新边数据失败", false);
            }
        } catch (Exception e) {
            log.error("更新边数据失败", e);
            return Result.error(500, "更新边数据失败: " + e.getMessage(), false);
        }
    }

    @DeleteMapping("/vertex")
    public Result<Boolean> deleteVertex(
            @RequestParam Long graphId,
            @RequestParam String vertexId,
            @RequestParam(required = false) String label) {
        try {
            log.info("开始删除节点，graphId={}, vertexId={}, label={}", graphId, vertexId, label);
            boolean result = graphDataService.deleteNode(graphId, vertexId, label);
            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "删除节点失败", false);
            }
        } catch (Exception e) {
            log.error("删除节点失败，graphId={}, vertexId={}", graphId, vertexId, e);
            return Result.error(500, "删除节点失败: " + e.getMessage(), false);
        }
    }

    /**
     * 批量删除节点
     */
    @DeleteMapping("/vertices")
    public Result<Boolean> deleteVertices(
            @RequestParam Long graphId,
            @RequestBody Map<String, Object> request) {
        try {
            List<String> vertexIds = (List<String>) request.get("vertexIds");
            if (vertexIds == null || vertexIds.isEmpty()) {
                return Result.error(400, "节点ID列表不能为空", false);
            }
            log.info("开始批量删除节点，graphId={}, vertexIds={}", graphId, vertexIds);
            boolean result = graphDataService.deleteNodes(graphId, vertexIds, request.get("label").toString());
            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "批量删除节点失败", false);
            }
        } catch (Exception e) {
            log.error("批量删除节点失败，graphId={}", graphId, e);
            return Result.error(500, "批量删除节点失败: " + e.getMessage(), false);
        }
    }

    /**
     * 获取图统计信息
     */
    @GetMapping("/summary")
    public Result<GraphSummary> getGraphSummary(
            @RequestParam Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        try {
            log.info("获取图统计信息，graphId={}, connectionId={}, graphCode={}", graphId, connectionId, graphCode);
            GraphSummary summary = graphDataService.getGraphSummary(graphId, connectionId, graphCode);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取图统计信息失败，graphId={}", graphId, e);
            return Result.error(500, "获取图统计信息失败: " + e.getMessage(), null);
        }
    }
}