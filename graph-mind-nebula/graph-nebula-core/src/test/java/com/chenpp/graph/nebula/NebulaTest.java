package com.chenpp.graph.nebula;

import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.nebula.NebulaGraphOperations;
import com.chenpp.graph.nebula.util.NebulaUtil;
import com.vesoft.nebula.Vertex;
import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/4/9 15:53
 */
public class NebulaTest {
    NebulaPool nebulaPool;
    NebulaConf nebulaConf;

    SessionPool sessionPool;
    NebulaClient nebulaClient;

    private String graphCode = "cpp_test_001";

    @Before
    public void init() {
        nebulaConf = new NebulaConf();
        nebulaConf.setHosts("10.57.36.17,10.57.36.18,10.57.36.19");
        nebulaConf.setPort(9660);
        nebulaConf.setUsername("root");
        nebulaConf.setPassword("nebula");
        nebulaConf.setGraphCode(graphCode);
        nebulaConf.setSpace(graphCode);
//        nebulaConf.setSpace("cpp_test_2503");
//        nebulaConf.setGraphCode("cpp_test_2503");


        nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
//        sessionPool = NebulaClientFactory.getSessionPool(nebulaConf);

        nebulaClient = new NebulaClient(nebulaConf);
    }

    @Test
    public void testShowSpaces() {
        List<Graph> graphs = nebulaClient.opsForGraph().listGraphs(nebulaConf);
        graphs.forEach(graph -> System.out.println(graph.getCode()));
    }

    @Test
    public void testDropSpace() {
        nebulaConf.setGraphCode("rt_sub_old_realtime_test");
        nebulaClient.opsForGraph().dropGraph(nebulaConf);
    }

