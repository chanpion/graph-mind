package com.chenpp.graph.neo4j.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/4/30 13:52
 */
@Slf4j
public class Neo4jUtil {

    public static String buildPropertiesClause(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder().append(" {");
        int index = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": $").append(entry.getKey());
            index++;
        }
        sb.append("} ");
        return sb.toString();
    }

    public static Driver connect(String uri, String user, String password) {
        try {
            Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            log.info("Successfully connected to Neo4j at {}", uri);
            return driver;
        } catch (Exception e) {
            log.error("Failed to connect to Neo4j at {}", uri, e);
            throw e;
        }
    }

    public static void executeCypher(Driver driver, String cypher) {
        try {
            driver.session().run(cypher);
            log.debug("Successfully executed cypher: {}", cypher);
        } catch (Exception e) {
            log.error("Failed to execute cypher: {}", cypher, e);
            throw new GraphException("Failed to execute cypher: " + cypher, e);
        }
    }

    public static GraphVertex parseVertex(Node node) {
        if (node == null) {
            log.warn("Attempted to parse null node");
            return null;
        }

        GraphVertex vertex = new GraphVertex();
        vertex.setUid(getNodePropertyAsString(node, "uid"));
        vertex.setLabel(node.labels().iterator().hasNext() ? node.labels().iterator().next() : "");
        Map<String, Object> properties = node.asMap();
        vertex.setProperties(properties);
        vertex.setId(node.elementId());
        return vertex;
    }

    public static GraphEdge parseEdge(Relationship relationship) {
        if (relationship == null) {
            log.warn("Attempted to parse null relationship");
            return null;
        }

        GraphEdge edge = new GraphEdge();
        edge.setId(relationship.elementId());
        edge.setUid(getRelationshipPropertyAsString(relationship, "uid"));
        edge.setStartUid(relationship.startNodeElementId());
        edge.setEndUid(relationship.endNodeElementId());
        edge.setProperties(relationship.asMap());
        edge.setLabel(relationship.type());
        return edge;
    }

    public static GraphData parseGraphData(Path path) {
        if (path == null) {
            log.warn("Attempted to parse null path");
            return new GraphData();
        }

        GraphData graphData = new GraphData();
        path.nodes().forEach(node -> graphData.addVertex(parseVertex(node)));
        path.relationships().forEach(relationship -> graphData.addEdge(parseEdge(relationship)));
        return graphData;
    }

    public static <T> Map<String, Object> convertToMap(T obj) {
        if (obj == null) {
            return Map.of();
        }
        return JSON.parseObject(JSON.toJSONString(obj), new TypeReference<>() {
        });
    }

    private static String getNodePropertyAsString(Node node, String propertyName) {
        try {
            return node.get(propertyName).asString();
        } catch (Exception e) {
            log.debug("Failed to get property {} from node: {}", propertyName, e.getMessage());
            return "";
        }
    }

    private static String getRelationshipPropertyAsString(Relationship relationship, String propertyName) {
        try {
            return relationship.get(propertyName).asString();
        } catch (Exception e) {
            log.debug("Failed to get property {} from relationship: {}", propertyName, e.getMessage());
            return String.valueOf(relationship.id());
        }
    }

    public static GraphData parseResult(Result result) {
        GraphData graphData = new GraphData();

        Map<String, GraphVertex> elementIdVertexMap = new HashMap<>();

        while (result.hasNext()) {
            Record record = result.next();
            List<Value> values = record.values();
            for (Value value : values) {
                // 解析节点
                if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.NODE())) {
                    Node node = value.asNode();
                    GraphVertex vertex = Neo4jUtil.parseVertex(node);
                    graphData.addVertex(vertex);
                    elementIdVertexMap.put(node.elementId(), vertex);
                }
                // 解析关系
                else if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP())) {
                    Relationship relationship = value.asRelationship();
                    GraphEdge edge = Neo4jUtil.parseEdge(relationship);
                    graphData.addEdge(edge);
                }
                // 解析路径
                else if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.PATH())) {
                    InternalPath path = (InternalPath) value.asPath();
                    for (Node node : path.nodes()) {
                        GraphVertex vertex = Neo4jUtil.parseVertex(node);
                        graphData.addVertex(vertex);
                        elementIdVertexMap.put(node.elementId(), vertex);
                    }
                    for (Relationship relationship : path.relationships()) {
                        GraphEdge edge = Neo4jUtil.parseEdge(relationship);
                        graphData.addEdge(edge);
                    }
                }
            }
        }

        graphData.getEdges().forEach(edge -> {
            GraphVertex start = elementIdVertexMap.get(edge.getStartUid());
            edge.setStartUid(start.getUid());
            edge.setStartLabel(start.getLabel());
            GraphVertex end = elementIdVertexMap.get(edge.getEndUid());
            edge.setEndUid(end.getUid());
            edge.setEndLabel(end.getLabel());
        });
        return graphData;
    }
}