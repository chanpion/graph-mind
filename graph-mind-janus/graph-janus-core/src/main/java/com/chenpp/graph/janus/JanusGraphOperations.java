package com.chenpp.graph.janus;

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
import com.chenpp.graph.core.schema.IndexType;
import com.chenpp.graph.janus.util.CassandraClient;
import com.chenpp.graph.janus.util.JanusUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.Multiplicity;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.VertexLabel;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * JanusGraph图操作实现类
 *
 * @author April.Chen
 * @date 2025/8/13 16:00
 */
@Slf4j
public class JanusGraphOperations implements GraphOperations {
    private static final String BACKING_INDEX = "search";
    private JanusConf janusConf;
    private JanusGraph graph;

    public JanusGraphOperations(JanusGraph graph, JanusConf janusConf) {
        this.graph = graph;
        this.janusConf = janusConf;
    }

    @Override
    public void createGraph(GraphConf graphConf) throws GraphException {
        // JanusGraph中创建图空间的操作通常在配置中完成，这里可以做一些初始化工作
        try {
            if (graph == null || graph.isClosed()) {
                log.error("JanusGraph instance is not available");
                throw new GraphException("JanusGraph instance is not available");
            }
            if (janusConf.getCassandraConf() != null && janusConf.getHBaseConf() == null) {
                CassandraClient cassandraClient = new CassandraClient(janusConf.getCassandraConf());
                if (!cassandraClient.keyspaceExists(janusConf.getGraphCode())) {
                    // 创建图对应的Cassandra keyspace
                    cassandraClient.createKeyspace(janusConf.getGraphCode(),
                            "SimpleStrategy",
                            Map.of("replication_factor", 1),
                            true);
                }
            }
            log.info("JanusGraph instance is ready for graph: {}", graphConf.getGraphCode());
        } catch (Exception e) {
            log.error("Failed to initialize JanusGraph for graph: {}", graphConf.getGraphCode(), e);
            throw new GraphException("Failed to initialize JanusGraph", e);
        }
    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {
        try {
            if (graph != null && !graph.isClosed()) {
                // 注意：这个操作会关闭图数据库并删除其数据
                graph.close();
                log.info("Closed graph instance for: {}", graphConf.getGraphCode());
            }
        } catch (Exception e) {
            log.error("Failed to drop graph: {}", graphConf.getGraphCode(), e);
            throw new GraphException("Failed to drop graph: " + graphConf.getGraphCode(), e);
        }
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) throws GraphException {
        try {
            Graph graph = new Graph();
            // 检查后端中是否存在对应的 keyspace/namespace
            if (janusConf.getCassandraConf() != null) {
                CassandraClient cassandraClient = new CassandraClient(janusConf.getCassandraConf());
                if (!cassandraClient.keyspaceExists(janusConf.getGraphCode())) {
                    log.warn("Cassandra keyspace '{}' not found, graph does not exist", janusConf.getGraphCode());
                    return java.util.Collections.emptyList();
                }
                graph.setCode(janusConf.getCassandraConf().getKeyspace());
                graph.setName(graph.getCode());
            } else if (janusConf.getHBaseConf() != null) {
                // HBase 命名空间检查暂未实现，默认返回配置中的图
                log.warn("HBase namespace existence check not implemented, returning configured graph");
            }
            log.debug("Listed graphs, returning single graph: {}", graphConf.getGraphCode());
            return java.util.Collections.singletonList(graph);
        } catch (Exception e) {
            log.error("Failed to list graphs", e);
            throw new GraphException("Failed to list graphs", e);
        }
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) throws GraphException {
        JanusGraphManagement management = null;
        try {
            management = graph.openManagement();
            // 创建顶点标签
            createVertexLabels(management, graphSchema.getEntities());

            // 创建边标签
            createEdgeLabels(management, graphSchema.getRelations());

            // 创建属性键
            createPropertyKeys(management, graphSchema.getEntities(), graphSchema.getRelations());

            // 确保系统属性键 uid 存在（用于顶点/边查询）
            ensureSystemPropertyKeys(management);

            // 创建索引
            createIndices(management, graphSchema.getIndexes());
            // 确保 uid 索引存在
            ensureSystemIndices(management);

            management.commit();
            log.info("Successfully applied schema to graph: {}", graphConf.getGraphCode());
        } catch (Exception e) {
            log.error("Failed to apply schema to graph: {}", graphConf.getGraphCode(), e);
            if (management != null) {
                try {
                    management.rollback();
                } catch (Exception rollbackException) {
                    log.warn("Failed to rollback management transaction", rollbackException);
                }
            }
            throw new GraphException("Failed to apply schema", e);
        }
    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        JanusGraphManagement management = null;
        GraphSchema schema = new GraphSchema();

        try {
            management = graph.openManagement();

            // 获取顶点标签
            List<GraphEntity> entities = getVertexLabels(management);
            schema.setEntities(entities);

            // 获取边标签
            List<GraphRelation> relations = getEdgeLabels(management);
            schema.setRelations(relations);

            getPropertyKeys(management);
            // 获取索引
            List<GraphIndex> indexes = getIndices(management);
            schema.setIndexes(indexes);

            log.debug("Retrieved published schema for graph: {}, entities: {}, relations: {}, indexes: {}",
                    graphConf.getGraphCode(), entities.size(), relations.size(), indexes.size());
            return schema;
        } catch (Exception e) {
            log.error("Failed to get published schema for graph: {}", graphConf.getGraphCode(), e);
            throw new GraphException("Failed to get published schema", e);
        } finally {
            if (management != null) {
                try {
                    management.rollback(); // 只读操作，回滚以避免持有管理会话
                } catch (Exception rollbackException) {
                    log.warn("Failed to rollback management transaction", rollbackException);
                }
            }
        }
    }

    private List<GraphEntity> getVertexLabels(JanusGraphManagement management) {
        Iterable<org.janusgraph.core.VertexLabel> vertexLabels = management.getVertexLabels();
        List<GraphEntity> entities = new ArrayList<>();

        for (org.janusgraph.core.VertexLabel vertexLabel : vertexLabels) {
            if (!vertexLabel.isPartitioned() && !vertexLabel.isStatic()) {
                GraphEntity entity = new GraphEntity();
                entity.setLabel(vertexLabel.name());

                List<GraphProperty> properties = new ArrayList<>();
                for (PropertyKey pk : vertexLabel.mappedProperties()) {
                    GraphProperty prop = new GraphProperty();
                    prop.setCode(pk.name());
                    prop.setName(pk.name());
                    prop.setDataType(DataType.instanceOf(pk.dataType().getSimpleName()));
                    properties.add(prop);
                }
                entity.setProperties(properties);

                entities.add(entity);
            }
        }

        return entities;
    }

    private List<GraphRelation> getEdgeLabels(JanusGraphManagement management) {
        Iterable<EdgeLabel> edgeLabels = management.getRelationTypes(EdgeLabel.class);
        List<GraphRelation> relations = new ArrayList<>();

        for (EdgeLabel edgeLabel : edgeLabels) {
            GraphRelation relation = new GraphRelation();
            relation.setLabel(edgeLabel.name());
            // 获取边的起点和终点顶点标签（从 mappedConnections 中获取）
            Collection<org.janusgraph.core.Connection> connections = edgeLabel.mappedConnections();
            if (connections != null && !connections.isEmpty()) {
                org.janusgraph.core.Connection firstConn = connections.iterator().next();
                VertexLabel outLabel = firstConn.getOutgoingVertexLabel();
                VertexLabel inLabel = firstConn.getIncomingVertexLabel();
                if (outLabel != null) {
                    relation.setStartLabel(outLabel.name());
                }
                if (inLabel != null) {
                    relation.setEndLabel(inLabel.name());
                }
            }

            List<GraphProperty> properties = new ArrayList<>();
            for (PropertyKey pk : edgeLabel.mappedProperties()) {
                GraphProperty prop = new GraphProperty();
                prop.setCode(pk.name());
                prop.setName(pk.name());
                prop.setDataType(DataType.instanceOf(pk.dataType().getSimpleName()));
                properties.add(prop);
            }
            relation.setProperties(properties);

            relations.add(relation);
        }

        return relations;
    }

    public List<GraphProperty> getPropertyKeys(JanusGraphManagement management) {
        Iterable<PropertyKey> propertyKeys = management.getRelationTypes(PropertyKey.class);
        List<GraphProperty> properties = new ArrayList<>();
        propertyKeys.forEach(key -> {
            GraphProperty property = new GraphProperty();
            property.setCode(key.name());
            property.setName(key.name());
            property.setDataType(DataType.instanceOf(key.dataType().getSimpleName()));
            properties.add(property);
        });
        return properties;
    }

    private List<GraphIndex> getIndices(JanusGraphManagement mgmt) {
        log.debug("Retrieved indices (currently returning empty list due to API limitations)");
        Iterable<JanusGraphIndex> indexes = mgmt.getGraphIndexes(Vertex.class);
        List<GraphIndex> graphIndices = new ArrayList<>();
        indexes.forEach(index -> {
            GraphIndex graphIndex = new GraphIndex();
            graphIndex.setName(index.name());
            graphIndex.setSchemaType(GraphConstants.VERTEX);
            String type = index.isMixedIndex() ? "MIXED" : "COMPOSITE";
            graphIndex.setType(type);
            List<String> propertyNames = Arrays.stream(index.getFieldKeys()).map(PropertyKey::name).toList();
            graphIndex.setPropertyNames(propertyNames);
            graphIndices.add(graphIndex);
        });

        return graphIndices;
    }

    /**
     * 创建顶点标签
     *
     * @param management 图管理对象
     * @param entities   实体列表
     */
    private void createVertexLabels(JanusGraphManagement management, List<GraphEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            log.info("No vertex labels to create");
            return;
        }

        int createdCount = 0;
        for (GraphEntity entity : entities) {
            if (!management.containsVertexLabel(entity.getLabel())) {
                management.makeVertexLabel(entity.getLabel()).make();
                log.info("Created vertex label: {}", entity.getLabel());
                createdCount++;
            }
        }
        log.debug("Created {} vertex labels", createdCount);
    }

