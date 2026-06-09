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
        String queryValue = request.containsKey("vertexId") ? (String) request.get("vertexId") : (String) request.get("vertexId");
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
            // 按 uid 属性查找时，直接使用值作为 uid，无需 LOOKUP
            if ("uid".equals(propertyName)) {
                nodeUid = queryValue;
                log.debug("按 uid 直接定位节点: {}", nodeUid);
            } else {
                String foundUid = lookupNodeUid(graphDataOperations, graphConf.getType(), label, propertyName, queryValue);
                if (foundUid != null) {
                    nodeUid = foundUid;
                }
            }
        }

        GraphData graphData = graphDataOperations.expand(nodeUid, depth);
        return Result.success(graphData);
    }

    /**
     * 按 Label + 属性查找节点 uid
     * @return 找到的 uid；如果该图数据库不支持按属性查找（如 Nebula 无索引），返回 null
     */
    private String lookupNodeUid(GraphDataOperations ops, String dbType, String label, String property, String value) {
        try {
            String lookupQuery = buildFindNodeByPropertyQuery(dbType, label, property, value);
            log.debug("查找节点: {}", lookupQuery);
            GraphData lookupResult = ops.query(lookupQuery);
            if (lookupResult != null && lookupResult.getVertices() != null && !lookupResult.getVertices().isEmpty()) {
                String foundUid = lookupResult.getVertices().get(0).getUid();
                if (foundUid != null && !foundUid.isEmpty()) {
                    log.info("通过属性找到节点 uid={}", foundUid);
                    return foundUid;
                }
            }
            log.warn("未找到匹配的节点: label={}, property={}, value={}", label, property, value);
            return null;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("-1005")) {
                log.warn("Nebula 无索引，不支持按属性查找: label={}, property={}, value={}", label, property, value);
            } else {
                log.warn("查找节点失败: label={}, property={}, value={}, error={}", label, property, value, msg);
            }
            return null;
        }
    }

    /**
     * 构建按 Label + 属性查找节点的查询语句
     */
    private String buildFindNodeByPropertyQuery(String dbType, String label, String property, String value) {
        String escapedValue = value.replace("'", "\\'");
        if ("nebula".equalsIgnoreCase(dbType)) {
            return String.format("MATCH (n:`%s`) WHERE n.`%s`.`%s` == '%s' RETURN n", label, label, property, escapedValue);
        } else if ("janus".equalsIgnoreCase(dbType) || "janusgraph".equalsIgnoreCase(dbType)) {
            return String.format("g.V().hasLabel('%s').has('%s', '%s')", label, property, escapedValue);
        } else {
            return String.format("MATCH (n:`%s`) WHERE n.`%s` = '%s' RETURN n", label, property, escapedValue);
        }
    }


    @PostMapping("/path")
    public Result<GraphData> findPath(
            @PathVariable Long graphId,
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String graphCode) {
        String startVertexId = (String) request.get("startVertexId");
        String endVertexId = (String) request.get("endVertexId");
        Integer maxDepth = (Integer) request.get("maxDepth");

        // 支持按 label + property + value 查找起点/终点
        String startValue = (String) request.get("startValue");
        String startLabel = (String) request.get("startLabel");
        String startProp = (String) request.get("startProp");
        String endValue = (String) request.get("endValue");
        String endLabel = (String) request.get("endLabel");
        String endProp = (String) request.get("endProp");

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

        // 如果传入了起点 label + property + value，先查找起点 uid
        if ((startVertexId == null || startVertexId.isEmpty()) && startLabel != null && !startLabel.isEmpty()
                && startProp != null && !startProp.isEmpty() && startValue != null && !startValue.isEmpty()) {
            if ("uid".equals(startProp)) {
                startVertexId = startValue;
                log.debug("起点按 uid 直接定位: {}", startVertexId);
            } else {
                String foundUid = lookupNodeUid(graphDataOperations, graphConf.getType(), startLabel, startProp, startValue);
                if (foundUid != null) {
                    startVertexId = foundUid;
                } else {
                    startVertexId = startValue;
                }
            }
        }

        // 如果传入了终点 label + property + value，先查找终点 uid
        if ((endVertexId == null || endVertexId.isEmpty()) && endLabel != null && !endLabel.isEmpty()
                && endProp != null && !endProp.isEmpty() && endValue != null && !endValue.isEmpty()) {
            if ("uid".equals(endProp)) {
                endVertexId = endValue;
                log.debug("终点按 uid 直接定位: {}", endVertexId);
            } else {
                String foundUid = lookupNodeUid(graphDataOperations, graphConf.getType(), endLabel, endProp, endValue);
                if (foundUid != null) {
                    endVertexId = foundUid;
                } else {
                    endVertexId = endValue;
                }
            }
        }

        if (startVertexId == null || startVertexId.isEmpty()) {
            return Result.error("起始节点不能为空，请检查起点查询条件");
        }

        if (endVertexId == null || endVertexId.isEmpty()) {
            return Result.error("目标节点不能为空，请检查终点查询条件");
        }

        if (maxDepth == null) {
            maxDepth = 5;
        }

        String escapedStartVertexId = startVertexId.replace("'", "\\'");
        String escapedEndVertexId = endVertexId.replace("'", "\\'");

        // 获取图数据库类型（从 graphConf 中读取）
        String dbType = graphConf.getType();
        String pathQuery;
        if ("nebula".equalsIgnoreCase(dbType)) {
            pathQuery = String.format("FIND SHORTEST PATH FROM \"%s\" TO \"%s\" OVER * UPTO %d STEPS YIELD PATH AS p",
                    escapedStartVertexId, escapedEndVertexId, maxDepth);
        } else if ("janus".equalsIgnoreCase(dbType) || "janusgraph".equalsIgnoreCase(dbType)) {
            pathQuery = String.format("g.V().has('uid','%s').repeat(bothE().bothV().simplePath()).until(has('uid','%s')).limit(1).path()",
                    escapedStartVertexId, escapedEndVertexId);
        } else {
            // neo4j 或默认
            pathQuery = String.format("MATCH p = (a)-[*1..%d]-(b) WHERE a.uid = '%s' AND b.uid = '%s' RETURN p LIMIT 1",
                    maxDepth, escapedStartVertexId, escapedEndVertexId);
        }
        // 执行查询
        GraphData graphData = graphDataOperations.query(pathQuery);
        return Result.success(graphData);
    }
}
