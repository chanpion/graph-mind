package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.GraphExpandRequest;
import com.chenpp.graph.admin.model.GraphPathRequest;
import com.chenpp.graph.admin.model.GraphQueryRequest;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphQueryService;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 图查询控制器
 * 专门处理图数据的查询、展开、路径查找等接口
 *
 * @author April.Chen
 * @date 2025/8/11 10:45
 */
@Slf4j
@RestController
@RequestMapping("/api/graphs")
public class GraphQueryController {

    @Autowired
    private GraphQueryService graphQueryService;

    @PostMapping("/query")
    public Result<GraphData> query(
            @RequestBody(required = false) GraphQueryRequest request,
            @RequestParam(required = false) Long graphId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        if (request == null) {
            request = new GraphQueryRequest();
        }
        if (request.getQuery() == null || request.getQuery().isEmpty()) {
            return Result.error("查询语句不能为空");
        }
        if (request.getGraphId() == null) request.setGraphId(graphId);
        if (request.getConnectionId() == null) request.setConnectionId(connectionId);
        if (request.getGraphCode() == null) request.setGraphCode(graphCode);

        GraphData graphData = graphQueryService.query(request);
        return Result.success(graphData);
    }

    @PostMapping("/expand")
    public Result<GraphData> expand(@RequestBody GraphExpandRequest request) {
        GraphData graphData = graphQueryService.expand(request);
        return Result.success(graphData);
    }

    @PostMapping("/path")
    public Result<GraphData> findPath(@RequestBody GraphPathRequest request) {
        GraphData graphData = graphQueryService.findPath(request);
        return Result.success(graphData);
    }

    @GetMapping("/summary")
    public Result<GraphSummary> getSummary(Long graphId, Long connectionId, String graphCode) {
        GraphSummary summary = graphQueryService.getSummary(graphId, connectionId, graphCode);
        return Result.success(summary);
    }
}