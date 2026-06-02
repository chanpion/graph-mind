package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.nebula.ngql.NGQLBuilder;
import com.chenpp.graph.nebula.schema.NebulaDataType;
import com.chenpp.graph.nebula.schema.NebulaEdge;
import com.chenpp.graph.nebula.schema.NebulaIndex;
import com.chenpp.graph.nebula.schema.NebulaProperty;
import com.chenpp.graph.nebula.schema.NebulaTag;
import com.chenpp.graph.nebula.schema.SchemaType;
import com.chenpp.graph.nebula.util.NebulaUtil;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author April.Chen
 * @date 2025/4/9 15:23
 */
@Slf4j
public class NebulaGraphOperations implements GraphOperations {

    private NGQLBuilder ngqlBuilder = new NGQLBuilder(ZoneOffset.of("+8"));
    private NebulaConf nebulaConf;

    public NebulaGraphOperations(NebulaConf nebulaConf) {
        this.nebulaConf = nebulaConf;
    }

    @Override
    public void createGraph(GraphConf graphConf) {
        log.info("Creating graph: {}", graphConf.getGraphCode());
        String nql = NebulaUtil.buildCreateSpace(nebulaConf);
        ResultSet resultSet = execute(nebulaConf, nql);
        if (!resultSet.isSucceeded()) {
            log.error("Create graph failed, errorCode: {}, errorMessage: {}",
                    resultSet.getErrorCode(), resultSet.getErrorMessage());
            throw new GraphException("create graph failed, errorCode: " + resultSet.getErrorCode() + ", errorMessage: " + resultSet.getErrorMessage());
        }
        log.info("Create graph {} success", nebulaConf.getGraphCode());
    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {
        log.info("Dropping graph: {}", graphConf.getGraphCode());
        String nql = NebulaUtil.buildDropSpace(this.nebulaConf.getGraphCode());
        ResultSet resultSet = execute(this.nebulaConf, nql);
        if (!resultSet.isSucceeded()) {
            log.error("Drop graph failed, errorCode: {}, errorMessage: {}",
                    resultSet.getErrorCode(), resultSet.getErrorMessage());
            throw new GraphException("drop graph failed, errorCode: " + resultSet.getErrorCode() + ", errorMessage: " + resultSet.getErrorMessage());
        }
        log.info("Drop graph {} success", this.nebulaConf.getGraphCode());
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) {
        log.info("Listing graphs");
        String nql = "SHOW SPACES";
        ResultSet resultSet = execute(nebulaConf, nql);
        if (!resultSet.isSucceeded()) {
            log.error("List graph failed, errorCode: {}, errorMessage: {}",
                    resultSet.getErrorCode(), resultSet.getErrorMessage());
            throw new GraphException("list graph failed, errorCode: " + resultSet.getErrorCode() + ", errorMessage: " + resultSet.getErrorMessage());
        }
        return resultSet.getRows().stream().map(row -> {
            String space = new String(row.getValues().get(0).getSVal(), StandardCharsets.UTF_8);
            Graph graph = new Graph();
            graph.setCode(space);
            graph.setName(space);
            return graph;
        }).collect(Collectors.toList());
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) {
        log.info("Begin apply graph schema for: {}", graphConf.getGraphCode());
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);

        List<Graph> graphs = listGraphs(graphConf);
        if (graphs.stream().noneMatch(graph -> Objects.equals(graph.getCode(), nebulaConf.getGraphCode()))) {
            createGraph(graphConf);
        }

        String useSpace = ngqlBuilder.buildUseSpace(nebulaConf.getSpace());

        try (Session session = nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
            ResultSet rs = session.execute(useSpace);
            if (!rs.isSucceeded()) {
                log.error("Failed to use space, error code: {}, error message: {}", rs.getErrorCode(), rs.getErrorMessage());
                throw new GraphException(String.format("Failed to use space, error code: %s ,error message %s", rs.getErrorCode(), rs.getErrorMessage()));
            }
            // 创建标签
            createTags(graphSchema.getEntities(), session);
            // 创建边
            createEdges(graphSchema.getRelations(), session);
            // 创建索引
            createIndices(graphSchema.getIndexes(), session);
            log.info("Successfully applied schema for graph: {}", graphConf.getGraphCode());
        } catch (Exception e) {
            log.error("Nebula create schema error", e);
            throw new GraphException("nebula create schema error", e);
        }

        ResultSet rs = execute(nebulaConf, useSpace);
        if (!rs.isSucceeded()) {
            log.warn("Failed to re-use space after schema application");
        }
    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        log.info("Getting published schema for: {}", graphConf.getGraphCode());
        GraphSchema schema = new GraphSchema();

        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(this.nebulaConf);

        String useSpace = ngqlBuilder.buildUseSpace(this.nebulaConf.getSpace());

        try (Session session = nebulaPool.getSession(this.nebulaConf.getUsername(), this.nebulaConf.getPassword(), false)) {
            ResultSet rs = session.execute(useSpace);
            if (!rs.isSucceeded()) {
                log.error("Failed to use space, error code: {}, error message: {}", rs.getErrorCode(), rs.getErrorMessage());
                throw new GraphException(String.format("Failed to use space, error code: %s ,error message %s", rs.getErrorCode(), rs.getErrorMessage()));
            }

            // 获取标签信息
            List<GraphEntity> entities = showTags(session);
            schema.setEntities(entities);

            // 获取边类型信息
            List<GraphRelation> relations = showEdges(session);
            schema.setRelations(relations);

            // 获取索引信息
            List<GraphIndex> indexes = showIndexes(session);
            schema.setIndexes(indexes);
            log.info("Retrieved schema: {} entities, {} relations, {} indexes",
                    entities.size(), relations.size(), indexes.size());
        } catch (Exception e) {
            log.error("Failed to get published schema from nebula", e);
            throw new GraphException("nebula get published schema error", e);
        }

        return schema;
    }


