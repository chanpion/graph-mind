package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.model.GraphExpandRequest;
import com.chenpp.graph.admin.model.GraphQueryRequest;
import com.chenpp.graph.admin.model.GraphPathRequest;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphQueryService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 图查询服务实现类
 * 专门处理图数据的查询、展开、路径查找等操作
 *
 * @author April.Chen
 * @date 2026/6/22
 */
@Slf4j
@Service
public class GraphQueryServiceImpl implements GraphQueryService {

    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphConnectionService connectionService;

    @Override
    public GraphData query(GraphQueryRequest request) {
        GraphConf graphConf = GraphClientFactory.resolveGraphConf(
                request.getGraphId(),
                request.getConnectionId(),
                request.getGraphCode(),
                graphService,
                connectionService
        );
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
        return graphDataOperations.query(request.getQuery());
    }

    @Override
    public GraphData expand(GraphExpandRequest request) {
        Long graphId = request.getGraphId();
        String vertexId = request.getVertexId();
        Integer depth = request.getDepth();
        String label = request.getLabel();
        String property = request.getProperty();
        Long connectionId = request.getConnectionId();
        String graphCode = request.getGraphCode();

        if (StringUtils.isBlank(vertexId)) {
            throw new IllegalArgumentException("查询值不能为空");
        }

        if (depth == null) {
            depth = 1;
        }

        log.info("展开邻居: queryValue={}, depth={}, label={}, property={}, graphId={}", vertexId, depth, label, property, graphId);

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

        // 如果传入了 label + property，先用属性值查找顶点 uid，再展开
        String vertexUid = vertexId;
        if (StringUtils.isNoneBlank(label, property) && !"uid".equals(property)) {
            String foundUid = lookupVertexUid(graphDataOperations, label, property, vertexId);
            if (foundUid == null) {
                return null;
            }
            vertexUid = foundUid;
        }

        return graphDataOperations.expand(vertexUid, depth);
    }

    @Override
    public GraphData findPath(GraphPathRequest request) {
        Long graphId = request.getGraphId();
        Long connectionId = request.getConnectionId();
        String graphCode = request.getGraphCode();

        String startVertexId = request.getStartVertexId();
        String endVertexId = request.getEndVertexId();
        Integer maxDepth = request.getMaxDepth();

        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

        // 判断是否提供了完整的属性条件用于直接查询
        boolean hasStartPropertyCondition = StringUtils.isNoneBlank(request.getStartLabel(), request.getStartProp(), request.getStartValue());
        boolean hasEndPropertyCondition = StringUtils.isNoneBlank(request.getEndLabel(), request.getEndProp(), request.getEndValue());

        if (maxDepth == null) {
            maxDepth = 5;
        }

        String dbType = graphConf.getType();
        String pathQuery;

        // 如果两端都提供了属性条件，则直接构造包含属性条件的路径查询，实现一次查询
        if (hasStartPropertyCondition && hasEndPropertyCondition) {
            log.info("使用属性条件直接查询路径: startLabel={}, startProp={}, startValue={}, endLabel={}, endProp={}, endValue={}",
                    request.getStartLabel(), request.getStartProp(), request.getStartValue(),
                    request.getEndLabel(), request.getEndProp(), request.getEndValue());

            String escapedStartValue = request.getStartValue().replace("'", "\\'");
            String escapedEndValue = request.getEndValue().replace("'", "\\'");

            pathQuery = buildPathQueryWithPropertyCondition(dbType,
                    request.getStartLabel(), request.getStartProp(), escapedStartValue,
                    request.getEndLabel(), request.getEndProp(), escapedEndValue,
                    maxDepth);
        } else {
            // 使用 uid 方式查询（原有逻辑，保持向后兼容）
            // 如果传入了起点 label + property + value，先查找起点 uid
            if ((startVertexId == null || startVertexId.isEmpty()) && hasStartPropertyCondition) {
                if ("uid".equals(request.getStartProp())) {
                    startVertexId = request.getStartValue();
                    log.debug("起点按 uid 直接定位: {}", startVertexId);
                } else {
                    String foundUid = lookupVertexUid(graphDataOperations, request.getStartLabel(), request.getStartProp(), request.getStartValue());
                    if (foundUid != null) {
                        startVertexId = foundUid;
                    } else {
                        startVertexId = request.getStartValue();
                    }
                }
            }

            // 如果传入了终点 label + property + value，先查找终点 uid
            if ((endVertexId == null || endVertexId.isEmpty()) && hasEndPropertyCondition) {
                if ("uid".equals(request.getEndProp())) {
                    endVertexId = request.getEndValue();
                    log.debug("终点按 uid 直接定位: {}", endVertexId);
                } else {
                    String foundUid = lookupVertexUid(graphDataOperations, request.getEndLabel(), request.getEndProp(), request.getEndValue());
                    if (foundUid != null) {
                        endVertexId = foundUid;
                    } else {
                        endVertexId = request.getEndValue();
                    }
                }
            }

            if (startVertexId == null || startVertexId.isEmpty()) {
                throw new IllegalArgumentException("起始顶点不能为空，请检查起点查询条件");
            }

            if (endVertexId == null || endVertexId.isEmpty()) {
                throw new IllegalArgumentException("目标顶点不能为空，请检查终点查询条件");
            }

            String escapedStartVertexId = startVertexId.replace("'", "\\'");
            String escapedEndVertexId = endVertexId.replace("'", "\\'");

            pathQuery = buildPathQueryWithUid(dbType, escapedStartVertexId, escapedEndVertexId, maxDepth);
        }

        log.debug("执行路径查询: {}", pathQuery);
        return graphDataOperations.query(pathQuery);
    }

