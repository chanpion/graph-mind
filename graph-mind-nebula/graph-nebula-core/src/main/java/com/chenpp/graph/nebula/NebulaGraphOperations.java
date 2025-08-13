package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.nebula.exception.NebulaException;
import com.chenpp.graph.nebula.ngql.NGQLBuilder;
import com.chenpp.graph.nebula.schema.NebulaDataType;
import com.chenpp.graph.nebula.schema.NebulaProperty;
import com.chenpp.graph.nebula.schema.NebulaTag;
import com.chenpp.graph.nebula.util.NebulaUtil;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.chenpp.graph.nebula.schema.NebulaProperty.FIXED_STRING_SIZE;

/**
 * @author April.Chen
 * @date 2025/4/9 15:23
 */
@Slf4j
public class NebulaGraphOperations implements GraphOperations {

    private NGQLBuilder ngqlBuilder = new NGQLBuilder(ZoneOffset.of("+8"));
    private NebulaConf nebulaConf;

    public NebulaPool getConnection(GraphConf graphConf) {
        NebulaConf nebulaConf = (NebulaConf) graphConf;
        return NebulaClientFactory.getNebulaPool(nebulaConf);
    }

    @Override
    public void createGraph(GraphConf graphConf) {
        NebulaConf nebulaConf = (NebulaConf) graphConf;
        String nql = NebulaUtil.buildCreateSpace(nebulaConf);
        ResultSet resultSet = execute(nebulaConf, nql);
        if (!resultSet.isSucceeded()) {
            throw new GraphException("create graph failed, errorCode: " + resultSet.getErrorCode() + ", errorMessage: " + resultSet.getErrorMessage());
        }
    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {
        NebulaConf nebulaConf = (NebulaConf) graphConf;
        String nql = NebulaUtil.buildDropSpace(nebulaConf.getGraphCode());
        ResultSet resultSet = execute(nebulaConf, nql);
        if (!resultSet.isSucceeded()) {
            throw new GraphException("drop graph failed, errorCode: " + resultSet.getErrorCode() + ", errorMessage: " + resultSet.getErrorMessage());
        }
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) {
        NebulaConf nebulaConf = (NebulaConf) graphConf;
        String nql = "SHOW SPACES";
        ResultSet resultSet = execute(nebulaConf, nql);
        if (!resultSet.isSucceeded()) {
            throw new GraphException("list graph failed, errorCode: " + resultSet.getErrorCode() + ", errorMessage: " + resultSet.getErrorMessage());
        }
        return resultSet.getRows().stream().map(row -> {
            String space = new String(row.getValues().get(0).getSVal(), StandardCharsets.UTF_8);
            Graph graph = new Graph();
            graph.setCode(space);
            return graph;
        }).collect(Collectors.toList());
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) {
        log.info("begin apply graph schema");
        NebulaConf nebulaConf = (NebulaConf) graphConf;
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);

        String useSpace = ngqlBuilder.buildUseSpace(nebulaConf.getSpace());

        ResultSet rs = execute(nebulaConf, useSpace);
        if (!rs.isSucceeded()) {
            return;
        }
        createTags(graphSchema.getEntities());
        createEdges(graphSchema.getRelations());
        createIndices(graphSchema.getIndexes());
    }


    private ResultSet execute(NebulaConf nebulaConf, String nql) {
        log.info("execute ngql: {}", nql);
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        try (Session session = nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
            return session.execute(nql);
        } catch (Exception e) {
            log.error("nebula execute error", e);
            throw new GraphException(e);
        }
    }


    public void createTags(List<GraphEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        
        entities.forEach(entity -> {
            try {
                // 构建创建Tag的NGQL语句
                StringBuilder builder = new StringBuilder();
                builder.append("CREATE TAG IF NOT EXISTS ").append(entity.getLabel()).append(" (");
                
                // 添加属性定义
                if (entity.getProperties() != null && !entity.getProperties().isEmpty()) {
                    List<String> propDefs = new ArrayList<>();
                    for (com.chenpp.graph.core.schema.GraphProperty prop : entity.getProperties()) {
                        StringBuilder propBuilder = new StringBuilder();
                        propBuilder.append(prop.getCode()).append(" ")
                                .append(convertToNebulaDataType(prop.getDataType()));
                        
                        // 处理字符串类型长度
                        if (prop.getDataType() == com.chenpp.graph.core.schema.DataType.String) {
                            propBuilder.append("(").append(NebulaProperty.FIXED_STRING_SIZE).append(")");
                        }
                        
                        propDefs.add(propBuilder.toString());
                    }
                    builder.append(String.join(", ", propDefs));
                }
                
                builder.append(")");
                
                // 执行创建Tag的语句
                String nql = builder.toString();
                ResultSet resultSet = execute(nebulaConf, nql);
                if (!resultSet.isSucceeded()) {
                    log.warn("Failed to create tag: {}, errorCode: {}, errorMessage: {}", 
                            entity.getLabel(), resultSet.getErrorCode(), resultSet.getErrorMessage());
                } else {
                    log.info("Successfully created tag: {}", entity.getLabel());
                }
            } catch (Exception e) {
                log.error("Error creating tag: " + entity.getLabel(), e);
            }
        });
    }

