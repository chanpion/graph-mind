package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
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
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author April.Chen
 * @date 2025/4/9 15:23
 */
@Slf4j
public class NebulaGraphOperations implements GraphOperations {

    private NebulaConf nebulaConf;

    public NebulaGraphOperations(NebulaConf nebulaConf) {
        this.nebulaConf = nebulaConf;
    }

    @Override
    public void createGraph(GraphConf graphConf) {
        log.info("Creating graph: {}", graphConf.getGraphCode());
        String nql = NebulaUtil.buildCreateSpace(nebulaConf);
        ResultSet resultSet = execute(nebulaConf, nql);
        assertSuccess(resultSet, "create graph");
        log.info("Create graph {} success, waiting for space to be ready...", nebulaConf.getGraphCode());
        waitForSpaceReady(nebulaConf.getGraphCode());
        log.info("Space {} is ready", nebulaConf.getGraphCode());
    }

    private void waitForSpaceReady(String spaceName) {
        int maxRetries = 30;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String useSpace = NebulaUtil.buildUseSpace(spaceName);
                NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
                try (Session session = nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
                    ResultSet rs = session.execute(useSpace);
                    if (rs.isSucceeded()) {
                        return;
                    }
                }
            } catch (Exception e) {
                log.debug("Space not ready yet, retry {}/{}: {}", i + 1, maxRetries, e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GraphException("Interrupted while waiting for space to be ready");
            }
        }
        throw new GraphException("Space " + spaceName + " not ready after " + maxRetries + " retries");
    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {
        log.info("Dropping graph: {}", graphConf.getGraphCode());
        String nql = NebulaUtil.buildDropSpace(this.nebulaConf.getGraphCode());
        ResultSet resultSet = execute(this.nebulaConf, nql);
        assertSuccess(resultSet, "drop graph");
        log.info("Drop graph {} success", this.nebulaConf.getGraphCode());
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) {
        log.info("Listing graphs");
        String nql = NebulaUtil.buildShowSpaces();
        ResultSet resultSet = execute(nebulaConf, nql);
        assertSuccess(resultSet, "list graphs");
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

        List<Graph> graphs = listGraphs(graphConf);
        if (graphs.stream().noneMatch(graph -> Objects.equals(graph.getCode(), nebulaConf.getGraphCode()))) {
            createGraph(graphConf);
        }

        try {
            withSession(session -> {
                useSpace(session, nebulaConf.getGraphCode());
                createTags(graphSchema.getEntities(), session);
                createEdges(graphSchema.getRelations(), session);
                checkSuccessOrWarn(session.execute(NebulaUtil.buildUseSpace(nebulaConf.getGraphCode())),
                        "re-use space after creating tags/edges");
                createIndices(graphSchema.getIndexes(), session);
                log.info("Successfully applied schema for graph: {}", graphConf.getGraphCode());
            });
        } catch (Exception e) {
            log.error("Nebula create schema error", e);
            throw new GraphException("nebula create schema error", e);
        }
    }

    @Override
    public void alterSchema(GraphConf graphConf, GraphSchema alterSchema) {
        if (CollectionUtils.isEmpty(alterSchema.getEntities()) && CollectionUtils.isEmpty(alterSchema.getRelations())) {
            log.info("No entities or relations to alter");
            return;
        }

        log.info("Begin alter graph schema for: {}", graphConf.getGraphCode());

        try {
            withSession(session -> {
                useSpace(session, nebulaConf.getGraphCode());

                GraphSchema publishedSchema = new GraphSchema();
                publishedSchema.setEntities(showTags(session));
                publishedSchema.setRelations(showEdges(session));

                alterTags(alterSchema.getEntities(), session, publishedSchema);
                alterEdges(alterSchema.getRelations(), session, publishedSchema);

                log.info("Successfully altered schema for graph: {}", graphConf.getGraphCode());
            });
        } catch (Exception e) {
            log.error("Nebula alter schema error", e);
            throw new GraphException("nebula alter schema error", e);
        }
    }

