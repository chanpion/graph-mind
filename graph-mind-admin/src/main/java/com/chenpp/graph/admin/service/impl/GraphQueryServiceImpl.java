package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.model.GraphExpandRequest;
import com.chenpp.graph.admin.model.GraphQueryRequest;
import com.chenpp.graph.admin.model.GraphPathRequest;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphQueryService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.admin.enums.GraphTypeEnum;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.PathQuery;
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
        GraphDataOperations graphDataOperations = resolveDataOps(request.getGraphId(), request.getConnectionId(), request.getGraphCode());
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

        GraphDataOperations graphDataOperations = resolveDataOps(graphId, connectionId, graphCode);

        if (StringUtils.isNoneBlank(label, property) && !GraphConstants.UID.equals(property)) {
            return graphDataOperations.expand(label, property, vertexId, depth, 100);
        }

        return graphDataOperations.expand(vertexId, depth);
    }

    @Override
    public GraphData findPath(GraphPathRequest request) {
        Integer maxDepth = request.getMaxDepth();
        if (maxDepth == null) {
            maxDepth = 5;
        }

        boolean hasStartPropCondition = StringUtils.isNoneBlank(request.getStartLabel(), request.getStartProp(), request.getStartValue());
        boolean hasEndPropCondition = StringUtils.isNoneBlank(request.getEndLabel(), request.getEndProp(), request.getEndValue());

        GraphDataOperations ops = resolveDataOps(request.getGraphId(), request.getConnectionId(), request.getGraphCode());

        if (hasStartPropCondition && hasEndPropCondition) {
            PathQuery pq = new PathQuery();
            pq.setStartProperty(new PathQuery.Condition(request.getStartLabel(), request.getStartProp(), request.getStartValue()));
            pq.setEndProperty(new PathQuery.Condition(request.getEndLabel(), request.getEndProp(), request.getEndValue()));
            pq.setMaxDepth(maxDepth);
            pq.setLimit(10);
            return ops.findPath(pq);
        }

        if (StringUtils.isNotBlank(request.getStartVertexId()) && StringUtils.isNotBlank(request.getEndVertexId())) {
            return ops.findPath(request.getStartVertexId(), request.getEndVertexId(), maxDepth);
        }

        throw new IllegalArgumentException("请提供起点和终点的查询条件（属性条件或顶点ID）");
    }

    private String buildPathQueryByUid(String dbType, String startVertexId, String endVertexId, int maxDepth) {
        if (GraphTypeEnum.nebula.name().equalsIgnoreCase(dbType)) {
            return String.format("FIND ALL PATH FROM \"%s\" TO \"%s\" OVER * UPTO %d STEPS YIELD PATH AS p | LIMIT 10",
                    escapeCypherValue(startVertexId), escapeCypherValue(endVertexId), maxDepth);
        } else if (GraphTypeEnum.janus.name().equalsIgnoreCase(dbType)) {
            return String.format(
                    "g.V().has(\"%s\", \"%s\").repeat(outE().inV().simplePath())"
                    + ".until(has(\"%s\", \"%s\").or().loops().is(%d))"
                    + ".has(\"%s\", \"%s\")"
                    + ".path().by(valueMap()).by(valueMap()).limit(10)",
                    GraphConstants.UID, escapeGremlinValue(startVertexId),
                    GraphConstants.UID, escapeGremlinValue(endVertexId), maxDepth,
                    GraphConstants.UID, escapeGremlinValue(endVertexId));
        } else {
            return String.format("MATCH p = (a {" + GraphConstants.UID + ": '%s'})-[*1..%d]-(b {" + GraphConstants.UID + ": '%s'}) RETURN p LIMIT 10",
                    escapeCypherValue(startVertexId), maxDepth, escapeCypherValue(endVertexId));
        }
    }

    @Override
    public GraphSummary getSummary(Long graphId, Long connectionId, String graphCode) {
        return resolveDataOps(graphId, connectionId, graphCode).getSummary();
    }

    private GraphDataOperations resolveDataOps(Long graphId, Long connectionId, String graphCode) {
        GraphConf graphConf = GraphClientFactory.resolveGraphConf(
                graphId, connectionId, graphCode, graphService, connectionService);
        return GraphClientFactory.createGraphClient(graphConf).opsForGraphData();
    }

    private static String escapeCypherValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String escapeGremlinValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}