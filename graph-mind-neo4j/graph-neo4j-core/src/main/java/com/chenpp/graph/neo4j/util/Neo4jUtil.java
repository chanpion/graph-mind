package com.chenpp.graph.neo4j.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author April.Chen
 * @date 2025/4/30 13:52
 */
@Slf4j
public class Neo4jUtil {
    public static String buildCreateSingleIndex(String indexName, String labelName, String propertyName) {
        if (indexName == null || indexName.isEmpty()) {
            indexName = String.format("idx_%s_%s", labelName, propertyName);
        }
        return String.format("CREATE INDEX %s FOR (n:%s) ON (n.%s)", indexName, labelName, propertyName);
    }

    public static boolean isEnterpriseEdition(Driver driver) {
        try (Session session = driver.session()) {
            Result result = session.run("CALL dbms.components() YIELD edition RETURN edition");
            if (result.hasNext()) {
                String edition = result.next().get(0).asString();
                return "enterprise".equalsIgnoreCase(edition);
            }
        } catch (Exception e) {
            log.error("Not enterprise edition: {}", e.getMessage());
        }
        return false;
    }

    public static String getDefaultDatabaseName(Driver driver) {
        try (Session session = driver.session()) {
            Result dbResult = session.run("SHOW DEFAULT DATABASE");
            if (dbResult.hasNext()) {
                String name = dbResult.next().get("name").asString();
                if (StringUtils.isNotBlank(name)) {
                    return name;
                }
            }
        }
        return "neo4j";
    }

    public static String resolveDatabase(Driver driver, String graphCode) {
        if (StringUtils.isNotBlank(graphCode)) {
            try (Session session = driver.session(SessionConfig.builder().withDatabase(graphCode).build())) {
                session.run("RETURN 1").consume();
                return graphCode;
            } catch (Exception e) {
                log.debug("Database '{}' not available, using default: {}", graphCode, e.getMessage());
            }
        }
        return getDefaultDatabaseName(driver);
    }

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
        String uid = getNodePropertyAsString(node, GraphConstants.UID);
        if (StringUtils.isBlank(uid)) {
            uid = node.elementId();
        }
        vertex.setUid(uid);
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

        String uid = getRelationshipPropertyAsString(relationship, GraphConstants.UID);
        if (StringUtils.isBlank(uid)) {
            uid = relationship.elementId();
        }

        edge.setId(relationship.elementId());
        edge.setUid(uid);
        edge.setStartUid(relationship.startNodeElementId());
        edge.setEndUid(relationship.endNodeElementId());
        edge.setProperties(relationship.asMap());
        edge.setLabel(relationship.type());
        return edge;
    }

    public static <T> Map<String, Object> convertToMap(T obj) {
        if (obj == null) {
            return Map.of();
        }
        return JSON.parseObject(JSON.toJSONString(obj), new TypeReference<>() {
        });
    }

    public static String safeGetString(Record record, String key) {
        try {
            Value value = record.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asString();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> List<T> safeGetList(Record record, String key, Function<Value, T> converter) {
        try {
            Value value = record.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asList(converter);
        } catch (Exception e) {
            return null;
        }
    }

    public static String safeGetSingleFromList(Record record, String key) {
        List<String> list = safeGetList(record, key, Value::asString);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    private static String getNodePropertyAsString(Node node, String propertyName) {
        try {
            Value value = node.get(propertyName);
            if (value == null || value.isNull()) {
                return "";
            }
            return value.asString();
        } catch (Exception e) {
            log.debug("Failed to get property {} from node: {}", propertyName, e.getMessage());
            return "";
        }
    }

    private static String getRelationshipPropertyAsString(Relationship relationship, String propertyName) {
        try {
            Value value = relationship.get(propertyName);
            if (value == null || value.isNull()) {
                return "";
            }
            return value.asString();
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
                    if (elementIdVertexMap.containsKey(node.elementId())) {
                        continue;
                    }
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
                        if (elementIdVertexMap.containsKey(node.elementId())) {
                            continue;
                        }
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
        if (graphData.getEdges() != null) {
            graphData.getEdges().forEach(edge -> {
                GraphVertex start = elementIdVertexMap.get(edge.getStartUid());
                if (start != null) {
                    edge.setStartUid(start.getUid());
                    edge.setStartLabel(start.getLabel());
                }
                GraphVertex end = elementIdVertexMap.get(edge.getEndUid());
                if (end != null) {
                    edge.setEndUid(end.getUid());
                    edge.setEndLabel(end.getLabel());
                }
            });
        }
        return graphData;
    }

    public static GraphProperty parseProperty(Record record) {
        String propertyName = Neo4jUtil.safeGetString(record, "propertyName");
        if (StringUtils.isBlank(propertyName)) {
            return null;
        }

        String typeStr = Neo4jUtil.safeGetSingleFromList(record, "propertyTypes");
        DataType dataType = DataType.instanceOf(typeStr);
        if (dataType == null) {
            dataType = DataType.String;
        }

        GraphProperty prop = new GraphProperty();
        prop.setCode(propertyName);
        prop.setName(propertyName);
        prop.setDataType(dataType);
        return prop;
    }
}