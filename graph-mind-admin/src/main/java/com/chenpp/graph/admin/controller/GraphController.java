package com.chenpp.graph.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图管理控制器
 *
 * @author April.Chen
 * @date 2025/8/1 17:00
 */
@Slf4j
@RestController
@RequestMapping("/api/graphs")
public class GraphController {

    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphSchemaService graphSchemaService;

    /**
     * 获取图列表
     */
    @GetMapping
    public Result<Page<GraphInfo>> getGraphs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        Page<GraphInfo> pageObj = new Page<>(page, pageSize);
        Page<GraphInfo> result = graphService.queryGraphs(pageObj, keyword);
        return Result.success(result);
    }

    /**
     * 根据连接ID获取图列表
     */
    @GetMapping("/connection/{connectionId}")
    public Result<Page<GraphInfo>> getGraphsByConnectionId(
            @PathVariable Long connectionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<GraphInfo> pageObj = new Page<>(page, pageSize);
        Page<GraphInfo> result = graphService.queryGraphsByConnectionId(connectionId, pageObj);
        return Result.success(result);
    }

    /**
     * 新增图
     */
    @PostMapping
    public Result<Long> createGraph(@RequestBody GraphInfo graphInfo) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String creator = authentication != null ? authentication.getName() : "unknown";
        graphInfo.setCreator(creator);

        try {
            // 先在图数据库创建图，创建成功后再保存到 MySQL
            Long graphId = graphSchemaService.createGraphInDatabase(graphInfo);
            return Result.success(graphId);
        } catch (Exception e) {
            log.warn("在图数据库中创建图失败: {}", e.getMessage());
            return Result.error("在图数据库中创建图失败: " + e.getMessage());
        }
    }

    /**
     * 更新图
     */
    @PutMapping("/{id}")
    public Result<String> updateGraph(@PathVariable Long id, @RequestBody GraphInfo graphInfo) {
        graphInfo.setId(id);
        graphService.updateById(graphInfo);
        return Result.success("更新成功");
    }

    /**
     * 删除图
     * <p>对于平台创建的图，传 graphId 即可；对于从图数据库发现的已有图，需额外传 connectionId + graphCode。</p>
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteGraph(
            @PathVariable Long id,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        boolean result = graphService.removeGraph(id, connectionId, graphCode);
        if (!result) {
            return Result.error(ErrorCode.GRAPH_NOT_FOUND);
        }
        return Result.success("删除成功");
    }

    /**
     * 获取图详情
     */
    @GetMapping("/{id}")
    public Result<GraphInfo> getGraph(@PathVariable Long id) {
        GraphInfo graphInfo = graphService.getById(id);
        if (graphInfo == null) {
            return Result.error(ErrorCode.GRAPH_NOT_FOUND);
        }
        return Result.success(graphInfo);
    }
}