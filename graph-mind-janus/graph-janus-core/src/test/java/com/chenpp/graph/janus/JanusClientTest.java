package com.chenpp.graph.janus;

import com.alibaba.fastjson2.JSON;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.core.schema.IndexType;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * JanusGraph测试类
 *
 * @author April.Chen
 * @date 2025/8/13 09:53
 */
public class JanusClientTest {
    private JanusConf janusConf;
    private JanusClient janusClient;
    private GraphOperations graphOperations;
    private GraphDataOperations graphDataOperations;
    private JanusGraph graph;

    @Before
    public void init() {
        CassandraConf cassandraConf = new CassandraConf();
        cassandraConf.setHostname("10.58.12.60");
        cassandraConf.setPort(9042);
        cassandraConf.setUsername("cassandra");
        cassandraConf.setPassword("cassandra");
        cassandraConf.setKeyspace("cpp_test");

        janusConf = new JanusConf();
        janusConf.setGraphCode("cpp_test_janus");
        janusConf.setStorageBackend("cassandra");
        janusConf.setStorageHost("10.58.12.60");
        janusConf.setStoragePort(9042);
        janusConf.setCassandraConf(cassandraConf);

        System.out.println(JSON.toJSONString(janusConf));
        janusClient = new JanusClient(janusConf);
        assertNotNull(janusClient);
        graphOperations = janusClient.opsForGraph();
        graphDataOperations = janusClient.opsForGraphData();


        graph = janusClient.getGraph();
    }

    @After
    public void cleanup() {
        if (janusClient != null) {
            janusClient.close();
        }
    }

    @Test
    public void testCreateClient() {
        // 测试创建JanusGraph客户端
        janusClient = new JanusClient(janusConf);
        assertNotNull(janusClient);

        // 获取图操作实例
        GraphOperations graphOps = janusClient.opsForGraph();
        assertNotNull(graphOps);

        // 获取图数据操作实例
        GraphDataOperations graphDataOps = janusClient.opsForGraphData();
        assertNotNull(graphDataOps);

        // 检查连接（当前实现返回false）
        boolean connected = janusClient.checkConnection();
        assertFalse(connected);

        // 关闭客户端
        janusClient.close();
    }