    /**
     * 增量更新标签：为已存在的标签添加新属性
     */
    private void alterTags(List<GraphEntity> entities, Session session, GraphSchema publishedSchema) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        Map<String, Set<String>> remoteProps = publishedSchema.getEntities() == null ? Map.of() :
                publishedSchema.getEntities().stream()
                        .filter(e -> e.getLabel() != null)
                        .collect(Collectors.toMap(
                                GraphEntity::getLabel,
                                e -> e.getProperties() == null ? Set.of() :
                                        e.getProperties().stream()
                                                .map(GraphProperty::getCode)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toSet())
                        ));

        for (GraphEntity entity : entities) {
            if (entity.getProperties() == null || entity.getProperties().isEmpty()) {
                continue;
            }
            Set<String> existingProps = remoteProps.getOrDefault(entity.getLabel(), Set.of());
            // 找出本地有新但远程没有的属性
            List<GraphProperty> newProps = entity.getProperties().stream()
                    .filter(p -> p.getCode() != null && !existingProps.contains(p.getCode()))
                    .toList();

            if (newProps.isEmpty()) {
                continue;
            }

            // 构建只含新属性的GraphEntity用于生成ALTER语句
            GraphEntity alterEntity = new GraphEntity();
            alterEntity.setLabel(entity.getLabel());
            alterEntity.setProperties(newProps);

            try {
                String nql = NebulaUtil.buildAlterTagAdd(alterEntity);
                log.info("Altering tag {} with new properties: {}", entity.getLabel(), newProps.stream().map(GraphProperty::getCode).collect(Collectors.joining(", ")));
                ResultSet resultSet = session.execute(nql);
                throwIfFailed(resultSet, "alter tag " + entity.getLabel());
                log.info("Successfully altered tag: {}", entity.getLabel());
            } catch (Exception e) {
                log.error("Error altering tag: " + entity.getLabel(), e);
                throw new GraphException("Error altering tag: " + entity.getLabel(), e);
            }
        }
    }

    /**
     * 增量更新边类型：为已存在的边类型添加新属性
     */
    private void alterEdges(List<GraphRelation> relations, Session session, GraphSchema publishedSchema) {
        if (relations == null || relations.isEmpty()) {
            return;
        }

        Map<String, Set<String>> remoteProps = publishedSchema.getRelations() == null ? Map.of() :
                publishedSchema.getRelations().stream()
                        .filter(r -> r.getLabel() != null)
                        .collect(Collectors.toMap(
                                GraphRelation::getLabel,
                                r -> r.getProperties() == null ? Set.of() :
                                        r.getProperties().stream()
                                                .map(GraphProperty::getCode)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toSet())
                        ));

        for (GraphRelation relation : relations) {
            if (CollectionUtils.isEmpty(relation.getProperties())) {
                continue;
            }
            Set<String> existingProps = remoteProps.getOrDefault(relation.getLabel(), Set.of());
            List<GraphProperty> newProps = relation.getProperties().stream()
                    .filter(p -> p.getCode() != null && !existingProps.contains(p.getCode()))
                    .toList();

            if (newProps.isEmpty()) {
                continue;
            }

            GraphRelation alterRelation = new GraphRelation();
            alterRelation.setLabel(relation.getLabel());
            alterRelation.setProperties(newProps);

            try {
                String nql = NebulaUtil.buildAlterEdgeAdd(alterRelation);
                log.info("Altering edge {} with new properties: {}", relation.getLabel(), newProps.stream().map(GraphProperty::getCode).collect(Collectors.joining(", ")));
                ResultSet resultSet = session.execute(nql);
                throwIfFailed(resultSet, "alter edge " + relation.getLabel());
                log.info("Successfully altered edge: {}", relation.getLabel());
            } catch (Exception e) {
                log.error("Error altering edge: " + relation.getLabel(), e);
                throw new GraphException("Error altering edge: " + relation.getLabel(), e);
            }
        }
    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        String graphCode = graphConf.getGraphCode();
        log.info("Getting published schema for: {}", graphCode);
        GraphSchema schema = new GraphSchema();

        try {
            withSession(session -> {
                useSpace(session, graphCode);
                List<GraphEntity> entities = showTags(session);
                schema.setEntities(entities);
                List<GraphRelation> relations = showEdges(session);
                schema.setRelations(relations);
                List<GraphIndex> indexes = showIndexes(session);
                schema.setIndexes(indexes);
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to get published schema from nebula", e);
            throw new GraphException("nebula get published schema error", e);
        }

        return schema;
    }


    private ResultSet execute(NebulaConf nebulaConf, String nql) {
        log.info("Execute ngql: {}", nql);
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        if (nebulaPool == null) {
            throw new GraphException("Nebula pool not initialized for " + nebulaConf.getHosts());
        }
        try (Session session = nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
            return session.execute(nql);
        } catch (Exception e) {
            log.error("Nebula execute error for query: {}", nql, e);
            throw new GraphException("Nebula execute error", e);
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
                String nql = NebulaUtil.buildCreateTag(entity);
                ResultSet resultSet = session.execute(nql);
                throwIfFailed(resultSet, "create tag " + entity.getLabel());
                log.info("Successfully created tag: {}", entity.getLabel());
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
                String nql = NebulaUtil.buildCreateEdge(edge);
                ResultSet resultSet = session.execute(nql);
                throwIfFailed(resultSet, "create edge " + edge.getLabel());
                log.info("Successfully created edge: {}", edge.getLabel());
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

        assertSuccess(rs, "show tags");

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

            GraphEntity entity = new GraphEntity();
            entity.setLabel(tagName);
            entities.add(entity);


            try {
                NebulaTag nebulaTag = describeTag(tagName, session);
                List<GraphProperty> properties = nebulaTag.getProperties().stream()
                    .map(NebulaUtil::toGraphProperty)
                    .toList();
                entity.setProperties(properties);
            } catch (Exception e) {
                log.warn("Failed to describe tag: {}", tagName, e);
            }
        }

        return entities;
    }

    public List<GraphRelation> showEdges(Session session) throws GraphException {
        String nql = NebulaUtil.buildShowEdges();

        ResultSet rs;
        try {
            rs = session.execute(nql);
        } catch (IOErrorException e) {
            throw new GraphException("Failed to show edges", e);
        }

        assertSuccess(rs, "show edges");

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

            GraphRelation relation = new GraphRelation();
            relation.setLabel(edgeName);
            relations.add(relation);

            try {
                NebulaEdge nebulaEdge = describeEdge(edgeName, session);
                List<GraphProperty> properties = nebulaEdge.getProperties().stream()
                    .map(NebulaUtil::toGraphProperty)
                    .toList();
                relation.setProperties(properties);
            } catch (Exception e) {
                log.warn("Failed to describe edge: {}", edgeName, e);
            }
        }

        return relations;
    }

    public void createIndices(List<GraphIndex> indices, Session session) {
        if (indices == null || indices.isEmpty()) {
            log.info("No indices to create");
            return;
        }

        log.info("Creating {} indices", indices.size());
        indices.forEach(i -> log.info("Creating index: name={}, label={}, schemaType={}, property={}, propertyNames={}", i.getName(), i.getLabel(), i.getSchemaType(), i.getProperty(), i.getPropertyNames()));
        indices.forEach(index -> {
            try {
                // 构建属性类型映射，用于创建索引时判断是否需要添加长度
                Map<String, String> propTypeMap = index.getProperties().stream().collect(Collectors.toMap(
                        GraphProperty::getCode, p-> NebulaUtil.convertToNebulaDataType(p.getDataType())

                ));

                NebulaIndex.NebulaIndexBuilder indexBuilder = NebulaIndex.builder()
                        .indexName(index.getName())
                        .typeName(index.getLabel())
                        .propNameList(index.getPropertyNames())
                        .propTypeMap(propTypeMap);

                if (GraphConstants.VERTEX.equalsIgnoreCase(index.getSchemaType())) {
                    indexBuilder.indexType(SchemaType.TAG);
                } else if (GraphConstants.EDGE.equalsIgnoreCase(index.getSchemaType())) {
                    indexBuilder.indexType(SchemaType.EDGE);
                }

                NebulaIndex nebulaIndex = indexBuilder.build();

                String nql = NebulaUtil.buildCreateIndex(nebulaIndex);
                log.info("Execute create index NGQL: {}", nql);
                log.info("Index details: indexType={}, indexName={}, typeName={}, propNameList={}, propTypeMap={}",
                        nebulaIndex.getIndexType(), nebulaIndex.getIndexName(),
                        nebulaIndex.getTypeName(), nebulaIndex.getPropNameList(), nebulaIndex.getPropTypeMap());

                ResultSet resultSet = session.execute(nql);
                if (!resultSet.isSucceeded()) {
                    log.error("Failed to create index: indexName={}, nql={}", index.getName(), nql);
                    throwIfFailed(resultSet, "create index " + index.getName());
                }
                log.info("Successfully created index: {}", index.getName());

            } catch (Exception e) {
                log.error("Error creating index: " + index.getName(), e);
                throw new GraphException("Error creating index: " + index.getName(), e);
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
        String nql = NebulaUtil.buildShowIndexes(SchemaType.valueOf(indexType));

        ResultSet rs;
        try {
            rs = session.execute(nql);
        } catch (IOErrorException e) {
            throw new GraphException("Failed to show tag indexes", e);
        }

        assertSuccess(rs, "show indexes");

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

            GraphIndex index = new GraphIndex();
            index.setName(indexName);
            indexes.add(index);
        }

        return indexes;
    }


    public NebulaTag describeTag(String tagName, Session session) throws GraphException {
        String nql = NebulaUtil.buildDescribeTag(tagName);
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
                nebulaProperty.setDataType(NebulaDataType.getDataType(typeName));
                properties.add(nebulaProperty);
            }

            return NebulaTag.builder().name(tagName).properties(properties).build();
        } catch (IOException | IOErrorException e) {
            throw new GraphException("Desc tag error", e);
        }
    }

    public NebulaEdge describeEdge(String edgeName, Session session) throws GraphException {
        String nql = NebulaUtil.buildDescribeEdge(edgeName);
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
                nebulaProperty.setDataType(NebulaDataType.getDataType(typeName));
                properties.add(nebulaProperty);
            }

            return NebulaEdge.builder().name(edgeName).properties(properties).build();
        } catch (IOException | IOErrorException e) {
            throw new GraphException("Desc edge error", e);
        }
    }

    private void assertSuccess(ResultSet rs, String operation) {
        if (!rs.isSucceeded()) {
            String msg = String.format("%s failed, errorCode: %s, errorMessage: %s",
                    operation, rs.getErrorCode(), rs.getErrorMessage());
            log.error(msg);
            throw new GraphException(msg);
        }
    }

    private void throwIfFailed(ResultSet rs, String operation) {
        if (!rs.isSucceeded()) {
            throw new GraphException(String.format("%s failed, errorCode: %s, errorMessage: %s",
                    operation, rs.getErrorCode(), rs.getErrorMessage()));
        }
    }

    private boolean checkSuccessOrWarn(ResultSet rs, String operation) {
        if (!rs.isSucceeded()) {
            log.warn("{} failed, errorCode: {}, errorMessage: {}", operation,
                    rs.getErrorCode(), rs.getErrorMessage());
            return false;
        }
        return true;
    }

    @FunctionalInterface
    private interface SessionFunction<T> {
        T apply(Session session) throws Exception;
    }

    @FunctionalInterface
    private interface SessionConsumer {
        void accept(Session session) throws Exception;
    }

    private <T> T withSession(SessionFunction<T> fn) throws Exception {
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        if (nebulaPool == null) {
            throw new GraphException("Nebula pool not initialized for " + nebulaConf.getHosts());
        }
        try (Session session = nebulaPool.getSession(
                nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
            return fn.apply(session);
        }
    }

    private void withSession(SessionConsumer fn) throws Exception {
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        if (nebulaPool == null) {
            throw new GraphException("Nebula pool not initialized for " + nebulaConf.getHosts());
        }
        try (Session session = nebulaPool.getSession(
                nebulaConf.getUsername(), nebulaConf.getPassword(), false)) {
            fn.accept(session);
        }
    }

    private void useSpace(Session session, String spaceName) throws IOErrorException {
        ResultSet rs = session.execute(NebulaUtil.buildUseSpace(spaceName));
        assertSuccess(rs, "use space " + spaceName);
    }

    @Override
    public void dropVertexLabel(String graphCode, String label) {
        log.info("Dropping vertex label: {}", label);
        try {
            withSession(session -> {
                useSpace(session, graphCode);
                // 先删除与该标签相关的所有索引
                dropIndexesForLabel(session, label, SchemaType.TAG);
                // 然后删除标签
                String ngql = "DROP TAG IF EXISTS `" + label + "`";
                log.info("Execute drop tag NGQL: {}", ngql);
                ResultSet rs = session.execute(ngql);
                assertSuccess(rs, "drop tag " + label);
            });
        } catch (Exception e) {
            log.error("Failed to drop vertex label: {}", label, e);
            throw new GraphException("Failed to drop vertex label: " + label, e);
        }
    }

    @Override
    public void dropEdgeLabel(String graphCode, String label) {
        log.info("Dropping edge label: {}", label);
        try {
            withSession(session -> {
                useSpace(session, graphCode);
                // 先删除与该边类型相关的所有索引
                dropIndexesForLabel(session, label, SchemaType.EDGE);
                // 然后删除边类型
                String ngql = "DROP EDGE IF EXISTS `" + label + "`";
                log.info("Execute drop edge NGQL: {}", ngql);
                ResultSet rs = session.execute(ngql);
                assertSuccess(rs, "drop edge " + label);
            });
        } catch (Exception e) {
            log.error("Failed to drop edge label: {}", label, e);
            throw new GraphException("Failed to drop edge label: " + label, e);
        }
    }

    /**
     * 删除与指定标签相关的所有索引
     *
     * @param session   会话对象
     * @param labelName 标签名称
     * @param schemaType  schema类型 (TAG 或 EDGE)
     */
    private void dropIndexesForLabel(Session session, String labelName, SchemaType schemaType) {
        log.info("Dropping indexes for label: {}, type: {}", labelName, schemaType);
        String nql = "SHOW " + schemaType + " INDEXES";
        
        try {
            ResultSet rs = session.execute(nql);
            if (!rs.isSucceeded()) {
                log.warn("Failed to show indexes for label: {}, error: {}", labelName, rs.getErrorMessage());
                return;
            }

            for (int i = 0; i < rs.rowsSize(); i++) {
                ResultSet.Record record = rs.rowValues(i);
                try {
                    // 获取索引名称（第1列）
                    String indexName = record.get(0).asString();
                    // 获取标签名称（第2列）
                    String indexedLabel = record.get(1).asString();
                    
                    // 检查索引是否与要删除的标签相关
                    if (labelName.equalsIgnoreCase(indexedLabel)) {
                        String dropNql = "DROP " + schemaType + " INDEX IF EXISTS `" + indexName + "`";
                        log.info("Execute drop index NGQL: {}", dropNql);
                        ResultSet dropRs = session.execute(dropNql);
                        if (dropRs.isSucceeded()) {
                            log.info("Successfully dropped index: {}", indexName);
                        } else {
                            log.warn("Failed to drop index: {}, error: {}", indexName, dropRs.getErrorMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse index record, skipping: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to drop indexes for label: {}", labelName, e);
        }
    }
}