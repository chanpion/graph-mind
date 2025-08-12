package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}