    @Test
    public void testCreateSpace() {
        // 创建一个新的space配置
        NebulaConf newSpaceConf = new NebulaConf();
        newSpaceConf.setHosts(nebulaConf.getHosts());
        newSpaceConf.setPort(nebulaConf.getPort());
        newSpaceConf.setUsername(nebulaConf.getUsername());
        newSpaceConf.setPassword(nebulaConf.getPassword());
        newSpaceConf.setGraphCode(graphCode);
        newSpaceConf.setSpace(graphCode);
        newSpaceConf.setPartitionNum(1);
        newSpaceConf.setReplicaFactor(1);
        newSpaceConf.setVidFixedStrLength(32);

        try {
            // 创建space
            nebulaClient.opsForGraph().createGraph(newSpaceConf);
            System.out.println("创建space成功: " + newSpaceConf.getSpace());

            // 验证space是否创建成功
            List<Graph> spaces = nebulaClient.opsForGraph().listGraphs(nebulaConf);
            boolean spaceExists = spaces.stream()
                    .anyMatch(graph -> graph.getCode().equals(newSpaceConf.getSpace()));

            if (spaceExists) {
                System.out.println("验证space创建成功");
            } else {
                System.out.println("验证space创建失败");
            }
        } catch (Exception e) {
            System.err.println("创建space失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testApplySchema() {
        try {
            // 创建测试用的GraphSchema
            GraphSchema schema = new GraphSchema();

            // 创建实体(标签)
            GraphEntity personEntity = new GraphEntity();
            personEntity.setLabel("person");

            GraphProperty uidProperty = new GraphProperty();
            uidProperty.setCode("uid");
            uidProperty.setName("uid");
            uidProperty.setDataType(DataType.String);

            GraphProperty nameProperty = new GraphProperty();
            nameProperty.setCode("name");
            nameProperty.setName("name");
            nameProperty.setDataType(DataType.String);

            GraphProperty ageProperty = new GraphProperty();
            ageProperty.setCode("age");
            ageProperty.setName("age");
            ageProperty.setDataType(DataType.Integer);

            personEntity.setProperties(Arrays.asList(uidProperty, nameProperty, ageProperty));

            // 创建关系(边)
            GraphRelation friendRelation = new GraphRelation();
            friendRelation.setLabel("friend");

            GraphProperty sinceProperty = new GraphProperty();
            sinceProperty.setCode("since");
            sinceProperty.setName("since");
            sinceProperty.setDataType(DataType.String);

            friendRelation.setProperties(Arrays.asList(uidProperty, sinceProperty));
            friendRelation.setSourceLabel("person");
            friendRelation.setTargetLabel("person");

            GraphIndex index = new GraphIndex();
            index.setName("idx_person_uid");
            index.setSchemaType(GraphConstants.VERTEX);
            index.setLabel("person");
            index.setProperty("uid");
            index.setUnique(true);

            GraphIndex edgeIndex = new GraphIndex();
            edgeIndex.setName("idx_friend_uid");
            edgeIndex.setSchemaType(GraphConstants.EDGE);
            edgeIndex.setLabel("friend");
            edgeIndex.setProperty("uid");
            edgeIndex.setUnique(true);

            // 设置schema
            schema.setEntities(Arrays.asList(personEntity));
            schema.setRelations(Arrays.asList(friendRelation));
            schema.setIndexes(Arrays.asList(index, edgeIndex));

            // 应用schema
            nebulaClient.opsForGraph().applySchema(nebulaConf, schema);
            System.out.println("应用schema成功");

            // 等待一段时间确保space创建完成
            Thread.sleep(1000);

            // 验证标签是否创建成功
            // 需要重新创建客户端以确保使用正确的space
            NebulaClient newClient = new NebulaClient(nebulaConf);
            List<GraphEntity> tags = newClient.opsForGraph().getPublishedSchema(nebulaConf).getEntities();
            boolean tagExists = tags.stream().anyMatch(entity -> "person".equals(entity.getLabel()));
            System.out.println("标签'person'创建" + (tagExists ? "成功" : "失败"));

            // 验证边是否创建成功
            List<GraphRelation> edges = newClient.opsForGraph().getPublishedSchema(nebulaConf).getRelations();
            boolean edgeExists = edges.stream().anyMatch(relation -> "friend".equals(relation.getLabel()));
            System.out.println("边'friend'创建" + (edgeExists ? "成功" : "失败"));

        } catch (Exception e) {
            System.err.println("应用schema失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testQuery() {
        String nql = "match (v:telinfo) return v limit 10";
        try {
            ResultSet resultSet = sessionPool.execute(nql);
            resultSet.getRows().forEach(row -> {
                row.getValues().forEach(value -> {
                    Vertex vertex = value.getVVal();
                    GraphVertex graphVertex = new GraphVertex();
                    String vid = new String(vertex.getVid().getSVal(), StandardCharsets.UTF_8);

                    graphVertex.setUid(vid);
                    vertex.getTags().forEach(tag -> {
                        String label = new String(tag.getName(), StandardCharsets.UTF_8);
                        graphVertex.setLabel(label);
                        Map<String, Object> properties = new HashMap<>();
                        tag.getProps().forEach((k, v) -> {
                            String key = new String(k, StandardCharsets.UTF_8);
                            String val = new String(v.getSVal(), StandardCharsets.UTF_8);
                            properties.put(key, val);
                        });
                        graphVertex.setProperties(properties);
                    });
                    System.out.println(graphVertex);
                });
                System.out.println("----------");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printResult(ResultSet resultSet) throws UnsupportedEncodingException {
        List<String> colNames = resultSet.keys();
        for (String name : colNames) {
            System.out.printf("%15s |", name);
        }
        System.out.println();
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            ResultSet.Record record = resultSet.rowValues(i);
            for (ValueWrapper value : record.values()) {
                if (value.isLong()) {
                    System.out.printf("%15s |", value.asLong());
                }
                if (value.isBoolean()) {
                    System.out.printf("%15s |", value.asBoolean());
                }
                if (value.isDouble()) {
                    System.out.printf("%15s |", value.asDouble());
                }
                if (value.isString()) {
                    System.out.printf("%15s |", value.asString());
                }
                if (value.isTime()) {
                    System.out.printf("%15s |", value.asTime());
                }
                if (value.isDate()) {
                    System.out.printf("%15s |", value.asDate());
                }
                if (value.isDateTime()) {
                    System.out.printf("%15s |", value.asDateTime());
                }
                if (value.isVertex()) {
                    System.out.printf("%15s |", value.asNode());
                }
                if (value.isEdge()) {
                    System.out.printf("%15s |", value.asRelationship());
                }
                if (value.isPath()) {
                    System.out.printf("%15s |", value.asPath());
                }
                if (value.isList()) {
                    System.out.printf("%15s |", value.asList());
                }
                if (value.isSet()) {
                    System.out.printf("%15s |", value.asSet());
                }
                if (value.isMap()) {
                    System.out.printf("%15s |", value.asMap());
                }
            }
            System.out.println();
        }
    }


    @Test
    public void testShowTags() throws Exception {
        String showTags = NebulaUtil.buildShowTags();
        if (sessionPool == null) {
            sessionPool = NebulaClientFactory.getSessionPool(nebulaConf);
        }
        ResultSet resultSet = sessionPool.execute(showTags);
        List<String> tags = new ArrayList<>();
        resultSet.getRows().forEach(row -> {
            row.getValues().forEach(value -> {
                String tag = new String(value.getSVal(), StandardCharsets.UTF_8);
                tags.add(tag);
            });
        });

        System.out.println("tags:" + tags);
        tags.forEach(tag -> {
            String descTag = NebulaUtil.buildDescribeTag(tag);
            try {
                ResultSet rs = sessionPool.execute(descTag);
                for (int i = 0; i < rs.rowsSize(); i++) {
                    ResultSet.Record record = rs.rowValues(i);
                    ValueWrapper field = record.get(0);
                    ValueWrapper type = record.get(1);
                    String fieldName = field.asString();
                    String typeName = type.asString();
//
                    System.out.println(tag + ":" + fieldName + ":" + typeName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


    }

    @Test
    public void testShowSchema() {
        GraphSchema schema = nebulaClient.opsForGraph().getPublishedSchema(nebulaConf);
        System.out.println(schema);
    }

    @Test
    public void testAddVertex() {
        // 创建顶点对象
        GraphVertex vertex = new GraphVertex();
        vertex.setUid("test_vertex_001");
        vertex.setLabel("person");

        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        properties.put("age", 30);
        vertex.setProperties(properties);

        // 添加顶点
        try {
            GraphVertex result = nebulaClient.opsForGraphData().addVertex(vertex);
            System.out.println("添加顶点成功: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBatchAddVertices() {
        List<GraphVertex> vertices = new ArrayList<>();
        // 创建顶点对象
        GraphVertex vertex1 = new GraphVertex();
        vertex1.setUid("test_vertex_001");
        vertex1.setLabel("person");

        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "John Doe");
        properties1.put("age", 30);
        properties1.put("uid", "test_vertex_001");
        vertex1.setProperties(properties1);

        GraphVertex vertex2 = new GraphVertex();
        vertex2.setUid("test_vertex_002");
        vertex2.setLabel("person");

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "Jane Smith");
        properties2.put("age", 25);
        properties2.put("uid", "test_vertex_002");
        vertex2.setProperties(properties2);

        vertices.add(vertex1);
        vertices.add(vertex2);

        nebulaClient.opsForGraphData().addVertices(vertices);
    }

    @Test
    public void testUpdateVertex() {
        // 创建顶点对象
        GraphVertex vertex = new GraphVertex();
        vertex.setUid("test_vertex_001");
        vertex.setLabel("person");

        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Smith");
        properties.put("age", 31);
        properties.put("uid", "test_vertex_001");
        vertex.setProperties(properties);

        // 更新顶点
        NebulaGraphDataOperations dataOps = new NebulaGraphDataOperations(nebulaConf);
        try {
            GraphVertex result = dataOps.updateVertex(vertex);
            System.out.println("更新顶点成功: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteVertex() {
        // 创建顶点对象
        GraphVertex vertex = new GraphVertex();
        vertex.setUid("test_vertex_001");

        // 删除顶点
        try {
            nebulaClient.opsForGraphData().deleteVertex(vertex);
            System.out.println("删除顶点成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAddEdge() {
        // 创建边对象
        GraphEdge edge = new GraphEdge();
        edge.setLabel("friend");
        edge.setStartUid("test_vertex_001");
        edge.setEndUid("test_vertex_002");

        Map<String, Object> properties = new HashMap<>();
        properties.put("since", "2022-01-01");
        edge.setProperties(properties);

        // 添加边
        try {
            nebulaClient.opsForGraphData().addEdge(edge);
            System.out.println("添加边成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateEdge() {
        // 创建边对象
        GraphEdge edge = new GraphEdge();
        edge.setLabel("friend");
        edge.setStartUid("test_vertex_001");
        edge.setEndUid("test_vertex_002");

        Map<String, Object> properties = new HashMap<>();
        properties.put("since", "2023-01-01");
        properties.put("uid", "FRI001");
        edge.setProperties(properties);

        // 更新边
        try {
            int result = nebulaClient.opsForGraphData().updateEdge(edge);
            System.out.println("更新边成功，影响行数: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteEdge() {
        // 创建边对象
        GraphEdge edge = new GraphEdge();
        edge.setLabel("friend");
        edge.setStartUid("test_vertex_001");
        edge.setEndUid("test_vertex_002");

        // 删除边
        NebulaGraphDataOperations dataOps = new NebulaGraphDataOperations(nebulaConf);
        try {
            int result = dataOps.deleteEdge(edge);
            System.out.println("删除边成功，影响行数: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testQueryVertex() {
        NebulaGraphDataOperations dataOps = new NebulaGraphDataOperations(nebulaConf);
        try {
            // 查询顶点
            String ngql = "MATCH (v:person) WHERE id(v) == \"test_vertex_001\" RETURN v LIMIT 1";
            GraphData result = dataOps.query(ngql);
            System.out.println("查询顶点结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testQueryEdge() {
        NebulaGraphDataOperations dataOps = new NebulaGraphDataOperations(nebulaConf);
        try {
            // 查询边
            String ngql = "MATCH (v1:person)-[e:friend]->(v2:person) WHERE id(v1) == \"test_vertex_001\" RETURN e LIMIT 1";
            GraphData result = dataOps.query(ngql);
            System.out.println("查询边结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDataCRUD() {
        NebulaGraphDataOperations dataOps = new NebulaGraphDataOperations(nebulaConf);

        try {
            // 1. 添加顶点1
            GraphVertex vertex1 = new GraphVertex();
            vertex1.setUid("test_user_1");
            vertex1.setLabel("person");

            Map<String, Object> properties1 = new HashMap<>();
            properties1.put("name", "Alice");
            properties1.put("age", 25);
            properties1.put("city", "Beijing");
            vertex1.setProperties(properties1);

            dataOps.addVertex(vertex1);
            System.out.println("添加顶点1成功");

            // 2. 添加顶点2
            GraphVertex vertex2 = new GraphVertex();
            vertex2.setUid("test_user_2");
            vertex2.setLabel("person");

            Map<String, Object> properties2 = new HashMap<>();
            properties2.put("name", "Bob");
            properties2.put("age", 28);
            properties2.put("city", "Shanghai");
            vertex2.setProperties(properties2);

            dataOps.addVertex(vertex2);
            System.out.println("添加顶点2成功");

            // 3. 添加边
            GraphEdge edge = new GraphEdge();
            edge.setLabel("friend");
            edge.setStartUid("test_user_1");
            edge.setEndUid("test_user_2");

            Map<String, Object> edgeProperties = new HashMap<>();
            edgeProperties.put("since", "2023-01-01");
            edge.setProperties(edgeProperties);

            dataOps.addEdge(edge);
            System.out.println("添加边成功");

            // 4. 查询数据
            String ngql = "MATCH (v1:person)-[e:friend]->(v2:person) WHERE id(v1) == \"test_user_1\" RETURN v1, e, v2 LIMIT 1";
            GraphData result = dataOps.query(ngql);
            System.out.println("查询结果: " + result);

            // 5. 更新顶点
            vertex1.getProperties().put("age", 26);
            dataOps.updateVertex(vertex1);
            System.out.println("更新顶点成功");

            // 6. 更新边
            edge.getProperties().put("relation", "close friend");
            dataOps.updateEdge(edge);
            System.out.println("更新边成功");

            // 7. 删除边
            dataOps.deleteEdge(edge);
            System.out.println("删除边成功");

            // 8. 删除顶点
            dataOps.deleteVertex(vertex1);
            dataOps.deleteVertex(vertex2);
            System.out.println("删除顶点成功");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateIndex() {
        try {
            // 先确保space存在
            NebulaGraphOperations graphOps = new NebulaGraphOperations(nebulaConf);

            // 创建一个索引
            GraphIndex index = new GraphIndex();
            index.setName("idx_person_name");
            index.setSchemaType(GraphConstants.VERTEX);
            index.setLabel("person");

            GraphProperty nameProperty = new GraphProperty();
            nameProperty.setCode("name");
            nameProperty.setName("name");
            nameProperty.setDataType(DataType.String);

            index.setPropertyNames(Arrays.asList("name"));
            index.setUnique(false);

            // 获取NebulaPool并创建Session
            NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
            try (com.vesoft.nebula.client.graph.net.Session session =
                         nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {

                // 创建索引
                graphOps.createIndices(Arrays.asList(index), session);
                System.out.println("创建索引成功: " + index.getName());

                // 验证索引是否创建成功
                List<GraphIndex> indexes = graphOps.showIndexes(session);
                boolean indexExists = indexes.stream()
                        .anyMatch(idx -> index.getName().equals(idx.getName()));

                if (indexExists) {
                    System.out.println("验证索引创建成功");
                } else {
                    System.out.println("验证索引创建失败");
                }
            }
        } catch (Exception e) {
            System.err.println("创建索引失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testGetSummary() {
        GraphSummary summary = nebulaClient.opsForGraphData().getSummary();
        System.out.println("图空间摘要: " + summary);
    }

}
