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
import com.chenpp.graph.core.model.GraphVertex;
import java.util.List;
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
        // 接收前端参数
        String queryValue = request.containsKey("nodeId") ? (String) request.get("nodeId") : (String) request.get("vertexId");
        Integer depth = (Integer) request.get("depth");
        String label = (String) request.get("label");
        String propertyName = (String) request.get("property");

        if (queryValue == null || queryValue.isEmpty()) {
            return Result.error("查询值不能为空");
        }

        if (depth == null) {
            depth = 1;
        }

        log.info("展开邻居: queryValue={}, depth={}, label={}, property={}, graphId={}", queryValue, depth, label, propertyName, graphId);

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

        // 如果传入了 label + property，先用属性值查找节点 uid，再展开
        String nodeUid = queryValue;
        if (label != null && !label.isEmpty() && propertyName != null && !propertyName.isEmpty()) {
            String lookupQuery = buildFindNodeByPropertyQuery(graphConf.getType(), label, propertyName, queryValue);
            log.debug("查找节点: {}", lookupQuery);
            GraphData lookupResult = graphDataOperations.query(lookupQuery);
            if (lookupResult != null && lookupResult.getVertices() != null && !lookupResult.getVertices().isEmpty()) {
                String foundUid = lookupResult.getVertices().get(0).getUid();
                if (foundUid != null && !foundUid.isEmpty()) {
                    nodeUid = foundUid;
                    log.info("通过属性找到节点 uid={}", nodeUid);
                }
            } else {
                log.warn("未找到匹配的节点: label={}, property={}, value={}", label, propertyName, queryValue);
            }
        }

        GraphData graphData = graphDataOperations.expand(nodeUid, depth);
        return Result.success(graphData);
    }

    /**
     * 构建按 Label + 属性查找节点的查询语句
     */
    private String buildFindNodeByPropertyQuery(String dbType, String label, String property, String value) {
        String escapedValue = value.replace("'", "\\'");
        if ("nebula".equalsIgnoreCase(dbType)) {
            return String.format("LOOKUP ON `%s` WHERE `%s`.`%s` == '%s' YIELD id(vertex) AS uid, properties(vertex) AS props",
                    label, label, property, escapedValue);
        } else if ("janus".equalsIgnoreCase(dbType) || "janusgraph".equalsIgnoreCase(dbType)) {
            return String.format("g.V().hasLabel('%s').has('%s', '%s')", label, property, escapedValue);
        } else {
            // neo4j 或默认
            return String.format("MATCH (n:`%s`) WHERE n.`%s` = '%s' RETURN n", label, property, escapedValue);
        }
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