    private ResultSet execute(NebulaConf nebulaConf, String nql) {
        log.debug("Execute ngql: {}", nql);
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        try (Session session = nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
            return session.execute(nql);
        } catch (Exception e) {
            log.error("Nebula execute error for query: {}", nql, e);
            throw new GraphException(e);
        }
    }


    public void createTags(List<GraphEntity> entities, Session session) {
        if (entities == null || entities.isEmpty()) {
            log.info("No tags to create");
            return;
        }

        log.info("Creating {} tags", entities.size());
        entities.forEach(entity -> {
            try {
                // 执行创建Tag的语句
                String nql = NebulaUtil.buildCreateTag(entity);
                ResultSet resultSet = session.execute(nql);
                if (!resultSet.isSucceeded()) {
                    throw new GraphException(String.format("Failed to create tag: %s, errorCode: %s, errorMessage: %s",
                            entity.getLabel(), resultSet.getErrorCode(), resultSet.getErrorMessage()));
                } else {
                    log.info("Successfully created tag: {}", entity.getLabel());
                }
            } catch (Exception e) {
                log.error("Error creating tag: " + entity.getLabel(), e);
                throw new GraphException(e);
            }
        });
    }

    public void createEdges(List<GraphRelation> edges, Session session) {
        if (edges == null || edges.isEmpty()) {
            log.info("No edges to create");
            return;
        }

        log.info("Creating {} edges", edges.size());
        edges.forEach(edge -> {
            try {
                // 执行创建Edge的语句
                String nql = NebulaUtil.buildCreateEdge(edge);
                ResultSet resultSet = session.execute(nql);
                if (!resultSet.isSucceeded()) {
                    throw new GraphException(String.format("Failed to create edge: %s, errorCode: %s, errorMessage: %s",
                            edge.getLabel(), resultSet.getErrorCode(), resultSet.getErrorMessage()));
                } else {
                    log.info("Successfully created edge: {}", edge.getLabel());
                }
            } catch (Exception e) {
                log.error("Error creating edge: " + edge.getLabel(), e);
                throw new GraphException(e);
            }
        });
    }

