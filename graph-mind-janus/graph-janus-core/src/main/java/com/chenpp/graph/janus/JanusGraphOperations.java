package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
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
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.Mapping;
import org.janusgraph.core.schema.Parameter;
import org.janusgraph.graphdb.types.ParameterType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
                throw new GraphException("JanusGraph instance is not available");
            }
            CassandraClient cassandraClient = new CassandraClient(janusConf.getCassandraConf());
            if (!cassandraClient.keyspaceExists(janusConf.getGraphCode())) {
                cassandraClient.createKeyspace(janusConf.getGraphCode(),
                        "SimpleStrategy",
                        Map.of("replication_factor", 1),
                        true);
            }
            log.info("JanusGraph instance is ready for graph: {}", graphConf.getGraphCode());
        } catch (Exception e) {
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
            throw new GraphException("Failed to drop graph: " + graphConf.getGraphCode(), e);
        }
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) throws GraphException {
        // JanusGraph没有像其他图数据库那样的"图空间"概念，这里返回一个默认的图实例
        try {
            Graph graph = new Graph();
            graph.setCode(graphConf.getGraphCode());
            return java.util.Collections.singletonList(graph);
        } catch (Exception e) {
            throw new GraphException("Failed to list graphs", e);
        }
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) throws GraphException {
        try {
            JanusGraphManagement management = graph.openManagement();
            try {
                // 创建顶点标签
                createVertexLabels(management, graphSchema.getEntities());

                // 创建边标签
                createEdgeLabels(management, graphSchema.getRelations());

                // 创建属性键
                createPropertyKeys(management, graphSchema.getEntities(), graphSchema.getRelations());

                // 创建索引
                createIndices(management, graphSchema.getIndexes());

                management.commit();
                log.info("Successfully applied schema to graph: {}", graphConf.getGraphCode());
            } catch (Exception e) {
                management.rollback();
                throw new GraphException("Failed to apply schema", e);
            }
        } catch (Exception e) {
            throw new GraphException("Failed to open management transaction", e);
        }
    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        try {
            JanusGraphManagement management = graph.openManagement();
            GraphSchema schema = new GraphSchema();

            try {
                // 获取顶点标签
                List<GraphEntity> entities = getVertexLabels(management);
                schema.setEntities(entities);

                // 获取边标签
                List<GraphRelation> relations = getEdgeLabels(management);
                schema.setRelations(relations);

                // 获取索引
                List<GraphIndex> indexes = getIndices(management);
                schema.setIndexes(indexes);

                return schema;
            } finally {
                management.rollback(); // 只读操作，回滚以避免持有管理会话
            }
        } catch (Exception e) {
            throw new GraphException("Failed to get published schema", e);
        }
    }

    private List<GraphEntity> getVertexLabels(JanusGraphManagement management) {
        Iterable<org.janusgraph.core.VertexLabel> vertexLabels = management.getVertexLabels();
        List<GraphEntity> entities = new ArrayList<>();

        for (org.janusgraph.core.VertexLabel vertexLabel : vertexLabels) {
            if (!vertexLabel.isPartitioned() && !vertexLabel.isStatic()) {
                GraphEntity entity = new GraphEntity();
                entity.setLabel(vertexLabel.name());
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
            relations.add(relation);
        }

        return relations;
    }

    private List<GraphIndex> getIndices(JanusGraphManagement management) {
        // 注意：由于JanusGraph API限制，我们无法直接获取所有索引信息
        // 实际应用中可以通过其他方式获取索引信息，这里返回空列表
        return new ArrayList<>();
    }

    /**
     * 创建顶点标签
     *
     * @param management 图管理对象
     * @param entities   实体列表
     */
    private void createVertexLabels(JanusGraphManagement management, List<GraphEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (GraphEntity entity : entities) {
            if (!management.containsVertexLabel(entity.getLabel())) {
                management.makeVertexLabel(entity.getLabel()).make();
                log.info("Created vertex label: {}", entity.getLabel());
            }
        }
    }

    /**
     * 创建边标签
     *
     * @param management 图管理对象
     * @param relations  关系列表
     */
    private void createEdgeLabels(JanusGraphManagement management, List<GraphRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }

        for (GraphRelation relation : relations) {
            if (!management.containsEdgeLabel(relation.getLabel())) {
                Multiplicity multiplicity = relation.getMultiple() ? Multiplicity.MULTI : Multiplicity.SIMPLE;
                management.makeEdgeLabel(relation.getLabel())
                        .multiplicity(multiplicity)
                        .make();
                log.info("Created edge label: {}", relation.getLabel());
            }
        }
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
        Set<GraphProperty> allProperties = entities.stream()
                .filter(entity -> entity.getProperties() != null)
                .flatMap(entity -> entity.getProperties().stream())
                .collect(Collectors.toSet());

        if (relations != null) {
            List<GraphProperty> relationProperties = relations.stream()
                    .filter(relation -> relation.getProperties() != null)
                    .flatMap(relation -> relation.getProperties().stream())
                    .toList();
            allProperties.addAll(relationProperties);
        }

        // 创建属性键
        for (GraphProperty property : allProperties) {
            if (!management.containsPropertyKey(property.getCode())) {
                // 根据数据类型创建属性键
                PropertyKey propertyKey = management.makePropertyKey(property.getCode())
                        .dataType(JanusUtil.getJanusDataType(property.getDataType()))
                        .cardinality(Cardinality.SINGLE)
                        .make();
                log.info("Created property key: {} with type: {}, propertyKey: {}", property.getCode(), property.getDataType(), propertyKey);
            }
        }
    }

    /**
     * 创建索引
     *
     * @param management 图管理对象
     * @param indexes    索引列表
     */
    private void createIndices(JanusGraphManagement management, List<GraphIndex> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return;
        }
        for (GraphIndex index : indexes) {
            if (CollectionUtils.isEmpty(index.getPropertyNames())) {
                log.warn("index ({}) contains empty property keys, skip.", index.getName());
                continue;
            }
            // 创建图索引
            if (isGraphIndex(index.getType())) {
                createVertexIndex(management, index);
            }
            // 创建关系索引
            if (isRelationIndex(index.getType())) {
                createRelationIndex(index, management);
            }
        }
    }


    private void createVertexIndex(JanusGraphManagement mgmt, GraphIndex index) {
        String name = index.getName();
        if (mgmt.containsGraphIndex(index.getName())) {
            log.warn("graph index ({}) is already exists.", index.getName());
            return;
        }

        JanusGraphManagement.IndexBuilder builder;
        if (Objects.equals(index.getSchemaType(), "vertex")) {
            builder = mgmt.buildIndex(name, Vertex.class);
        } else if (Objects.equals(index.getSchemaType(), "edge")) {
            builder = mgmt.buildIndex(name, Edge.class);
        } else {
            // never happen
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
        if (IndexType.MIX.equals(index.getType())) {
            builder.buildMixedIndex(BACKING_INDEX);
        }
    }

    private static void addPropertyKeyForIndex(JanusGraphManagement.IndexBuilder builder, GraphIndex index, JanusGraphManagement mgmt) {
        for (String propName : index.getPropertyNames()) {
            PropertyKey propertyKey = mgmt.getPropertyKey(propName);
            if (IndexType.MIX.equals(index.getType())) {
                // 如果是混合索引（用于支持模糊查找）
                builder.addKey(propertyKey, Mapping.STRING.asParameter(), Parameter.of(ParameterType.STRING_ANALYZER.getName(), "ik_max_word"));
            } else {
                builder.addKey(propertyKey);
            }
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
    }
}