    /**
     * 根据属性条件构建路径查询语句
     */
    private String buildPathQueryWithPropertyCondition(String dbType,
                                                       String startLabel, String startProp, String startValue,
                                                       String endLabel, String endProp, String endValue,
                                                       int maxDepth) {
        if ("nebula".equalsIgnoreCase(dbType)) {
            return String.format("MATCH p = shortestPath((a:`%s`)-[*1..%d]-(b:`%s`)) WHERE a.`%s`.`%s` == '%s' AND b.`%s`.`%s` == '%s' RETURN p",
                    startLabel, maxDepth, endLabel, startLabel, startProp, startValue, endLabel, endProp, endValue);
        } else if ("janus".equalsIgnoreCase(dbType) || "janusgraph".equalsIgnoreCase(dbType)) {
            return String.format("g.V().hasLabel('%s').has('%s', '%s').repeat(bothE().bothV().simplePath()).until(hasLabel('%s').has('%s', '%s')).limit(1).path()",
                    startLabel, startProp, startValue, endLabel, endProp, endValue);
        } else {
            // neo4j 或默认
            return String.format("MATCH p = shortestPath((a:`%s`)-[*1..%d]-(b:`%s`)) WHERE a.`%s` = '%s' AND b.`%s` = '%s' RETURN p LIMIT 1",
                    startLabel, maxDepth, endLabel, startProp, startValue, endProp, endValue);
        }
    }

    /**
     * 根据 uid 构建路径查询语句
     */
    private String buildPathQueryWithUid(String dbType, String startVertexId, String endVertexId, int maxDepth) {
        if ("nebula".equalsIgnoreCase(dbType)) {
            return String.format("FIND SHORTEST PATH FROM \"%s\" TO \"%s\" OVER * UPTO %d STEPS YIELD PATH AS p",
                    startVertexId, endVertexId, maxDepth);
        } else if ("janus".equalsIgnoreCase(dbType) || "janusgraph".equalsIgnoreCase(dbType)) {
            return String.format("g.V().has('uid','%s').repeat(bothE().bothV().simplePath()).until(has('uid','%s')).limit(1).path()",
                    startVertexId, endVertexId);
        } else {
            // neo4j 或默认
            return String.format("MATCH p = (a)-[*1..%d]-(b) WHERE a.uid = '%s' AND b.uid = '%s' RETURN p LIMIT 1",
                    maxDepth, startVertexId, endVertexId);
        }
    }

    @Override
    public GraphSummary getSummary(Long graphId, Long connectionId, String graphCode) {
        GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
        return graphDataOperations.getSummary();
    }

    /**
     * 按 Label + 属性查找顶点 uid
     */
    private String lookupVertexUid(GraphDataOperations ops, String label, String property, String value) {
        GraphVertex vertex = ops.findVertex(label, property, value);
        if (vertex != null && vertex.getUid() != null && !vertex.getUid().isEmpty()) {
            log.info("通过属性找到顶点 uid={}", vertex.getUid());
            return vertex.getUid();
        }
        log.warn("未找到匹配的顶点: label={}, property={}, value={}", label, property, value);
        return null;
    }
}