    /**
     * 创建边标签
     *
     * @param management 图管理对象
     * @param relations  关系列表
     */
    private void createEdgeLabels(JanusGraphManagement management, List<GraphRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            log.info("No edge labels to create");
            return;
        }

        int createdCount = 0;
        for (GraphRelation relation : relations) {
            if (!management.containsEdgeLabel(relation.getLabel())) {
                Multiplicity multiplicity = relation.getMultiple() ? Multiplicity.MULTI : Multiplicity.SIMPLE;
                management.makeEdgeLabel(relation.getLabel())
                        .multiplicity(multiplicity)
                        .make();
                log.info("Created edge label: {}", relation.getLabel());
                createdCount++;
            }
        }
        log.debug("Created {} edge labels", createdCount);
    }

    /**
     * 创建属性键
     *
     * @param management 图管理对象
     * @param entities   实体列表
     * @param relations  关系列表
     */
    private void createPropertyKeys(JanusGraphManagement management, List<GraphEntity> entities, List<GraphRelation> relations) {

        // 收集所有属性
        Set<GraphProperty> allProperties = new java.util.HashSet<>();

        if (entities != null) {
            entities.stream()
                    .filter(entity -> entity.getProperties() != null)
                    .flatMap(entity -> entity.getProperties().stream())
                    .forEach(allProperties::add);
        }

        if (relations != null) {
            relations.stream()
                    .filter(relation -> relation.getProperties() != null)
                    .flatMap(relation -> relation.getProperties().stream())
                    .forEach(allProperties::add);
        }

        if (allProperties.isEmpty()) {
            log.info("No property keys to create");
            return;
        }

        int createdCount = 0;
        // 创建属性键
        for (GraphProperty property : allProperties) {
            if (!management.containsPropertyKey(property.getCode())) {
                Class<?> clazz = JanusUtil.getJanusDataType(property.getDataType());
                if (clazz == null) {
                    log.info("property ({}) data type ({}) is not supported, skip.", property.getCode(), property.getDataType());
                    continue;
                }
                // 根据数据类型创建属性键
                PropertyKey propertyKey = management.makePropertyKey(property.getCode())
                        .dataType(clazz)
                        .cardinality(Cardinality.SINGLE)
                        .make();
                log.info("Created property key: {} with type: {}, propertyKey: {}", property.getCode(), property.getDataType(), propertyKey);
                createdCount++;
            }
        }
        log.info("Created {} property keys", createdCount);
    }

    /**
     * 创建索引
     *
     * @param management 图管理对象
     * @param indexes    索引列表
     */
    private void createIndices(JanusGraphManagement management, List<GraphIndex> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            log.info("No indices to create");
            return;
        }

        int createdCount = 0;
        for (GraphIndex index : indexes) {
            if (CollectionUtils.isEmpty(index.getPropertyNames())) {
                log.warn("index ({}) contains empty property keys, skip.", index.getName());
                continue;
            }
            // 创建图索引
            if (isGraphIndex(index.getType())) {
                createVertexIndex(management, index);
                createdCount++;
            }
            // 创建关系索引
            if (isRelationIndex(index.getType())) {
                createRelationIndex(index, management);
                createdCount++;
            }
        }
        log.debug("Created {} indices", createdCount);
    }


    private void createVertexIndex(JanusGraphManagement mgmt, GraphIndex index) {
        String name = index.getName();
        if (mgmt.containsGraphIndex(index.getName())) {
            log.warn("graph index ({}) is already exists.", index.getName());
            return;
        }

        JanusGraphManagement.IndexBuilder builder;
        if (Objects.equals(index.getSchemaType(), GraphConstants.VERTEX)) {
            builder = mgmt.buildIndex(name, Vertex.class);
        } else if (Objects.equals(index.getSchemaType(), GraphConstants.EDGE)) {
            builder = mgmt.buildIndex(name, Edge.class);
        } else {
            // never happen
            log.error("the schema type ({}) of index ({}) is not support!", index.getType(), index.getName());
            throw new IllegalArgumentException("the schema type (" + index.getType() + ") of index (" + index.getName() + ") is not support!");
        }

        addPropertyKeyForIndex(builder, index, mgmt);

        // 仅 组合索引支持 唯一配置
        if (isCompositeIndex(index.getType()) && index.isUnique()) {
            builder.unique();
        }

        if (isCompositeIndex(index.getType())) {
            builder.buildCompositeIndex();
        }

        log.info("Created vertex index: {}", name);
    }

    private static void addPropertyKeyForIndex(JanusGraphManagement.IndexBuilder builder, GraphIndex index, JanusGraphManagement mgmt) {
        for (String propName : index.getPropertyNames()) {
            PropertyKey propertyKey = mgmt.getPropertyKey(propName);
            builder.addKey(propertyKey);
        }
    }

    private static boolean isCompositeIndex(String type) {
        // 默认为 composite 索引
        return type == null || IndexType.COMPOSITE.name().equals(type);
    }

    private static boolean isGraphIndex(String type) {
        return isCompositeIndex(type) || IndexType.MIX.name().equals(type);
    }

    private static boolean isRelationIndex(String type) {
        return IndexType.VERTEX_CENTRIC.name().equals(type);
    }

    private static void createRelationIndex(GraphIndex index, JanusGraphManagement mgmt) {
        String name = index.getName();
        EdgeLabel edgeLabel = mgmt.getEdgeLabel(index.getLabel());

        if (mgmt.containsRelationIndex(edgeLabel, name)) {
            log.warn("relation index ({}) for EdgeLabel ({}) is already exists.", name, edgeLabel.name());
            return;
        }

        List<PropertyKey> propertyKeys = new ArrayList<>();
        for (String propName : index.getPropertyNames()) {
            PropertyKey propertyKey = mgmt.getPropertyKey(propName);
            propertyKeys.add(propertyKey);
        }

        mgmt.buildEdgeIndex(edgeLabel, name, Direction.BOTH, Order.desc, propertyKeys.toArray(PropertyKey[]::new));
        log.info("Created relation index: {} for edge label: {}", name, edgeLabel.name());
    }

    /**
     * 确保系统属性键存在（如uid）
     */
    private void ensureSystemPropertyKeys(JanusGraphManagement management) {
        if (!management.containsPropertyKey(GraphConstants.UID)) {
            management.makePropertyKey(GraphConstants.UID)
                    .dataType(String.class)
                    .cardinality(Cardinality.SINGLE)
                    .make();
            log.info("Created system property key: {}", GraphConstants.UID);
        }
    }

    /**
     * 确保系统索引存在（如uid索引）
     */
    private void ensureSystemIndices(JanusGraphManagement management) {
        String uidIndexName = "byUid";
        if (!management.containsGraphIndex(uidIndexName)) {
            PropertyKey uidKey = management.getPropertyKey(GraphConstants.UID);
            management.buildIndex(uidIndexName, Vertex.class)
                    .addKey(uidKey)
                    .buildCompositeIndex();
            log.info("Created system composite index: {} on property: {}", uidIndexName, GraphConstants.UID);
        }
    }
}