    @Test
    public void testCreateGraph() {
        try {
            graphOperations.createGraph(janusConf);
            // 验证图实例是可用的
            assertTrue(graph.isOpen());
        } catch (Exception e) {
            fail("创建图时发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testDropGraph() {
        try {
            graphOperations.dropGraph(janusConf);
            // 验证图实例已关闭
            assertTrue(graph.isClosed());
        } catch (Exception e) {
            fail("删除图时发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testListGraphs() {
        try {
            List<Graph> graphs = graphOperations.listGraphs(janusConf);
            assertNotNull(graphs);
            assertEquals(1, graphs.size());
            assertEquals("cpp_test_graph", graphs.get(0).getCode());
        } catch (Exception e) {
            fail("列出图时发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testApplySchema() {
        try {
            // 创建图谱schema
            GraphSchema schema = new GraphSchema();

            // 创建实体列表
            List<GraphEntity> entities = new ArrayList<>();
            GraphEntity personEntity = new GraphEntity();
            personEntity.setLabel("person");

            // 添加属性
            List<GraphProperty> personProperties = new ArrayList<>();

            GraphProperty uidProperty = new GraphProperty();
            uidProperty.setCode("uid");
            uidProperty.setName("uid");
            uidProperty.setDataType(DataType.String);
            uidProperty.setNullable(false);
            personProperties.add(uidProperty);


            GraphProperty nameProperty = new GraphProperty();
            nameProperty.setCode("name");
            nameProperty.setName("姓名");
            nameProperty.setDataType(DataType.String);
            personProperties.add(nameProperty);

            GraphProperty ageProperty = new GraphProperty();
            ageProperty.setCode("age");
            ageProperty.setName("年龄");
            ageProperty.setDataType(DataType.Integer);
            personProperties.add(ageProperty);

            personEntity.setProperties(personProperties);
            entities.add(personEntity);

            // 创建关系列表
            List<GraphRelation> relations = new ArrayList<>();
            GraphRelation knowsRelation = new GraphRelation();
            knowsRelation.setLabel("knows");

            // 添加关系属性
            List<GraphProperty> relationProperties = new ArrayList<>();
            GraphProperty sinceProperty = new GraphProperty();
            sinceProperty.setCode("since");
            sinceProperty.setName("since");
            sinceProperty.setDataType(DataType.String);
            relationProperties.add(sinceProperty);
            relationProperties.add(uidProperty);

            knowsRelation.setProperties(relationProperties);
            relations.add(knowsRelation);

            // 创建索引列表
            List<GraphIndex> indexes = new ArrayList<>();
            GraphIndex nameIndex = new GraphIndex();
            nameIndex.setName("nameIndex");
            List<String> propertyNames = new ArrayList<>();
            propertyNames.add("name");
            nameIndex.setPropertyNames(propertyNames);
            nameIndex.setSchemaType("vertex");
            nameIndex.setType(IndexType.COMPOSITE.name());
            indexes.add(nameIndex);
            // 创建边索引
            GraphIndex edgeIndex = new GraphIndex();
            edgeIndex.setName("uidIndex");
            edgeIndex.setSchemaType("edge");
            edgeIndex.setType(IndexType.VERTEX_CENTRIC.name());
            edgeIndex.setPropertyNames(Collections.singletonList("uid"));
            edgeIndex.setLabel("knows");
            indexes.add(edgeIndex);

            schema.setEntities(entities);
            schema.setRelations(relations);
            schema.setIndexes(indexes);

            // 应用schema
            graphOperations.applySchema(janusConf, schema);

            // 验证schema是否正确应用
            assertNotNull(graph);
        } catch (Exception e) {
            e.printStackTrace();
            fail("应用图谱schema时发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testQuerySchema() {
        GraphSchema graphSchema = graphOperations.getPublishedSchema(janusConf);
        System.out.println(graphSchema);
    }

    @Test
    public void testGraphVertexOperations() throws GraphException {
        janusClient = new JanusClient(janusConf);
        GraphDataOperations graphDataOps = janusClient.opsForGraphData();

        // 创建顶点
        GraphVertex vertex = new GraphVertex();
        vertex.setUid("1");
        vertex.setLabel("person");

        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "Tom");
        properties.put("age", 25);
        vertex.setProperties(properties);

        GraphVertex addedVertex = graphDataOps.addVertex(vertex);
        assertNotNull(addedVertex);
        assertEquals("1", addedVertex.getUid());
        assertEquals("person", addedVertex.getLabel());

        // 更新顶点
        properties.put("age", 26);
        vertex.setProperties(properties);
        GraphVertex updatedVertex = graphDataOps.updateVertex(vertex);
        assertNotNull(updatedVertex);
        assertEquals(26, updatedVertex.getProperties().get("age"));

        // 删除顶点
        graphDataOps.deleteVertex(vertex);
    }

    @Test
    public void testGraphEdgeOperations() throws GraphException {
        janusClient = new JanusClient(janusConf);
        GraphDataOperations graphDataOps = janusClient.opsForGraphData();

        // 创建起始顶点
        GraphVertex startVertex = new GraphVertex();
        startVertex.setUid("1");
        startVertex.setLabel("person");
        Map<String, Object> startProperties = new HashMap<>();
        startProperties.put("name", "Tom");
        startVertex.setProperties(startProperties);
        graphDataOps.addVertex(startVertex);

        // 创建结束顶点
        GraphVertex endVertex = new GraphVertex();
        endVertex.setUid("2");
        endVertex.setLabel("person");
        Map<String, Object> endProperties = new HashMap<>();
        endProperties.put("name", "Jerry");
        endVertex.setProperties(endProperties);
        graphDataOps.addVertex(endVertex);

        // 创建边
        GraphEdge edge = new GraphEdge();
        edge.setUid("1");
        edge.setLabel("knows");
        edge.setStartUid("1");
        edge.setEndUid("2");
        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("since", "2020");
        edge.setProperties(edgeProperties);

        graphDataOps.addEdge(edge);

        // 更新边
        edgeProperties.put("since", "2021");
        edge.setProperties(edgeProperties);
        int updated = graphDataOps.updateEdge(edge);
        assertEquals(1, updated);

        // 删除边
        int deleted = graphDataOps.deleteEdge(edge);
        assertEquals(1, deleted);
    }

    @Test
    public void testBatchOperations() throws GraphException {
        janusClient = new JanusClient(janusConf);
        GraphDataOperations graphDataOps = janusClient.opsForGraphData();

        // 批量添加顶点
        List<GraphVertex> vertices = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            GraphVertex vertex = new GraphVertex();
            vertex.setUid(String.valueOf(i));
            vertex.setLabel("person");
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "Person" + i);
            vertex.setProperties(properties);
            vertices.add(vertex);
        }
        graphDataOps.addVertices(vertices);

        // 批量添加边
        List<GraphEdge> edges = new ArrayList<>();
        GraphEdge edge = new GraphEdge();
        edge.setUid("1");
        edge.setLabel("knows");
        edge.setStartUid("1");
        edge.setEndUid("2");
        edges.add(edge);

        GraphEdge edge2 = new GraphEdge();
        edge2.setUid("2");
        edge2.setLabel("knows");
        edge2.setStartUid("2");
        edge2.setEndUid("3");
        edges.add(edge2);

        graphDataOps.addEdges(edges);
    }

    @Test
    public void testGetVerticesByIds() throws GraphException {
        // 添加测试顶点
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("1");
        vertex1.setLabel("person");
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "Tom");
        vertex1.setProperties(properties1);
        graphDataOperations.addVertex(vertex1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("2");
        vertex2.setLabel("person");
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jerry");
        vertex2.setProperties(properties2);
        graphDataOperations.addVertex(vertex2);

        // 测试通过ID列表获取顶点
        List<String> vertexIds = new ArrayList<>();
        vertexIds.add("1");
        vertexIds.add("2");

        // 将graphDataOperations转换为JanusGraphDataOperations以访问新增方法
        JanusGraphDataOperations janusGraphDataOperations = (JanusGraphDataOperations) graphDataOperations;
        List<GraphVertex> vertices = janusGraphDataOperations.getVerticesByIds(vertexIds);

        assertNotNull(vertices);
        assertEquals(2, vertices.size());
        assertTrue(vertices.stream().anyMatch(v -> "1".equals(v.getUid()) && "Tom".equals(v.getProperties().get("name"))));
        assertTrue(vertices.stream().anyMatch(v -> "2".equals(v.getUid()) && "Jerry".equals(v.getProperties().get("name"))));
    }

    @Test
    public void testGetEdgesByIds() throws GraphException {
        // 添加测试顶点
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("1");
        vertex1.setLabel("person");
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "Tom");
        vertex1.setProperties(properties1);
        graphDataOperations.addVertex(vertex1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("2");
        vertex2.setLabel("person");
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jerry");
        vertex2.setProperties(properties2);
        graphDataOperations.addVertex(vertex2);

        // 添加测试边
        GraphEdge edge = new GraphEdge();
        edge.setUid("1");
        edge.setLabel("knows");
        edge.setStartUid("1");
        edge.setEndUid("2");
        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("since", "2020");
        edge.setProperties(edgeProperties);
        graphDataOperations.addEdge(edge);

        // 测试通过ID列表获取边
        List<String> edgeIds = new ArrayList<>();
        edgeIds.add("1");

        // 将graphDataOperations转换为JanusGraphDataOperations以访问新增方法
        JanusGraphDataOperations janusGraphDataOperations = (JanusGraphDataOperations) graphDataOperations;
        List<GraphEdge> edges = janusGraphDataOperations.getEdgesByIds(edgeIds);

        assertNotNull(edges);
        assertEquals(1, edges.size());
        GraphEdge retrievedEdge = edges.get(0);
        assertEquals("1", retrievedEdge.getUid());
        assertEquals("knows", retrievedEdge.getLabel());
        assertEquals("1", retrievedEdge.getStartUid());
        assertEquals("2", retrievedEdge.getEndUid());
        assertEquals("2020", retrievedEdge.getProperties().get("since"));
    }

    @Test
    public void testQueryVerticesByLabel() throws GraphException {
        // 添加测试顶点
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("1");
        vertex1.setLabel("person");
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "Tom");
        vertex1.setProperties(properties1);
        graphDataOperations.addVertex(vertex1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("2");
        vertex2.setLabel("person");
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jerry");
        vertex2.setProperties(properties2);
        graphDataOperations.addVertex(vertex2);

        // 注意：这里需要使用Gremlin查询顶点
        // 由于JanusGraphDataOperations中没有实现相关查询方法，暂时无法完成
    }

    @Test
    public void testQueryEdgesByLabel() throws GraphException {
        // 添加测试顶点
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("1");
        vertex1.setLabel("person");
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "Tom");
        vertex1.setProperties(properties1);
        graphDataOperations.addVertex(vertex1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("2");
        vertex2.setLabel("person");
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jerry");
        vertex2.setProperties(properties2);
        graphDataOperations.addVertex(vertex2);

        // 添加测试边
        GraphEdge edge = new GraphEdge();
        edge.setUid("1");
        edge.setLabel("knows");
        edge.setStartUid("1");
        edge.setEndUid("2");
        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("since", "2020");
        edge.setProperties(edgeProperties);
        graphDataOperations.addEdge(edge);

        // 注意：这里需要使用Gremlin查询边
        // 由于JanusGraphDataOperations中没有实现相关查询方法，暂时无法完成
    }
}