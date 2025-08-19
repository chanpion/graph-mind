package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphDataService;
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
@Slf4j
@RestController
@RequestMapping("/api/graphs/{graphId}")
public class GraphDataController {

    @Autowired
    private GraphDataService graphDataService;

    /**
     * 导入节点数据
     *
     * @param graphId    图ID
     * @param nodeTypeId 节点类型ID
     * @param file       CSV文件
     * @param headers    CSV表头信息
     * @param mapping    字段映射关系
     * @param data       数据内容
     * @return 导入结果
     */
    @PostMapping("/nodes/{nodeTypeId}/import")
    public Result<ImportResult> importNodeData(
            @PathVariable Long graphId,
            @PathVariable Long nodeTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("headers") String headers,
            @RequestPart("mapping") String mapping,
            @RequestPart("data") String data) {
        try {
            // TODO: 实现节点数据导入逻辑
            log.info("开始导入节点数据，graphId={}, nodeTypeId={}", graphId, nodeTypeId);
            log.info("文件名: {}", file.getOriginalFilename());
            log.info("表头信息: {}", headers);
            log.info("映射关系: {}", mapping);
            log.info("数据内容: {}", data);

            // 调用服务方法处理数据导入
            ImportResult result = graphDataService.importNodeData(graphId, nodeTypeId, file, headers, mapping, data);

            return Result.success(result);
        } catch (Exception e) {
            log.error("导入节点数据失败", e);
            return Result.error(500, "导入节点数据失败: " + e.getMessage(), null);
        }
    }

    /**
     * 导入边数据
     *
     * @param graphId    图ID
     * @param edgeTypeId 边类型ID
     * @param file       CSV文件
     * @param headers    CSV表头信息
     * @param mapping    字段映射关系
     * @param data       数据内容
     * @return 导入结果
     */
    @PostMapping("/edges/{edgeTypeId}/import")
    public Result<ImportResult> importEdgeData(
            @PathVariable Long graphId,
            @PathVariable Long edgeTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("headers") String headers,
            @RequestPart("mapping") String mapping,
            @RequestPart("data") String data) {
        try {
            // TODO: 实现边数据导入逻辑
            log.info("开始导入边数据，graphId={}, edgeTypeId={}", graphId, edgeTypeId);
            log.info("文件名: {}", file.getOriginalFilename());
            log.info("表头信息: {}", headers);
            log.info("映射关系: {}", mapping);
            log.info("数据内容: {}", data);

            // 调用服务方法处理数据导入
            ImportResult result = graphDataService.importEdgeData(graphId, edgeTypeId, file, headers, mapping, data);

            return Result.success(result);
        } catch (Exception e) {
            log.error("导入边数据失败", e);
            return Result.error(500, "导入边数据失败: " + e.getMessage(), null);
        }
    }

    /**
     * 删除节点
     *
     * @param graphId 图ID
     * @param nodeId  节点ID
     * @param label   节点标签
     * @return 删除结果
     */
    @DeleteMapping("/data/nodes/{nodeId}")
    public Result<Boolean> deleteNode(
            @PathVariable Long graphId,
            @PathVariable String nodeId,
            @RequestParam(required = false) String label) {
        try {
            log.info("开始删除节点，graphId={}, nodeId={}, label={}", graphId, nodeId, label);

            // 调用服务方法处理节点删除
            boolean result = graphDataService.deleteNode(graphId, nodeId, label);

            if (result) {
                return Result.success(true);
            } else {
                return Result.error(500, "删除节点失败", false);
            }
        } catch (Exception e) {
            log.error("删除节点失败，graphId={}, nodeId={}", graphId, nodeId, e);
            return Result.error(500, "删除节点失败: " + e.getMessage(), false);
        }
    }

    /**
     * 批量删除节点
     *
     * @param graphId 图ID
     * @param request 包含节点ID列表的请求体
     * @return 删除结果
     */
    @DeleteMapping("/data/nodes")
    public Result<Boolean> deleteNodes(
            @PathVariable Long graphId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> nodeIds = (List<String>) request.get("nodeIds");

            if (nodeIds == null || nodeIds.isEmpty()) {
                return Result.error(400, "节点ID列表不能为空", false);
            }

            log.info("开始批量删除节点，graphId={}, nodeIds={}", graphId, nodeIds);

            // 调用服务方法处理节点批量删除
            boolean result = graphDataService.deleteNodes(graphId, nodeIds, request.get("label").toString());

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
}