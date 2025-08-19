package com.chenpp.graph.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.Result;
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

import java.time.LocalDateTime;

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

    /**
     * 获取图列表
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词
     * @return 图列表
     */
    @GetMapping
    public Result<Page<Graph>> getGraphs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        Page<Graph> pageObj = new Page<>(page, pageSize);
        Page<Graph> result = graphService.queryGraphs(pageObj, keyword);
        return Result.success(result);
    }

    /**
     * 根据连接ID获取图列表
     *
     * @param connectionId 连接ID
     * @param page         页码
     * @param pageSize     每页数量
     * @return 图列表
     */
    @GetMapping("/connection/{connectionId}")
    public Result<Page<Graph>> getGraphsByConnectionId(
            @PathVariable Long connectionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<Graph> pageObj = new Page<>(page, pageSize);
        Page<Graph> result = graphService.queryGraphsByConnectionId(connectionId, pageObj);
        return Result.success(result);
    }

    /**
     * 新增图
     *
     * @param graph 图信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Long> createGraph(@RequestBody Graph graph) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String creator = authentication != null ? authentication.getName() : "unknown";

        graph.setCreator(creator);
        graph.setCreateTime(LocalDateTime.now());
        graph.setUpdateTime(LocalDateTime.now());
        graph.setStatus(0);
        graphService.save(graph);
        return Result.success(graph.getId());
    }

    /**
     * 更新图
     *
     * @param id    图ID
     * @param graph 图信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<String> updateGraph(@PathVariable Long id, @RequestBody Graph graph) {
        graph.setId(id);
        graph.setUpdateTime(LocalDateTime.now());
        graphService.updateById(graph);
        return Result.success("更新成功");
    }

    /**
     * 删除图
     *
     * @param id 图ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteGraph(@PathVariable Long id) {
        boolean result = graphService.removeGraph(id);
        if (!result) {
            return Result.error(ErrorCode.GRAPH_NOT_FOUND);
        }
        return Result.success("删除成功");
    }

    /**
     * 获取图详情
     *
     * @param id 图ID
     * @return 图信息
     */
    @GetMapping("/{id}")
    public Result<Graph> getGraph(@PathVariable Long id) {
        Graph graph = graphService.getById(id);
        if (graph == null) {
            return Result.error(ErrorCode.GRAPH_NOT_FOUND);
        }
        return Result.success(graph);
    }
}