    public List<GraphEntity> showTags(Session session) throws GraphException {
        String nql = NebulaUtil.buildShowTags();

        ResultSet rs;
        try {
            rs = session.execute(nql);
        } catch (IOErrorException e) {
            throw new GraphException("Failed to show tags", e);
        }

        if (!rs.isSucceeded()) {
            log.error("Failed to get tags, errorCode: {}, errorMessage: {}", rs.getErrorCode(), rs.getErrorMessage());
            throw new GraphException("Failed to get tags, errorCode: " + rs.getErrorCode() + ", errorMessage: " + rs.getErrorMessage());
        }

        List<GraphEntity> entities = new ArrayList<>();
        for (int i = 0; i < rs.rowsSize(); i++) {
            ResultSet.Record record = rs.rowValues(i);
            ValueWrapper tagValue = record.get(0);
            String tagName;
            try {
                tagName = tagValue.asString();
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to parse tag name", e);
                throw new GraphException("Failed to parse tag name", e);
            }

            // 获取标签的详细信息
            GraphEntity entity = new GraphEntity();
            entity.setLabel(tagName);
            entities.add(entity);


            NebulaTag nebulaTag = describeTag(tagName, session);
            List<GraphProperty> properties = nebulaTag.getProperties().stream().map(p -> {
                GraphProperty property = new GraphProperty();
                property.setCode(p.getName());
                property.setName(p.getName());
                property.setDataType(NebulaUtil.convertToDataType(p.getDataType()));
                return property;
            }).toList();
            entity.setProperties(properties);
        }

        return entities;
    }

    public List<GraphRelation> showEdges(Session session) throws GraphException {
        String nql = "SHOW EDGES";

        ResultSet rs;
        try {
            rs = session.execute(nql);
        } catch (IOErrorException e) {
            throw new GraphException("Failed to show edges", e);
        }

        if (!rs.isSucceeded()) {
            log.error("Failed to get edges, errorCode: {}, errorMessage: {}",
                    rs.getErrorCode(), rs.getErrorMessage());
            throw new GraphException("Failed to get edges, errorCode: " + rs.getErrorCode() + ", errorMessage: " + rs.getErrorMessage());
        }

        List<GraphRelation> relations = new ArrayList<>();
        for (int i = 0; i < rs.rowsSize(); i++) {
            ResultSet.Record record = rs.rowValues(i);
            ValueWrapper edgeValue = record.get(0);
            String edgeName;
            try {
                edgeName = edgeValue.asString();
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to parse edge name", e);
                throw new GraphException("Failed to parse edge name", e);
            }

            // 获取边类型的详细信息
            GraphRelation relation = new GraphRelation();
            relation.setLabel(edgeName);
            relations.add(relation);

            NebulaEdge nebulaEdge = describeEdge(edgeName, session);
            List<GraphProperty> properties = nebulaEdge.getProperties().stream().map(p -> {
                GraphProperty property = new GraphProperty();
                property.setCode(p.getName());
                property.setName(p.getName());
                property.setDataType(NebulaUtil.convertToDataType(p.getDataType()));
                return property;
            }).toList();
            relation.setProperties(properties);
        }

        return relations;
    }

