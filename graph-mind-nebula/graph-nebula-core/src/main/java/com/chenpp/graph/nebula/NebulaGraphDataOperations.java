package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/8/13 11:18
 */
@Slf4j
public class NebulaGraphDataOperations implements GraphDataOperations {
    private NebulaConf nebulaConf;
    private SessionPool sessionPool;

    public NebulaGraphDataOperations(NebulaConf nebulaConf) {
        this.nebulaConf = nebulaConf;
        this.sessionPool = NebulaClientFactory.getSessionPool(nebulaConf);
    }

    @Override
    public GraphVertex addVertex(GraphVertex vertex) throws GraphException {
        try {
            // 构建插入顶点的NGQL语句
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT VERTEX ").append(vertex.getLabel()).append(" (");

            // 添加属性键
            if (vertex.getProperties() != null && !vertex.getProperties().isEmpty()) {
                String propKeys = String.join(", ", vertex.getProperties().keySet());
                builder.append(propKeys);
            }

            builder.append(") VALUES \"").append(vertex.getUid()).append("\":(");

            // 添加属性值
            if (vertex.getProperties() != null && !vertex.getProperties().isEmpty()) {
                String propValues = vertex.getProperties().values().stream()
                        .map(this::formatValue)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(propValues);
            }

            builder.append(");");

            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to add vertex, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return vertex;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to add vertex due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to add vertex", e);
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        // Nebula中更新顶点使用UPDATE语法
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE VERTEX ON ").append(vertex.getLabel()).append(" \"")
                    .append(vertex.getUid()).append("\" ");

            // 添加SET子句
            if (vertex.getProperties() != null && !vertex.getProperties().isEmpty()) {
                builder.append("SET ");
                String setClause = vertex.getProperties().entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + formatValue(entry.getValue()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(setClause);
            }

            builder.append(";");

            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to update vertex, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return vertex;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to update vertex due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to update vertex", e);
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            return;
        }

        // 批量插入顶点
        for (GraphVertex vertex : vertices) {
            addVertex(vertex);
        }
    }

    @Override
    public void deleteVertex(GraphVertex vertex) throws GraphException {
        try {
            // 删除顶点及其关联的边
            String nql = "DELETE VERTEX \"" + vertex.getUid() + "\" WITH EDGE;";
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to delete vertex, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
        } catch (IOErrorException e) {
            throw new GraphException("Failed to delete vertex due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to delete vertex", e);
        }
    }

    @Override
    public void addEdge(GraphEdge edge) throws GraphException {
        try {
            // 构建插入边的NGQL语句
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT EDGE ").append(edge.getLabel()).append(" (");

            // 添加属性键
            if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                String propKeys = String.join(", ", edge.getProperties().keySet());
                builder.append(propKeys);
            }

            builder.append(") VALUES \"").append(edge.getStartUid()).append("\" -> \"")
                    .append(edge.getEndUid()).append("\":(");

            // 添加属性值
            if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                String propValues = edge.getProperties().values().stream()
                        .map(this::formatValue)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(propValues);
            }

            builder.append(");");

            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to add edge, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
        } catch (IOErrorException e) {
            throw new GraphException("Failed to add edge due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to add edge", e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            return;
        }

        // 批量插入边
        for (GraphEdge edge : edges) {
            addEdge(edge);
        }
    }

    @Override
    public int updateEdge(GraphEdge edge) throws GraphException {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE EDGE ON ").append(edge.getLabel()).append(" \"")
                    .append(edge.getStartUid()).append("\" -> \"").append(edge.getEndUid()).append("\" ");

            // 添加SET子句
            if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                builder.append("SET ");
                String setClause = edge.getProperties().entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + formatValue(entry.getValue()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(setClause);
            }

            builder.append(";");

            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to update edge, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return 1; // Nebula更新操作成功返回1
        } catch (IOErrorException e) {
            throw new GraphException("Failed to update edge due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to update edge", e);
        }
    }

    @Override
    public int deleteEdge(GraphEdge edge) throws GraphException {
        try {
            String nql = "DELETE EDGE " + edge.getLabel() + " \"" + edge.getStartUid()
                    + "\" -> \"" + edge.getEndUid() + "\";";
            log.info("Execute NGQL: {}", nql);

            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to delete edge, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            return 1; // 成功删除
        } catch (IOErrorException e) {
            throw new GraphException("Failed to delete edge due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to delete edge", e);
        }
    }

    @Override
    public GraphData query(String cypher) throws GraphException {
        try {
            log.info("Execute NGQL: {}", cypher);

            ResultSet resultSet = sessionPool.execute(cypher);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to execute query, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            GraphData graphData = new GraphData();
            graphData.setVertices(new ArrayList<>());
            graphData.setEdges(new ArrayList<>());

            // 解析结果集
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                for (ValueWrapper value : record.values()) {
                    if (value.isVertex()) {
                        GraphVertex vertex = parseVertex(value.asNode());
                        graphData.addVertex(vertex);
                    }
                    if (value.isEdge()) {
                        GraphEdge edge = parseEdge(value.asRelationship());
                        graphData.addEdge(edge);
                    }
                }
            }

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
        } catch (IOErrorException e) {
            throw new GraphException("Failed to execute query due to IO error", e);
        } catch (Exception e) {
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    /**
     * 解析Nebula Vertex为GraphVertex
     *
     * @param node Nebula Node对象
     * @return GraphVertex对象
     */
    private GraphVertex parseVertex(com.vesoft.nebula.client.graph.data.Node node) {
        GraphVertex graphVertex = new GraphVertex();

        // 解析顶点ID
        try {
            String vid = node.getId().asString();
            graphVertex.setUid(vid);

            // 解析标签和属性
            if (!node.tagNames().isEmpty()) {
                // 使用第一个标签作为顶点标签
                String label = node.tagNames().get(0);
                graphVertex.setLabel(label);

                // 解析属性
                Map<String, Object> properties = new HashMap<>();
                Map<String, ValueWrapper> nodeProps = node.properties(label);
                if (nodeProps != null) {
                    nodeProps.forEach((key, value) -> {
                        Object propValue = parseValueWrapper(value);
                        properties.put(key, propValue);
                    });
                }
                graphVertex.setProperties(properties);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse vertex due to encoding issue: {}", e.getMessage(), e);
        }

        return graphVertex;
    }

    /**
     * 解析Nebula Edge为GraphEdge
     *
     * @param relationship Nebula Relationship对象
     * @return GraphEdge对象
     */
    private GraphEdge parseEdge(com.vesoft.nebula.client.graph.data.Relationship relationship) {
        GraphEdge graphEdge = new GraphEdge();

        try {
            // 解析边的名称
            String edgeName = relationship.edgeName();
            graphEdge.setLabel(edgeName);


            // 解析起点和终点ID
            String srcId = relationship.srcId().asString();
            String dstId = relationship.dstId().asString();
            graphEdge.setStartUid(srcId);
            graphEdge.setEndUid(dstId);

            // 解析属性
            Map<String, Object> properties = new HashMap<>();
            Map<String, ValueWrapper> edgeProps = relationship.properties();
            if (edgeProps != null) {
                edgeProps.forEach((key, value) -> {
                    Object propValue = parseValueWrapper(value);
                    properties.put(key, propValue);
                });
            }
            graphEdge.setProperties(properties);
            graphEdge.setUid(properties.get("uid").toString());
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse edge due to encoding issue: {}", e.getMessage(), e);
        }

        return graphEdge;
    }

    /**
     * 解析ValueWrapper值为Java对象
     *
     * @param value ValueWrapper值
     * @return Java对象
     */
    private Object parseValueWrapper(ValueWrapper value) {
        try {
            if (value.isString()) {
                return value.asString();
            } else if (value.isLong()) {
                return value.asLong();
            } else if (value.isDouble()) {
                return value.asDouble();
            } else if (value.isBoolean()) {
                return value.asBoolean();
            } else if (value.isList()) {
                // 列表值
                List<Object> list = new ArrayList<>();
                for (ValueWrapper item : value.asList()) {
                    list.add(parseValueWrapper(item));
                }
                return list;
            } else if (value.isMap()) {
                // 映射值
                Map<String, Object> map = new HashMap<>();
                Map<String, ValueWrapper> valueMap = value.asMap();
                valueMap.forEach((key, val) -> {
                    map.put(key, parseValueWrapper(val));
                });
                return map;
            } else {
                // 其他类型默认转换为字符串
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse value: {}", e.getMessage(), e);
            return value.toString();
        }
    }

    /**
     * 格式化属性值，用于构建NGQL语句
     *
     * @param value 属性值
     * @return 格式化后的字符串
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }

        if (value instanceof Boolean) {
            return value.toString().toUpperCase();
        }

        return value.toString();
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
            String ngql = String.format("MATCH (v) WHERE id(v) IN [%s] RETURN v", idListStr);
            log.info("Execute NGQL: {}", ngql);

            ResultSet resultSet = sessionPool.execute(ngql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to query vertices by IDs, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            List<GraphVertex> vertices = new ArrayList<>();

            // 解析结果集
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                for (ValueWrapper value : record.values()) {
                    if (value.isVertex()) {
                        GraphVertex vertex = parseVertex(value.asNode());
                        vertices.add(vertex);
                    }
                }
            }

            return vertices;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to query vertices by IDs due to IO error", e);
        } catch (Exception e) {
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    /**
     * 根据ID列表查询边
     *
     * @param idList 边ID列表
     * @return 边列表
     * @throws GraphException 查询异常
     */
    public List<GraphEdge> getEdgesByIds(Collection<String> idList) throws GraphException {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }

        try {
            // 构建NGQL查询语句
            StringBuilder builder = new StringBuilder();
            builder.append("MATCH (src)-[e]->(dst) WHERE e.uid IN [");

            // 添加ID列表
            String idListStr = idList.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(Collectors.joining(", "));
            builder.append(idListStr);

            builder.append("] RETURN e, src, dst");

            String ngql = builder.toString();
            log.info("Execute NGQL: {}", ngql);

            ResultSet resultSet = sessionPool.execute(ngql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to query edges by IDs, errorCode: " + resultSet.getErrorCode()
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }

            List<GraphEdge> edges = new ArrayList<>();

            // 解析结果集
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                // 遍历记录中的值
                for (ValueWrapper value : record.values()) {
                    if (value.isEdge()) {
                        GraphEdge edge = parseEdge(value.asRelationship());
                        edges.add(edge);
                    }
                }
            }

            return edges;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to query edges by IDs due to IO error", e);
        } catch (Exception e) {
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }
}