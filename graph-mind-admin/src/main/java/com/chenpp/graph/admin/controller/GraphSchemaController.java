package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphNodeDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
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
    private GraphNodeDefService nodeDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphSchemaService graphSchemaService;

    /**
     * 获取节点定义列表
     *
     * @param graphId 图ID
     * @return 节点定义列表
     */
    @GetMapping("/nodes")
    public Result<List<GraphNodeDef>> getNodeDefs(@PathVariable Long graphId) {
        try {
            List<GraphNodeDef> nodeDefs = nodeDefService.getNodeDefsByGraphId(graphId);
            return Result.success(nodeDefs);
        } catch (Exception e) {
            log.error("获取节点定义列表失败", e);
            return Result.error("获取节点定义列表失败");
        }
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
        try {
            nodeDef.setGraphId(graphId);
            boolean success = nodeDefService.saveNodeDefWithProperties(nodeDef);
            if (success) {
                return Result.success("新增节点定义成功");
            } else {
                return Result.error("新增节点定义失败");
            }
        } catch (Exception e) {
            log.error("新增节点定义失败", e);
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
        try {
            nodeDef.setId(nodeId);
            nodeDef.setGraphId(graphId);
            boolean success = nodeDefService.updateNodeDefWithProperties(nodeDef);
            if (success) {
                return Result.success("更新节点定义成功");
            } else {
                return Result.error("更新节点定义失败");
            }
        } catch (Exception e) {
            log.error("更新节点定义失败", e);
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
        try {
            boolean success = nodeDefService.deleteNodeDefWithProperties(nodeId);
            if (success) {
                return Result.success("删除节点定义成功");
            } else {
                return Result.error("删除节点定义失败");
            }
        } catch (Exception e) {
            log.error("删除节点定义失败", e);
            return Result.error("删除节点定义失败");
        }
    }

    /**
     * 获取边定义列表
     *
     * @param graphId 图ID
     * @return 边定义列表
     */
    @GetMapping("/edges")
    public Result<List<GraphEdgeDef>> getEdgeDefs(@PathVariable Long graphId) {
        try {
            List<GraphEdgeDef> edgeDefs = edgeDefService.getEdgeDefsByGraphId(graphId);
            return Result.success(edgeDefs);
        } catch (Exception e) {
            log.error("获取边定义列表失败", e);
            return Result.error("获取边定义列表失败");
        }
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
        try {
            edgeDef.setGraphId(graphId);
            boolean success = edgeDefService.saveEdgeDefWithProperties(edgeDef);
            if (success) {
                return Result.success("新增边定义成功");
            } else {
                return Result.error("新增边定义失败");
            }
        } catch (Exception e) {
            log.error("新增边定义失败", e);
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
        try {
            edgeDef.setId(edgeId);
            edgeDef.setGraphId(graphId);
            boolean success = edgeDefService.updateEdgeDefWithProperties(edgeDef);
            if (success) {
                return Result.success("更新边定义成功");
            } else {
                return Result.error("更新边定义失败");
            }
        } catch (Exception e) {
            log.error("更新边定义失败", e);
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
        try {
            boolean success = edgeDefService.deleteEdgeDefWithProperties(edgeId);
            if (success) {
                return Result.success("删除边定义成功");
            } else {
                return Result.error("删除边定义失败");
            }
        } catch (Exception e) {
            log.error("删除边定义失败", e);
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
        try {
            graphSchemaService.publishSchema(graphId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("发布图Schema失败", e);
            return Result.error("发布图Schema失败: " + e.getMessage());
        }
    }
}