package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
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
    private GraphDatabaseConnectionService connectionService;

    @PostMapping("/query")
    public Result<GraphData> query(
            @PathVariable Long graphId,
            @RequestBody Map<String, String> request) {
        String cypher = request.get("cypher");
        if (cypher == null || cypher.isEmpty()) {
            return Result.error("Cypher查询语句不能为空");
        }

        // 获取图信息
        Graph graph = graphService.getById(graphId);
        if (graph == null) {
            return Result.error("图不存在，graphId=" + graphId);
        }

        // 获取图数据库连接信息
        GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
        if (connection == null) {
            return Result.error("图数据库连接不存在，connectionId=" + graph.getConnectionId());
        }

        // 构建图配置信息
        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);
        // 创建图客户端
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

        // 执行查询
        GraphData graphData = graphDataOperations.query(cypher);
        return Result.success(graphData);
    }
}