    public void createIndices(List<GraphIndex> indices, Session session) {
        if (indices == null || indices.isEmpty()) {
            log.info("No indices to create");
            return;
        }

        log.info("Creating {} indices", indices.size());
        indices.forEach(index -> {
            try {
                // 构建NebulaIndex对象
                NebulaIndex.NebulaIndexBuilder nebulaIndexBuilder = NebulaIndex.builder();

                // 设置索引名称
                nebulaIndexBuilder.indexName(index.getName());

                // 设置索引目标类型名
                nebulaIndexBuilder.typeName(index.getLabel());

                // 设置属性列表
                nebulaIndexBuilder.propNameList(index.getPropertyNames());

                // 根据schemaType确定索引类型
                if (GraphConstants.VERTEX.equalsIgnoreCase(index.getSchemaType())) {
                    nebulaIndexBuilder.indexType(SchemaType.TAG);
                } else if (GraphConstants.EDGE.equalsIgnoreCase(index.getSchemaType())) {
                    nebulaIndexBuilder.indexType(SchemaType.EDGE);
                } else {
                    // 默认使用TAG类型
                    nebulaIndexBuilder.indexType(SchemaType.TAG);
                    log.warn("Unknown schema type: {}, using TAG as default", index.getSchemaType());
                }

                NebulaIndex nebulaIndex = nebulaIndexBuilder.build();

                // 使用NGQLBuilder构建创建索引的NGQL语句
                String nql = ngqlBuilder.buildCreateIndex(nebulaIndex);
                log.debug("Execute create index NGQL: {}", nql);

                // 执行创建索引的语句
                ResultSet resultSet = session.execute(nql);
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

    public List<GraphIndex> showIndexes(Session session) throws GraphException {
        List<GraphIndex> tagIndex = showIndexes(session, GraphConstants.VERTEX);
        List<GraphIndex> edgeIndex = showIndexes(session, GraphConstants.EDGE);
        return Stream.concat(tagIndex.stream(), edgeIndex.stream()).toList();
    }

    public List<GraphIndex> showIndexes(Session session, String type) throws GraphException {
        String indexType = type.equalsIgnoreCase(GraphConstants.VERTEX) ? "TAG" : "EDGE";
        String nql = String.format("SHOW %s INDEXES", indexType);

        ResultSet rs;
        try {
            rs = session.execute(nql);
        } catch (IOErrorException e) {
            throw new GraphException("Failed to show tag indexes", e);
        }

        if (!rs.isSucceeded()) {
            log.error("Failed to get indexes, errorCode: {}, errorMessage: {}",
                    rs.getErrorCode(), rs.getErrorMessage());
            throw new GraphException("Failed to get indexes, errorCode: " + rs.getErrorCode() + ", errorMessage: " + rs.getErrorMessage());
        }

        List<GraphIndex> indexes = new ArrayList<>();
        for (int i = 0; i < rs.rowsSize(); i++) {
            ResultSet.Record record = rs.rowValues(i);
            ValueWrapper indexValue = record.get(0);
            String indexName;
            try {
                indexName = indexValue.asString();
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to parse index name", e);
                throw new GraphException("Failed to parse index name", e);
            }

            // 获取索引的详细信息
            GraphIndex index = new GraphIndex();
            index.setName(indexName);
            indexes.add(index);
        }

        return indexes;
    }


    public NebulaTag describeTag(String tagName, Session session) throws GraphException {
        String nql = ngqlBuilder.buildDescribeTag(tagName);
        ResultSet rs;
        try {
            rs = session.execute(nql);
            List<NebulaProperty> properties = new ArrayList<>();

            for (int i = 0; i < rs.rowsSize(); i++) {
                ResultSet.Record record = rs.rowValues(i);
                ValueWrapper field = record.get(0);
                ValueWrapper type = record.get(1);
                String fieldName = field.asString();
                String typeName = type.asString();

                NebulaProperty nebulaProperty = new NebulaProperty();
                nebulaProperty.setName(fieldName);
                // 使用不区分大小写的方式查找枚举值
                nebulaProperty.setDataType(NebulaDataType.getDataType(typeName));
                properties.add(nebulaProperty);
            }

            return NebulaTag.builder().name(tagName).properties(properties).build();
        } catch (IOException | IOErrorException e) {
            throw new GraphException("Desc tag error", e);
        }
    }

    public NebulaEdge describeEdge(String edgeName, Session session) throws GraphException {
        String nql = ngqlBuilder.buildDescribeEdge(edgeName);
        ResultSet rs;
        try {
            rs = session.execute(nql);
            List<NebulaProperty> properties = new ArrayList<>();

            for (int i = 0; i < rs.rowsSize(); i++) {
                ResultSet.Record record = rs.rowValues(i);
                ValueWrapper field = record.get(0);
                ValueWrapper type = record.get(1);
                String fieldName = field.asString();
                String typeName = type.asString();

                NebulaProperty nebulaProperty = new NebulaProperty();
                nebulaProperty.setName(fieldName);
                // 使用不区分大小写的方式查找枚举值
                nebulaProperty.setDataType(NebulaDataType.getDataType(typeName));
                properties.add(nebulaProperty);
            }

            return NebulaEdge.builder().name(edgeName).properties(properties).build();
        } catch (IOException | IOErrorException e) {
            throw new GraphException("Desc edge error", e);
        }
    }
}