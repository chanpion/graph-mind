package com.chenpp.graph.neo4j;

//import com.chenpp.graph.api.exception.GraphException;
//import com.chenpp.graph.api.model.GraphData;
//import com.chenpp.graph.api.model.GraphEdge;
//import com.chenpp.graph.api.model.GraphVertex;
//import com.chenpp.graph.neo4j.util.Neo4jUtil;

import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.neo4j.util.Neo4jUtil;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author April.Chen
 * @date 2024/5/21 10:06
 */
public class Neo4jTest {
    private Neo4jConf neo4jConf;
    private Driver driver;

    @Before
    public void init() {
        neo4jConf = new Neo4jConf();
        neo4jConf.setUri("neo4j://localhost:7687");
        neo4jConf.setUsername("neo4j");
        neo4jConf.setPassword("neo4j123");
        driver = Neo4jClientFactory.createNeo4jDriver(neo4jConf);
    }

    @Test
    public void testGetDriver() {

        System.out.println(driver.isEncrypted());
    }


    @Test
    public void testQuery() {
        Map<String, Object> params = Maps.newHashMap();
        params.put("born", 1970);
        EagerResult result = driver.executableQuery("MATCH (p:Person {born: $born}) RETURN p.name AS name")
                .withParameters(params)
                .withConfig(QueryConfig.builder().withDatabase("neo4j").build())
                .execute();

        ResultSummary summary = result.summary();
        List<Record> records = result.records();
        System.out.printf("The query %s returned %d records in %d ms.%n",
                summary.query(), records.size(),
                summary.resultAvailableAfter(TimeUnit.MILLISECONDS));

        records.forEach(record -> {
            System.out.println(record.get("name").asString());
        });
    }

