package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.PathQuery;
import com.chenpp.graph.nebula.util.NebulaUtil;
import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.data.PathWrapper;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/8/13 11:18
 */
@Slf4j
public class NebulaGraphDataOperations implements GraphDataOperations {
    private final NebulaConf nebulaConf;
    private final SessionPool sessionPool;

    public NebulaGraphDataOperations(NebulaConf nebulaConf) {
        this.nebulaConf = nebulaConf;
        this.sessionPool = NebulaClientFactory.getSessionPool(nebulaConf);
    }

    @Override
    public GraphVertex addVertex(GraphVertex vertex) throws GraphException {
        if (MapUtils.isEmpty(vertex.getProperties())) {
            throw new GraphException(ErrorCode.BAD_REQUEST, "Vertex properties cannot be empty");
        }
        try {
            String nql = NebulaUtil.buildInsertVertex(vertex);
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to add vertex, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to add vertex, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return vertex;
        } catch (Exception e) {
            log.error("Failed to add vertex: {}", vertex, e);
            throw new GraphException("Failed to add vertex", e);
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        if (MapUtils.isEmpty(vertex.getProperties())) {
            throw new GraphException(ErrorCode.BAD_REQUEST, "Vertex properties cannot be empty");
        }
        // Nebula中更新顶点使用UPDATE语法
        try {
            String vid = vertex.getUid();
            String setClause = vertex.getProperties().entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + NebulaUtil.formatValue(entry.getValue()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            String nql = NebulaUtil.buildUpdateVertex(vertex.getLabel(), vid, setClause);
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to update vertex, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to update vertex, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return vertex;
        } catch (Exception e) {
            log.error("Failed to update vertex: {}", vertex, e);
            throw new GraphException("Failed to update vertex", e);
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            log.info("Vertices collection is empty, skipping batch insert");
            return;
        }

        try {
            // 按标签分组顶点
            Map<String, List<GraphVertex>> verticesByLabel = vertices.stream()
                    .filter(vertex -> MapUtils.isNotEmpty(vertex.getProperties()))
                    .collect(Collectors.groupingBy(GraphVertex::getLabel));

            // 为每个标签构建批量插入语句
            verticesByLabel.forEach((label, vertexList) -> {
                if (CollectionUtils.isEmpty(vertexList)) {
                    return;
                }
                // 获取属性键（假设同标签的顶点具有相同的属性结构）
                String keys = String.join(",", vertexList.get(0).getProperties().keySet());

                String valuesClause = vertexList.stream().map(vertex -> {
                    String propValues = NebulaUtil.buildPropertyValuesClause(vertex.getProperties());
                    return String.format("\"%s\":(%s)", vertex.getUid(), propValues);
                }).collect(Collectors.joining(","));

                // 构建完整的NGQL语句
                String nql = NebulaUtil.buildInsertVertexBatch(label, keys, valuesClause);
                log.info("Execute batch insert NGQL: {}", nql);

                ResultSet resultSet;
                try {
                    resultSet = sessionPool.execute(nql);
                    if (!resultSet.isSucceeded()) {
                        log.error("Failed to batch insert vertices, errorCode: {}, errorMessage: {}",
                                resultSet.getErrorCode(), resultSet.getErrorMessage());
                        throw new GraphException("Failed to batch insert vertices, errorCode: " + resultSet.getErrorCode()
                                + ", errorMessage: " + resultSet.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Failed to batch insert vertices", e);
                    throw new GraphException("Failed to batch insert vertices", e);
                }

            });
        } catch (Exception e) {
            log.error("Failed to batch insert vertices", e);
            throw new GraphException("Failed to batch insert vertices", e);
        }
    }

    @Override
    public boolean deleteVertex(GraphVertex vertex) throws GraphException {
        try {
            // 删除顶点及其关联的边
            String nql = NebulaUtil.buildDeleteVertex(vertex.getUid());
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to delete vertex, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to delete vertex, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to delete vertex: {}", vertex, e);
            throw new GraphException("Failed to delete vertex", e);
        }
    }

    @Override
    public GraphEdge addEdge(GraphEdge edge) throws GraphException {
        try {
            // 构建插入边的NGQL语句
            // 语法: INSERT EDGE edge_type (prop1, prop2) VALUES src_vid -> dst_vid @rank: (val1, val2)
            String keys = String.join(",", edge.getProperties().keySet());
            String propValues = NebulaUtil.buildPropertyValuesClause(edge.getProperties());
            String nql = NebulaUtil.buildInsertEdge(edge.getLabel(), keys, edge.getStartUid(), edge.getEndUid(), propValues);


            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to add edge, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to add edge, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            return edge;
        } catch (Exception e) {
            log.error("Failed to add edge: {}", edge, e);
            throw new GraphException("Failed to add edge", e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            log.info("Edges collection is empty, skipping batch insert");
            return;
        }

        try {
            // 按标签分组边
            Map<String, List<GraphEdge>> edgesByLabel = edges.stream()
                    .collect(Collectors.groupingBy(GraphEdge::getLabel));

            // 为每个标签构建批量插入语句
            for (Map.Entry<String, List<GraphEdge>> entry : edgesByLabel.entrySet()) {
                String label = entry.getKey();
                List<GraphEdge> edgeList = entry.getValue();

                if (edgeList.isEmpty()) {
                    continue;
                }

                // 获取属性键（假设同标签的边具有相同的属性结构）
                GraphEdge firstEdge = edgeList.get(0);
                String propKeys = "";
                if (firstEdge.getProperties() != null && !firstEdge.getProperties().isEmpty()) {
                    propKeys = String.join(", ", firstEdge.getProperties().keySet());
                }

                // 构建批量插入语句的值部分
                StringBuilder valuesBuilder = new StringBuilder();
                String valuesStr = edgeList.stream().map(edge -> {
                    String propValues = NebulaUtil.buildPropertyValuesClause(edge.getProperties());
                    return String.format("\"%s\" -> \"%s\":(%s)", edge.getStartUid(), edge.getEndUid(), propValues);
                }).collect(Collectors.joining(", "));
                valuesBuilder.append(valuesStr);

                // 构建完整的NGQL语句
                String nql = NebulaUtil.buildInsertEdgeBatch(label, propKeys, valuesBuilder.toString());
                log.info("Execute batch insert NGQL: {}", nql);

                ResultSet resultSet = sessionPool.execute(nql);
                if (!resultSet.isSucceeded()) {
                    log.error("Failed to batch insert edges, errorCode: {}, errorMessage: {}",
                            resultSet.getErrorCode(), resultSet.getErrorMessage());
                    throw new GraphException("Failed to batch insert edges, errorCode: " + resultSet.getErrorCode()
                            + ", errorMessage: " + resultSet.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to batch insert edges", e);
            throw new GraphException("Failed to batch insert edges", e);
        }
    }

    @Override
    public GraphEdge updateEdge(GraphEdge edge) throws GraphException {
        try {
            // 添加SET子句
            String setClause = edge.getProperties().entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + NebulaUtil.formatValue(entry.getValue()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            String nql = NebulaUtil.buildUpdateEdge(edge.getLabel(), edge.getStartUid(), edge.getEndUid(), setClause);

            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to update edge, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to update edge, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return edge;
        } catch (Exception e) {
            log.error("Failed to update edge: {}", edge, e);
            throw new GraphException("Failed to update edge", e);
        }
    }

    @Override
    public boolean deleteEdge(GraphEdge edge) throws GraphException {
        try {
            String nql = NebulaUtil.buildDeleteEdge(edge.getLabel(), edge.getStartUid(), edge.getEndUid());
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to delete edge, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to delete edge, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to delete edge: {}", edge, e);
            throw new GraphException("Failed to delete edge", e);
        }
    }

    @Override
    public GraphData query(String cypher) throws GraphException {
        try {
            log.info("Execute NGQL: {}", cypher);

            ResultSet resultSet = sessionPool.execute(cypher);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to execute query, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to execute query, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            // 使用 Map 做去重，key 为 uid 保证顺序
            Map<String, GraphVertex> vertexMap = new LinkedHashMap<>();
            Map<String, GraphEdge> edgeMap = new LinkedHashMap<>();

            // 解析结果集
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                for (ValueWrapper value : record.values()) {
                    if (value.isVertex()) {
                        GraphVertex vertex = NebulaUtil.parseVertex(value.asNode());
                        if (vertex.getUid() != null) {
                            vertexMap.put(vertex.getUid(), vertex);
                        }
                    }
                    if (value.isEdge()) {
                        GraphEdge edge = NebulaUtil.parseEdge(value.asRelationship());
                        edgeMap.put(edge.getUid(), edge);
                    }
                    if (value.isPath()) {
                        PathWrapper path = value.asPath();
                        for (com.vesoft.nebula.client.graph.data.Node node : path.getNodes()) {
                            GraphVertex vertex = NebulaUtil.parseVertex(node);
                            if (vertex.getUid() != null) {
                                vertexMap.put(vertex.getUid(), vertex);
                            }
                        }
                        for (com.vesoft.nebula.client.graph.data.Relationship rel : path.getRelationships()) {
                            GraphEdge edge = NebulaUtil.parseEdge(rel);
                            edgeMap.put(edge.getUid(), edge);
                        }
                    }
                }
            }
            GraphData graphData = new GraphData();
            graphData.setVertices(new ArrayList<>(vertexMap.values()));
            graphData.setEdges(new ArrayList<>(edgeMap.values()));

            if (CollectionUtils.isNotEmpty(graphData.getEdges()) && CollectionUtils.isEmpty(graphData.getVertices())) {
                Set<String> vertexIds = new HashSet<>();
                graphData.getEdges().forEach(e -> {
                    vertexIds.add(e.getStartUid());
                    vertexIds.add(e.getEndUid());
                });
                List<GraphVertex> vertices = this.getVerticesByIds(vertexIds);
                graphData.setVertices(vertices);
            }
            return graphData;
        } catch (Exception e) {
            log.error("Failed to execute query: {}", cypher, e);
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    /**
     * 根据ID列表查询顶点
     *
     * @param idList 顶点ID列表
     * @return 顶点列表
     * @throws GraphException 查询异常
     */
    public List<GraphVertex> getVerticesByIds(Collection<String> idList) throws GraphException {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }

        try {
            // 添加ID列表
            String idListStr = idList.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", "));
            // 根据Nebula Graph查询规范，必须添加LIMIT子句
            String ngql = NebulaUtil.buildMatchVertices(idListStr);
            log.info("Execute NGQL: {}", ngql);

            ResultSet resultSet = sessionPool.execute(ngql);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to query vertices by IDs, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                throw new GraphException("Failed to query vertices by IDs, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            List<GraphVertex> vertices = new ArrayList<>();

            // 解析结果集
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                for (ValueWrapper value : record.values()) {
                    if (value.isVertex()) {
                        GraphVertex vertex = NebulaUtil.parseVertex(value.asNode());
                        vertices.add(vertex);
                    }
                }
            }

            return vertices;
        } catch (IOErrorException e) {
            log.error("Failed to query vertices by IDs due to IO error", e);
            throw new GraphException("Failed to query vertices by IDs due to IO error", e);
        } catch (Exception e) {
            log.error("Failed to query vertices by IDs", e);
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    @Override
    public GraphData expand(String vertexId, int depth) throws GraphException {
        // 使用 MATCH 匹配从起点出发的所有可达路径，返回点、边、关系
        String ngql = NebulaUtil.buildExpandPath(vertexId, depth);
        return query(ngql);
    }

    @Override
    public GraphData expand(String label, String property, String value, int depth, int limit) throws GraphException {
        String ngql = String.format(
                "LOOKUP ON %s WHERE %s.%s == '%s' YIELD id(vertex) AS src"
                + " | GO 1 TO %d STEPS FROM $-.src OVER * BIDIRECT YIELD $^ AS v1, $$ AS v2, edge AS e"
                + " | LIMIT %d",
                label, label, property, value, depth, limit);
        log.debug("Executing expand by property: {}", ngql);
        return query(ngql);
    }

    @Override
    public GraphData findPath(String startVertexId, String endVertexId, int maxDepth) throws GraphException {
        String ngql = NebulaUtil.buildShortestPath(startVertexId, endVertexId, maxDepth);
        return query(ngql);
    }

    @Override
    public GraphData findPath(PathQuery pathQuery) throws GraphException {
        PathQuery.Condition start = pathQuery.getStartProperty();
        PathQuery.Condition end = pathQuery.getEndProperty();
        String ngql = String.format(
                "LOOKUP ON %s WHERE %s.%s == '%s' YIELD id(vertex) AS src_id"
                + " | LOOKUP ON %s WHERE %s.%s == '%s' YIELD $-.src_id AS src_id, id(vertex) AS dst_id"
                + " | FIND ALL PATH FROM src_id TO dst_id OVER * UPTO %d STEPS YIELD PATH AS p"
                + " | LIMIT %d",
                start.getLabel(), start.getLabel(), start.getProperty(), start.getValue(),
                end.getLabel(), end.getLabel(), end.getProperty(), end.getValue(),
                pathQuery.getMaxDepth(), pathQuery.getLimit());
        log.debug("Executing findPath by property: {}", ngql);
        return query(ngql);
    }

    @Override
    public GraphSummary getSummary() throws GraphException {
        GraphSummary summary = new GraphSummary();
        try {
            // 提交STATS作业
            String submitStatsJob = NebulaUtil.buildSubmitJobStats();
            ResultSet submitResult = sessionPool.execute(submitStatsJob);
            if (!submitResult.isSucceeded()) {
                log.error("Failed to submit STATS job, errorCode: {}, errorMessage: {}",
                        submitResult.getErrorCode(), submitResult.getErrorMessage());
                throw new GraphException("Failed to submit STATS job, errorCode: " + submitResult.getErrorCode()
                        + ", errorMessage: " + submitResult.getErrorMessage());
            }

            // 获取作业ID
            long jobId = submitResult.rowValues(0).get(0).asLong();

            // 轮询检查作业状态，直到完成
            String showJob = NebulaUtil.buildShowJob(jobId);
            ResultSet jobResult = null;
            int retryCount = 0;
            // 最多重试30次，每次间隔100ms，总共3秒
            final int maxRetries = 30;
            while (retryCount < maxRetries) {
                Thread.sleep(1000);
                jobResult = sessionPool.execute(showJob);
                if (!jobResult.isSucceeded()) {
                    log.error("Failed to get job status, errorCode: {}, errorMessage: {}",
                            jobResult.getErrorCode(), jobResult.getErrorMessage());
                    throw new GraphException("Failed to get job status, errorCode: " + jobResult.getErrorCode()
                            + ", errorMessage: " + jobResult.getErrorMessage());
                }
                // 检查作业状态
                String status = jobResult.rowValues(1).get(2).asString();
                if ("FINISHED".equals(status)) {
                    break;
                }
                if ("FAILED".equals(status)) {
                    throw new GraphException("STATS job failed");
                }
                retryCount++;
            }

            if (retryCount >= maxRetries) {
                throw new GraphException("STATS job timeout");
            }

            // 获取统计信息
            String showStats = NebulaUtil.buildShowStats();
            ResultSet statsResult = sessionPool.execute(showStats);
            if (!statsResult.isSucceeded()) {
                log.error("Failed to show stats, errorCode: {}, errorMessage: {}",
                        statsResult.getErrorCode(), statsResult.getErrorMessage());
                throw new GraphException("Failed to show stats, errorCode: " + statsResult.getErrorCode()
                        + ", errorMessage: " + statsResult.getErrorMessage());
            }

            // 解析统计信息
            Map<String, Integer> vertexLabelCount = new HashMap<>();
            Map<String, Integer> edgeLabelCount = new HashMap<>();

            for (int i = 0; i < statsResult.rowsSize(); i++) {
                ResultSet.Record record = statsResult.rowValues(i);
                String type = record.get(0).asString();
                String name = record.get(1).asString();
                long count = record.get(2).asLong();

                switch (type) {
                    case "Tag" -> vertexLabelCount.put(name, (int) count);
                    case "Edge" -> edgeLabelCount.put(name, (int) count);
                    case "Space" -> {
                        if ("vertices".equals(name)) {
                            summary.setVertexCount((int) count);
                        } else if ("edges".equals(name)) {
                            summary.setEdgeCount((int) count);
                        }
                    }
                    default -> log.warn("Unknown stats type: {}", type);
                }
            }

            summary.setVertexLabelCount(vertexLabelCount);
            summary.setEdgeLabelCount(edgeLabelCount);
            summary.setGraphCode(nebulaConf.getGraphCode());

            return summary;
        } catch (Exception e) {
            log.error("Failed to get graph summary from Nebula", e);
            throw new GraphException("Failed to get graph summary from Nebula", e);
        }
    }

    @Override
    public long countVertices(String label) throws GraphException {
        String nql = NebulaUtil.buildCountVertex(label);
        try {
            ResultSet resultSet = sessionPool.execute(nql);
            if (resultSet.isSucceeded() && resultSet.rowsSize() > 0) {
                return resultSet.rowValues(0).get(0).asLong();
            }
        } catch (Exception e) {
            log.error("Failed to count vertices with label {} from Nebula", label, e);
        }
        return 0L;
    }

    @Override
    public long countEdges(String label) throws GraphException {
        String nql = NebulaUtil.buildCountEdge(label);
        try {
            ResultSet resultSet = sessionPool.execute(nql);
            if (resultSet.isSucceeded() && resultSet.rowsSize() > 0) {
                return resultSet.rowValues(0).get(0).asLong();
            }
        } catch (Exception e) {
            log.error("Failed to count edges with label {} from Nebula", label, e);
        }
        return 0L;
    }

    @Override
    public GraphVertex findVertex(String label, String property, String value) throws GraphException {
        try {
            String nql = NebulaUtil.buildFindVertexByProperty(label, property, value);
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.warn("Failed to find vertex by property, errorCode: {}, errorMessage: {}",
                        resultSet.getErrorCode(), resultSet.getErrorMessage());
                return null;
            }

            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                for (ValueWrapper valueWrapper : record.values()) {
                    if (valueWrapper.isVertex()) {
                        return NebulaUtil.parseVertex(valueWrapper.asNode());
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to find vertex by property: label={}, property={}, value={}, error={}",
                    label, property, value, e.getMessage());
            return null;
        }
    }

    private String edgeKey(GraphEdge edge) {
        if (edge.getUid() != null) {
            return edge.getUid();
        }
        return edge.getStartUid() + "->" + edge.getEndUid() + "#" + edge.getLabel();
    }

}