    public void createEdges(List<GraphRelation> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }
        
        edges.forEach(edge -> {
            try {
                // 构建创建Edge的NGQL语句
                StringBuilder builder = new StringBuilder();
                builder.append("CREATE EDGE IF NOT EXISTS ").append(edge.getLabel()).append(" (");
                
                // 添加属性定义
                if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                    List<String> propDefs = new ArrayList<>();
                    for (com.chenpp.graph.core.schema.GraphProperty prop : edge.getProperties()) {
                        StringBuilder propBuilder = new StringBuilder();
                        propBuilder.append(prop.getCode()).append(" ")
                                .append(convertToNebulaDataType(prop.getDataType()));
                        
                        // 处理字符串类型长度
                        if (prop.getDataType() == com.chenpp.graph.core.schema.DataType.String) {
                            propBuilder.append("(").append(NebulaProperty.FIXED_STRING_SIZE).append(")");
                        }
                        
                        propDefs.add(propBuilder.toString());
                    }
                    builder.append(String.join(", ", propDefs));
                }
                
                builder.append(")");
                
                // 执行创建Edge的语句
                String nql = builder.toString();
                ResultSet resultSet = execute(nebulaConf, nql);
                if (!resultSet.isSucceeded()) {
                    log.warn("Failed to create edge: {}, errorCode: {}, errorMessage: {}", 
                            edge.getLabel(), resultSet.getErrorCode(), resultSet.getErrorMessage());
                } else {
                    log.info("Successfully created edge: {}", edge.getLabel());
                }
            } catch (Exception e) {
                log.error("Error creating edge: " + edge.getLabel(), e);
            }
        });
    }

    public void createIndices(List<GraphIndex> indices) {
        if (indices == null || indices.isEmpty()) {
            return;
        }
        
        indices.forEach(index -> {
            try {
                // 构建创建索引的NGQL语句
                StringBuilder builder = new StringBuilder();
                builder.append("CREATE INDEX IF NOT EXISTS ").append(index.getName())
                        .append(" ON ").append(index.getLabel()).append(" (");
                
                // 添加索引属性
                if (index.getPropertyNames() != null && !index.getPropertyNames().isEmpty()) {
                    builder.append(String.join(", ", index.getPropertyNames()));
                }
                
                builder.append(")");
                
                // 执行创建索引的语句
                String nql = builder.toString();
                ResultSet resultSet = execute(nebulaConf, nql);
                if (!resultSet.isSucceeded()) {
                    log.warn("Failed to create index: {}, errorCode: {}, errorMessage: {}", 
                            index.getName(), resultSet.getErrorCode(), resultSet.getErrorMessage());
                } else {
                    log.info("Successfully created index: {}", index.getName());
                }
            } catch (Exception e) {
                log.error("Error creating index: " + index.getName(), e);
            }
        });
    }

    public void showTags() {
        String nql = NebulaUtil.buildShowTags();
        ResultSet resultSet = execute(nebulaConf, nql);
        String colName = resultSet.getColumnNames().get(0);
        List<String> tagNames = resultSet.colValues(colName).stream()
                .map(value -> new String(value.getValue().getSVal(), StandardCharsets.UTF_8))
                .collect(Collectors.toList());
    }

    public NebulaTag describeTag(String tagName) throws NebulaException, UnsupportedEncodingException {
        String nql = ngqlBuilder.buildDescribeTag(tagName);
        ResultSet rs = execute(nebulaConf, nql);

        List<NebulaProperty> properties = new ArrayList<>();

        for (int i = 0; i < rs.rowsSize(); i++) {
            ResultSet.Record record = rs.rowValues(i);
            ValueWrapper field = record.get(0);
            ValueWrapper type = record.get(1);
            String fieldName = field.asString();
            String typeName = type.asString();

            NebulaProperty nebulaProperty = new NebulaProperty();
            nebulaProperty.setName(fieldName);
            nebulaProperty.setDataType(NebulaDataType.valueOf(typeName));
            properties.add(nebulaProperty);
        }

        return NebulaTag.builder().name(tagName).properties(properties).build();
    }

    /**
     * 将通用数据类型转换为Nebula数据类型
     * 
     * @param dataType 通用数据类型
     * @return Nebula数据类型字符串
     */
    private String convertToNebulaDataType(com.chenpp.graph.core.schema.DataType dataType) {
        if (dataType == null) {
            return "STRING";
        }

        return switch (dataType) {
            case Integer, Long -> "INT64";
            case Float, Double -> "DOUBLE";
            case Boolean -> "BOOL";
            case String -> "STRING";
            default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
        };
    }
}
