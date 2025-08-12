package com.chenpp.graph.neo4j.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/4/30 13:52
 */
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
        return GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public static void executeCypher(Driver driver, String cypher) {
        driver.session().run(cypher);
    }

    public static GraphVertex parseVertex(Node node) {
        GraphVertex vertex = new GraphVertex();
        vertex.setUid(node.get("uid").asString());
        vertex.setLabel(node.labels().iterator().next());
        Map<String, Object> properties = node.asMap();
        vertex.setProperties(properties);
        vertex.setId(node.elementId());
        return vertex;
    }

    public static GraphEdge parseEdge(Relationship relationship) {
        GraphEdge edge = new GraphEdge();
        edge.setId(relationship.elementId());
        edge.setUid(relationship.get("uid").asString());
        edge.setStartUid(relationship.startNodeElementId());
        edge.setEndUid(relationship.endNodeElementId());
        edge.setProperties(relationship.asMap());
        edge.setLabel(relationship.type());
        return edge;
    }

    public static GraphData parseGraphData(Path path) {
        GraphData graphData = new GraphData();
        path.nodes().forEach(node -> graphData.addVertex(parseVertex(node)));
        path.relationships().forEach(relationship -> graphData.addEdge(parseEdge(relationship)));
        return graphData;
    }

    public static <T> Map<String, Object> convertToMap(T obj) {
        return JSON.parseObject(JSON.toJSONString(obj), new TypeReference<Map<String, Object>>() {
        });
    }

}