    @Test
    public void testReadSession() {
        String sql = "match (n) return count(n)";
        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
            Record record = session.executeRead(tx -> tx.run(sql).single());
            System.out.println(record.get(0).asInt());
        }
    }

    public void testWriteSession() {
        String sql = "match (n) return count(n)";
        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
            Record record = session.executeWrite(tx -> tx.run(sql).single());
            System.out.println(record.get(0).asInt());
        }
    }

    @Test
    public void testQueryNode() {
        String sql = "match (n) return n limit 10";
        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
            List<GraphVertex> vertices = session.executeRead(tx -> tx.run(sql).list(record -> {
                Node node = record.get(0).asNode();
                GraphVertex vertex = new GraphVertex();
                vertex.setUid(node.id() + "");
                vertex.setLabel(node.labels().iterator().next());
                vertex.setProperties(node.asMap());
                return vertex;
            }));

            vertices.forEach(System.out::println);
        }
    }

    @Test
    public void testQueryRelation() {
        String sql = "MATCH p=(m)-[r:ACTED_IN]->(n) RETURN p LIMIT 10";
        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
            session.executeRead(tx -> tx.run(sql).list(record -> {
                record.values().forEach(value -> {
                    if (value instanceof PathValue) {
                        Path path = value.asPath();
                        path.nodes().forEach(node -> {
                            GraphVertex vertex = convertToVertex(node);
                            System.out.println(vertex);
                        });
                        path.relationships().forEach(relationship -> {
                            GraphEdge edge = convertToEdge(relationship);
                            System.out.println(edge);
                        });
                    }
                });
                return null;
            }));
        }
    }

    public GraphVertex convertToVertex(Node node) {
        GraphVertex vertex = new GraphVertex();
        vertex.setUid(node.elementId());
        vertex.setLabel(node.labels().iterator().next());
        vertex.setProperties(node.asMap());
        node.elementId();
        return vertex;
    }

    public GraphEdge convertToEdge(Relationship relationship) {
        GraphEdge edge = new GraphEdge();
        String startId = relationship.startNodeElementId();
        String endId = relationship.endNodeElementId();
        edge.setUid(relationship.elementId());
        edge.setStartUid(startId);
        edge.setEndUid(endId);
        edge.setLabel(relationship.type());
        edge.setProperties(relationship.asMap());
        return edge;
    }

    @Test
    public void testCreateIndex() {
        String cypher = "CREATE INDEX [IndexName] FOR (n:LabelName) ON (n.propertyName)";
        String uniqueCypher = "CREATE CONSTRAINT [ConstraintName] FOR (n:LabelName) REQUIRE n.<propertyKey> IS UNIQUE";
    }


    @Test
    public void testAddVertex() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "同盾科技");
        properties.put("uid", "C0001");
        properties.put("create_time",  LocalDateTime.now());


        try (Session session = driver.session()) {
            String cypher = String.format(
                    "CREATE (n:%s {id: $id}) SET n += $properties RETURN n",
                    "Company"
            );

            Map<String, Object> params = new HashMap<>();
            params.put("properties", properties);
            params.put("id", properties.get("uid"));

            Record record = session.run(cypher, params).single();
            GraphVertex vertex = convertNodeToVertex(record.get(0).asNode());
            System.out.println(vertex);
        }
    }

    @Test
    public void testUpdateVertex() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "同盾科技有限公司");
        properties.put("uid", "C0001");
        properties.put("create_time",  LocalDateTime.now());

        GraphVertex v = new GraphVertex();
        v.setLabel("Company");
        v.setProperties( properties);


        try (Session session = driver.session()) {
            String cypher = String.format(
                    "MATCH (n:%s {id: $id}) SET n += $properties RETURN n",
                    v.getLabel()
            );

            Map<String, Object> params = new HashMap<>();
            params.put("properties", properties);
            params.put("id", properties.get("uid"));

            Record record = session.run(cypher, params).single();
            GraphVertex vertex = convertNodeToVertex(record.get("n").asNode());
            System.out.println(vertex);
        }
    }


    // 辅助方法：将Neo4j Node转换为GraphVertex
    private GraphVertex convertNodeToVertex(Node node) {
        GraphVertex vertex = new GraphVertex();
        vertex.setUid(node.get("id").asString());
        vertex.setLabel(node.labels().iterator().next());

        Map<String, Object> properties = new HashMap<>();
        node.asMap().forEach((key, value) -> {
            if (!"id".equals(key)) {
                properties.put(key, value);
            }
        });
        vertex.setProperties(properties);

        return vertex;
    }
    @Test
    public void testAddEdge() {
        Map<String, Object> props = new HashMap<>();
        props.put("date", "2025-01-01");

        try (Session session = driver.session()) {
            String propertiesClause = Neo4jUtil.buildPropertiesClause(props);
            String query = String.format("MATCH (a:%s {%s: $fromValue}),(b:%s {%s: $toValue})  CREATE (a)-[r:%s %s]->(b)",
                    "person", "name", "person", "name", "friend", propertiesClause);

            System.out.println("query: " + query);
            Map<String, Object> parameters = new HashMap<>(props);
            parameters.put("fromValue", "张三");
            parameters.put("toValue", "李四");

            int addCount = session.executeWrite(tx -> {
                Result result = tx.run(query, parameters);
                return result.consume().counters().relationshipsCreated();
            });
            System.out.println("addCount: " + addCount);
        } catch (Exception e) {
            throw new GraphException("Failed to create relationship in Neo4j", e);
        }
    }

    @Test
    public void testQueryEdge() {
        String query = "MATCH p=()-[r:ACTED_IN]->() RETURN p LIMIT 1";
        GraphData graphData = new GraphData();
        Set<String> vertexIds = new HashSet<>();
        Set<String> edgeIds = new HashSet<>();
        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
            session.executeRead(tx -> tx.run(query).list(record -> {
                record.values().forEach(value -> {
                    if (value instanceof PathValue) {
                        Path path = value.asPath();
                        path.nodes().forEach(node -> {
                            if (!vertexIds.contains(node.elementId())) {
                                GraphVertex vertex = convertToVertex(node);
                                graphData.addVertex(vertex);
                                vertexIds.add(node.elementId());
                            }
                        });
                        path.relationships().forEach(relationship -> {
                            if (!edgeIds.contains(relationship.elementId())) {
                                GraphEdge edge = convertToEdge(relationship);
                                graphData.addEdge(edge);
                                edgeIds.add(relationship.elementId());
                            }
                        });
                    } else {
                        System.out.println(value.getClass());
                    }
                });
                return null;
            }));
        }

        System.out.println(graphData);
    }

    @Test
    public void testBatchAddVertex() {
        List<Map<String, Object>> vertices = new ArrayList<>();

        Map<String, Object> props = new HashMap<>();
        props.put("age", 21);
        props.put("name", "王五");
        vertices.add(props);

        Map<String, Object> props1 = new HashMap<>();
        props1.put("age", 21);
        props1.put("name", "赵六");
        vertices.add(props1);

        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {

            // 使用 UNWIND 批量插入
            String cypherQuery =
                    "UNWIND $batch AS row " +
                            "MERGE (n:person {name: row.name, age: row.age})";

            // 构建参数化批量数据
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("batch", vertices);

            // 执行查询
            session.run(cypherQuery, parameters);
            System.out.println("批量插入完成！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testBatchAddEdge() {
        try (Session session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
            // 模拟批量插入的关系数据
            List<Map<String, Object>> relationshipsData = new ArrayList<>();
            Map<String, Object> rel1 = new HashMap<>();
            rel1.put("startName", "Alice");
            rel1.put("endName", "Bob");
            rel1.put("relationshipType", "FRIEND");
            relationshipsData.add(rel1);

            Map<String, Object> rel2 = new HashMap<>();
            rel2.put("startName", "Bob");
            rel2.put("endName", "Charlie");
            rel2.put("relationshipType", "FRIEND");
            relationshipsData.add(rel2);

            // 构建 Cypher 查询语句
            String query = "UNWIND $relationships AS rel " +
                    "MATCH (a:Person {name: rel.startName}), (b:Person {name: rel.endName}) " +
                    "CREATE (a)-[r: {relationshipType}]->(b) RETURN r";

            // 执行批量插入
            session.writeTransaction(tx -> {
                tx.run(query, Values.parameters("relationships", relationshipsData));
                return null;
            });

            System.out.println("批量插入关系完成");
        }
    }

    @Test
    public void testIndex() {
        String label = "";
        String property = "";

        String createIndexCypher = "CREATE INDEX [IndexName] FOR (n:LabelName) ON (n.propertyName)";
        String createUniqueIndexCypher = "CREATE CONSTRAINT [ConstraintName] FOR (n:LabelName) REQUIRE n.<propertyKey> IS UNIQUE";
        String dropIndexCypher = "DROP INDEX [IndexName]";
        String dropConstraintCypher = "DROP CONSTRAINT [ConstraintName]";

        String cypher = String.format("CREATE INDEX %s FOR (n:%s) ON (n.%s)", label);

    }

    @Test
    public void testComplexQuery() {

        String query = "MATCH p=(a:Person {name: 'Alice'})-[r:FRIEND*2..3]-(b:Person) RETURN p";
        String c1 = "match p=(a:Movie|Person)-[r]->(b)  return p";
    }
}