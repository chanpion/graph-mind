package com.chenpp.graph.janus;

import com.alibaba.fastjson2.JSON;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.core.schema.IndexType;
import org.janusgraph.core.JanusGraph;
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
 * JanusGraph使用HBase作为存储的测试类
 *
 * @author April.Chen
 * @date 2025/10/10
 */
public class JanusHBaseTest {
    private JanusConf janusConf;
    private JanusClient janusClient;
    private GraphOperations graphOperations;
    private GraphDataOperations graphDataOperations;
    private JanusGraph graph;

    @Before
    public void init() {
        // 配置HBase存储
        HBaseConf hbaseConf = new HBaseConf();
        hbaseConf.setHbaseHost("10.57.36.17,10.57.36.18,10.57.36.19");
        hbaseConf.setHbasePort(2182);
        hbaseConf.setHbaseZnode("/hbase");
        hbaseConf.setHbaseNs("cpp");
        hbaseConf.setHbaseRegionNum(9);

        janusConf = new JanusConf();
        janusConf.setGraphCode("cpp_test_janus_hbase");
        janusConf.setStorageBackend("hbase");
        janusConf.setStorageHost("10.57.36.17,10.57.36.18,10.57.36.19");
        janusConf.setStoragePort(2182);
        janusConf.setHBaseConf(hbaseConf);

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
        assertTrue(connected);

        // 关闭客户端
        janusClient.close();
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
            knowsRelation.setMultiple(true);

            // 添加关系属性
            List<GraphProperty> relationProperties = new ArrayList<>();
            GraphProperty sinceProperty = new GraphProperty();
            sinceProperty.setCode("since");
            sinceProperty.setName("since");
            sinceProperty.setDataType(DataType.Datetime);
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
    public void testGraphVertexOperations() {
        try {
            // 创建顶点
            GraphVertex vertex = new GraphVertex();
            vertex.setUid("1");
            vertex.setLabel("person");

            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "Tom");
            properties.put("age", 25);
            vertex.setProperties(properties);

            GraphVertex addedVertex = graphDataOperations.addVertex(vertex);
            assertNotNull(addedVertex);
            assertEquals("1", addedVertex.getUid());
            assertEquals("person", addedVertex.getLabel());

            // 更新顶点
            properties.put("age", 26);
            vertex.setProperties(properties);
            GraphVertex updatedVertex = graphDataOperations.updateVertex(vertex);
            assertNotNull(updatedVertex);
            assertEquals(26, updatedVertex.getProperties().get("age"));

            // 删除顶点
            graphDataOperations.deleteVertex(vertex);
        } catch (Exception e) {
            e.printStackTrace();
            fail("顶点操作测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testGraphEdgeOperations() {
        try {
            // 创建起始顶点
            GraphVertex startVertex = new GraphVertex();
            startVertex.setUid("1");
            startVertex.setLabel("person");
            Map<String, Object> startProperties = new HashMap<>();
            startProperties.put("name", "Tom");
            startVertex.setProperties(startProperties);
            graphDataOperations.addVertex(startVertex);

            // 创建结束顶点
            GraphVertex endVertex = new GraphVertex();
            endVertex.setUid("2");
            endVertex.setLabel("person");
            Map<String, Object> endProperties = new HashMap<>();
            endProperties.put("name", "Jerry");
            endVertex.setProperties(endProperties);
            graphDataOperations.addVertex(endVertex);

            // 创建边
            GraphEdge edge = new GraphEdge();
            edge.setUid("1");
            edge.setLabel("knows");
            edge.setStartUid("1");
            edge.setEndUid("2");
            Map<String, Object> edgeProperties = new HashMap<>();
            edgeProperties.put("since", "2020");
            edge.setProperties(edgeProperties);

            graphDataOperations.addEdge(edge);

            // 更新边
            edgeProperties.put("since", "2021");
            edge.setProperties(edgeProperties);
            GraphEdge updated = graphDataOperations.updateEdge(edge);
            assertEquals(1, updated);

            // 删除边
            int deleted = graphDataOperations.deleteEdge(edge);
            assertEquals(1, deleted);
        } catch (Exception e) {
            e.printStackTrace();
            fail("边操作测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testBatchOperations() {
        try {
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
            graphDataOperations.addVertices(vertices);

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

            graphDataOperations.addEdges(edges);
        } catch (Exception e) {
            e.printStackTrace();
            fail("批量操作测试失败: " + e.getMessage());
        }
    }


    @Test
    public void testQuery() {
        GraphData graphData = graphDataOperations.query("g.V().path()");
        System.out.println(graphData);
        GraphData allData = graphDataOperations.query("g.E()");
        System.out.println("Vertices: ");
        allData.getVertices().forEach(System.out::println);
        System.out.println("Edges: ");
        allData.getEdges().forEach(System.out::println);
    }

    @Test
    public void testExpand() throws GraphException {
        // 先添加测试数据
        // 添加测试顶点
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("expand_1");
        vertex1.setLabel("person");
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "Tom");
        vertex1.setProperties(properties1);
        graphDataOperations.addVertex(vertex1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("expand_2");
        vertex2.setLabel("person");
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jerry");
        vertex2.setProperties(properties2);
        graphDataOperations.addVertex(vertex2);

        // 添加测试边
        GraphEdge edge = new GraphEdge();
        edge.setUid("expand_edge_1");
        edge.setLabel("knows");
        edge.setStartUid("expand_1");
        edge.setEndUid("expand_2");
        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("since", "2020");
        edge.setProperties(edgeProperties);
        graphDataOperations.addEdge(edge);

        // 测试expand方法
        GraphData expandedData = graphDataOperations.expand("expand_1", 1);
        System.out.println("Expanded data: " + expandedData);
        assertNotNull(expandedData);
        // 验证返回的数据不为空
        assertTrue(expandedData.getVertices().size() > 0);
        // 验证至少有一条边被返回
        assertTrue(expandedData.getEdges().size() > 0);
    }

    @Test
    public void testFindPath() throws GraphException {
        // 先添加测试数据
        // 添加测试顶点
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("path_1");
        vertex1.setLabel("person");
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "Tom");
        vertex1.setProperties(properties1);
        graphDataOperations.addVertex(vertex1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("path_2");
        vertex2.setLabel("person");
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jerry");
        vertex2.setProperties(properties2);
        graphDataOperations.addVertex(vertex2);

        // 添加测试边
        GraphEdge edge = new GraphEdge();
        edge.setUid("path_edge_1");
        edge.setLabel("knows");
        edge.setStartUid("path_1");
        edge.setEndUid("path_2");
        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("since", "2020");
        edge.setProperties(edgeProperties);
        graphDataOperations.addEdge(edge);

        // 测试findPath方法
        GraphData pathData = graphDataOperations.findPath("path_1", "path_2", 3);
        System.out.println("Path data: " + pathData);
        assertNotNull(pathData);
        // 验证返回的数据不为空
        assertTrue(pathData.getVertices().size() > 0);
        // 验证至少有一条边被返回
        assertTrue(pathData.getEdges().size() > 0);
    }
}