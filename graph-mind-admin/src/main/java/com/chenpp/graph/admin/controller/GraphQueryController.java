package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.neo4j.Neo4jClient;
import com.chenpp.graph.neo4j.Neo4jConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
        try {
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

            // 检查是否为Neo4j数据库
            if (!"neo4j".equalsIgnoreCase(connection.getType())) {
                return Result.error("暂不支持的数据库类型: " + connection.getType());
            }

            // 构建Neo4j客户端
            Neo4jConf neo4jConf = new Neo4jConf();
            neo4jConf.setUri(String.format("neo4j://%s:%d", connection.getHost(), connection.getPort()));
            neo4jConf.setUsername(connection.getUsername());
            neo4jConf.setPassword(connection.getPassword());
            neo4jConf.setGraphCode(graph.getCode());

            GraphClient graphClient = new Neo4jClient(neo4jConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            // 执行查询
            GraphData graphData = graphDataOperations.query(cypher);
            return Result.success(graphData);
        } catch (Exception e) {
            log.error("图查询失败，graphId={}", graphId, e);
            return Result.error("图查询失败: " + e.getMessage());
        }
    }
}