package com.chenpp.graph.nebula;

import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.Graph;
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

    @Before
    public void init() {
        nebulaConf = new NebulaConf();
        nebulaConf.setHosts("10.57.36.17,10.57.36.18,10.57.36.19");
        nebulaConf.setPort(9660);
        nebulaConf.setUsername("root");
        nebulaConf.setPassword("nebula");
        nebulaConf.setSpace("cpp_test_2503");
        nebulaConf.setGraphCode("cpp_test_2503");


        nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        sessionPool = NebulaClientFactory.getSessionPool(nebulaConf);

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
}
