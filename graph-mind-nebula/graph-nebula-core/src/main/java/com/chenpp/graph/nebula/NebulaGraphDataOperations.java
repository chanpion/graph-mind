package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.PathQuery;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.nebula.schema.NebulaEdge;
import com.chenpp.graph.nebula.schema.NebulaProperty;
import com.chenpp.graph.nebula.schema.NebulaTag;
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
            // 获取该标签的属性类型映射
            Map<String, DataType> propertyTypes = getPropertyTypes(vertex.getLabel(), true);

            String nql = NebulaUtil.buildInsertVertex(vertex, propertyTypes);
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
        try {
            String vid = vertex.getUid();
            Map<String, DataType> propertyTypes = getPropertyTypes(vertex.getLabel(), true);
            String setClause = vertex.getProperties().entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + NebulaUtil.formatValue(entry.getValue(), propertyTypes != null ? propertyTypes.get(entry.getKey()) : null))
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
            Map<String, List<GraphVertex>> verticesByLabel = vertices.stream()
                    .filter(vertex -> MapUtils.isNotEmpty(vertex.getProperties()))
                    .collect(Collectors.groupingBy(GraphVertex::getLabel));

            verticesByLabel.forEach((label, vertexList) -> {
                if (CollectionUtils.isEmpty(vertexList)) {
                    return;
                }
                String keys = String.join(",", vertexList.get(0).getProperties().keySet());

                Map<String, DataType> propertyTypes = getPropertyTypes(label, true);

                String valuesClause = vertexList.stream().map(vertex -> {
                    String propValues = NebulaUtil.buildPropertyValuesClause(vertex.getProperties(), propertyTypes);
                    return String.format("\"%s\":(%s)", vertex.getUid(), propValues);
                }).collect(Collectors.joining(","));

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
            Map<String, DataType> propertyTypes = getPropertyTypes(edge.getLabel(), false);

            // 构建插入边的NGQL语句
            // 语法: INSERT EDGE edge_type (prop1, prop2) VALUES src_vid -> dst_vid @rank: (val1, val2)
            String keys = String.join(",", edge.getProperties().keySet());
            String propValues = NebulaUtil.buildPropertyValuesClause(edge.getProperties(), propertyTypes);
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
            Map<String, List<GraphEdge>> edgesByLabel = edges.stream()
                    .collect(Collectors.groupingBy(GraphEdge::getLabel));

            for (Map.Entry<String, List<GraphEdge>> entry : edgesByLabel.entrySet()) {
                String label = entry.getKey();
                List<GraphEdge> edgeList = entry.getValue();

                if (edgeList.isEmpty()) {
                    continue;
                }

                GraphEdge firstEdge = edgeList.get(0);
                String propKeys = "";
                if (firstEdge.getProperties() != null && !firstEdge.getProperties().isEmpty()) {
                    propKeys = String.join(", ", firstEdge.getProperties().keySet());
                }

                Map<String, DataType> propertyTypes = getPropertyTypes(label, false);

                StringBuilder valuesBuilder = new StringBuilder();
                String valuesStr = edgeList.stream().map(edge -> {
                    String propValues = NebulaUtil.buildPropertyValuesClause(edge.getProperties(), propertyTypes);
                    return String.format("\"%s\" -> \"%s\":(%s)", edge.getStartUid(), edge.getEndUid(), propValues);
                }).collect(Collectors.joining(", "));
                valuesBuilder.append(valuesStr);

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
            Map<String, DataType> propertyTypes = getPropertyTypes(edge.getLabel(), false);

            String setClause = edge.getProperties().entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + NebulaUtil.formatValue(entry.getValue(), propertyTypes != null ? propertyTypes.get(entry.getKey()) : null))
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

            Map<String, GraphVertex> vertexMap = new LinkedHashMap<>();
            Map<String, GraphEdge> edgeMap = new LinkedHashMap<>();

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
            Set<String> vertexIds = vertexMap.values().stream().filter(v -> v.getProperties() == null)
                    .map(GraphVertex::getUid).collect(Collectors.toSet());
            GraphData graphData = new GraphData();
            graphData.setVertices(new ArrayList<>(vertexMap.values()));
            graphData.setEdges(new ArrayList<>(edgeMap.values()));

            if (CollectionUtils.isNotEmpty(graphData.getEdges()) && CollectionUtils.isEmpty(graphData.getVertices())) {
                graphData.getEdges().forEach(e -> {
                    vertexIds.add(e.getStartUid());
                    vertexIds.add(e.getEndUid());
                });

            }
            List<GraphVertex> vertices = this.getVerticesByIds(vertexIds);
            graphData.setVertices(vertices);
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
            String idListStr = idList.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", "));
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
        String startVid;
        String endVid;
        if (start.getProperty().equals(GraphConstants.UID)) {
            startVid = start.getValue();
        } else {
            GraphVertex startVertex = findVertex(start.getLabel(), start.getProperty(), start.getValue());
            if (startVertex == null) {
                log.warn("Start vertex not found: label={}, property={}, value={}",
                        start.getLabel(), start.getProperty(), start.getValue());
                throw new GraphException("Start vertex not found");
            }
            startVid = startVertex.getId();
        }

        if (GraphConstants.UID.equals(end.getProperty())) {
            endVid = end.getValue();
        } else {
            GraphVertex endVertex = findVertex(end.getLabel(), end.getProperty(), end.getValue());
            if (endVertex == null) {
                log.warn("End vertex not found: label={}, property={}, value={}",
                        end.getLabel(), end.getProperty(), end.getValue());
                throw new GraphException("End vertex not found");
            }
            endVid = endVertex.getId();
        }

        String pathNgql = String.format(
                "FIND ALL PATH FROM \"%s\" TO \"%s\" OVER * UPTO %d STEPS YIELD PATH AS p | LIMIT %d",
                startVid, endVid, pathQuery.getMaxDepth(), pathQuery.getLimit());
        log.info("Execute path query NGQL: {}", pathNgql);

        return query(pathNgql);
    }

    @Override
    public GraphSummary getSummary() throws GraphException {
        GraphSummary summary = new GraphSummary();
        try {
            String submitStatsJob = NebulaUtil.buildSubmitJobStats();
            ResultSet submitResult = sessionPool.execute(submitStatsJob);
            if (!submitResult.isSucceeded()) {
                log.error("Failed to submit STATS job, errorCode: {}, errorMessage: {}",
                        submitResult.getErrorCode(), submitResult.getErrorMessage());
                throw new GraphException("Failed to submit STATS job, errorCode: " + submitResult.getErrorCode()
                        + ", errorMessage: " + submitResult.getErrorMessage());
            }

            long jobId = submitResult.rowValues(0).get(0).asLong();

            String showJob = NebulaUtil.buildShowJob(jobId);
            ResultSet jobResult = null;
            int retryCount = 0;
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

            String showStats = NebulaUtil.buildShowStats();
            ResultSet statsResult = sessionPool.execute(showStats);
            if (!statsResult.isSucceeded()) {
                log.error("Failed to show stats, errorCode: {}, errorMessage: {}",
                        statsResult.getErrorCode(), statsResult.getErrorMessage());
                throw new GraphException("Failed to show stats, errorCode: " + statsResult.getErrorCode()
                        + ", errorMessage: " + statsResult.getErrorMessage());
            }

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

    private Map<String, DataType> getPropertyTypes(String label, boolean isVertex) {
        String nql = isVertex ? NebulaUtil.buildDescribeTag(label) : NebulaUtil.buildDescribeEdge(label);
        try {
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                log.warn("Failed to describe tag/edge {}, errorCode: {}, errorMessage: {}",
                        label, resultSet.getErrorCode(), resultSet.getErrorMessage());
                return null;
            }

            Map<String, DataType> propertyTypes = new HashMap<>();
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                String fieldName = record.get(0).asString();
                String typeName = record.get(1).asString();
                DataType dataType = DataType.instanceOf(typeName);
                propertyTypes.put(fieldName, dataType);
            }
            return propertyTypes;
        } catch (Exception e) {
            log.warn("Failed to get property types for {}: {}", label, e.getMessage());
            return null;
        }
    }
}