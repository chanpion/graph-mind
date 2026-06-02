package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.model.GraphData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/8/11 10:45
 */
@Slf4j
@RestController
@RequestMapping("/api/graphs/{graphId}")
public class GraphQueryController {

    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphConnectionService connectionService;

    @PostMapping("/query")
    public Result<GraphData> query(
            @PathVariable Long graphId,
            @RequestBody Map<String, String> request,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        String cypher = request.get("cypher");
        if (cypher == null || cypher.isEmpty()) {
            return Result.error("Cypher查询语句不能为空");
        }

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
        GraphData graphData = graphDataOperations.query(cypher);
        return Result.success(graphData);
    }

    @PostMapping("/expand")
    public Result<GraphData> expand(
            @PathVariable Long graphId,
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        String vertexId = (String) request.get("vertexId");
        Integer depth = (Integer) request.get("depth");

        if (vertexId == null || vertexId.isEmpty()) {
            return Result.error("节点ID不能为空");
        }

        if (depth == null) {
            depth = 1;
        }

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
        GraphData graphData = graphDataOperations.expand(vertexId, depth);
        return Result.success(graphData);
    }

    @PostMapping("/path")
    public Result<GraphData> findPath(
            @PathVariable Long graphId,
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        String startNodeId = (String) request.get("startNodeId");
        String endNodeId = (String) request.get("endNodeId");
        Integer maxDepth = (Integer) request.get("maxDepth");

        if (startNodeId == null || startNodeId.isEmpty()) {
            return Result.error("起始节点ID不能为空");
        }

        if (endNodeId == null || endNodeId.isEmpty()) {
            return Result.error("目标节点ID不能为空");
        }

        if (maxDepth == null) {
            maxDepth = 5;
        }

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

        // 获取图数据库类型（从 graphConf 中读取）
        String dbType = graphConf.getType();
        String pathQuery;
        if ("nebula".equalsIgnoreCase(dbType)) {
            pathQuery = String.format("FIND SHORTEST PATH FROM \"%s\" TO \"%s\" OVER * UPTO %d STEPS YIELD PATH AS p",
                    startNodeId, endNodeId, maxDepth);
        } else if ("janus".equalsIgnoreCase(dbType) || "janusgraph".equalsIgnoreCase(dbType)) {
            pathQuery = String.format("g.V().has('uid','%s').repeat(bothE().bothV().simplePath()).until(has('uid','%s')).limit(1).path()",
                    startNodeId, endNodeId);
        } else {
            // neo4j 或默认
            pathQuery = String.format("MATCH p = (a)-[*1..%d]-(b) WHERE a.uid = '%s' AND b.uid = '%s' RETURN p LIMIT 1",
                    maxDepth, startNodeId, endNodeId);
        }
        // 执行查询
        GraphData graphData = graphDataOperations.query(pathQuery);
        return Result.success(graphData);
    }
}