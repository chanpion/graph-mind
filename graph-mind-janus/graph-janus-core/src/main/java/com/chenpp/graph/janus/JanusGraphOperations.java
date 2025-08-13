package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import lombok.extern.slf4j.Slf4j;
import org.janusgraph.core.*;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JanusGraph图操作实现类
 *
 * @author April.Chen
 * @date 2025/8/13 16:00
 */
@Slf4j
public class JanusGraphOperations implements GraphOperations {
    private JanusGraph graph;

    public JanusGraphOperations(JanusGraph graph) {
        this.graph = graph;
    }

    @Override
    public void createGraph(GraphConf graphConf) throws GraphException {
        // JanusGraph中创建图空间的操作通常在配置中完成，这里可以做一些初始化工作
        try {
            if (graph == null || graph.isClosed()) {
                throw new GraphException("JanusGraph instance is not available");
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

    /**
     * 创建顶点标签
     *
     * @param management  图管理对象
     * @param entities    实体列表
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
                EdgeLabel edgeLabel = management.makeEdgeLabel(relation.getLabel()).make();
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
        List<com.chenpp.graph.core.schema.GraphProperty> allProperties = entities.stream()
                .filter(entity -> entity.getProperties() != null)
                .flatMap(entity -> entity.getProperties().stream())
                .collect(Collectors.toList());
        
        if (relations != null) {
            List<com.chenpp.graph.core.schema.GraphProperty> relationProperties = relations.stream()
                    .filter(relation -> relation.getProperties() != null)
                    .flatMap(relation -> relation.getProperties().stream())
                    .collect(Collectors.toList());
            allProperties.addAll(relationProperties);
        }

        // 创建属性键
        for (com.chenpp.graph.core.schema.GraphProperty property : allProperties) {
            if (!management.containsPropertyKey(property.getCode())) {
                // 根据数据类型创建属性键
                PropertyKey propertyKey;
                switch (property.getDataType()) {
                    case String:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(String.class).make();
                        break;
                    case Integer:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(Integer.class).make();
                        break;
                    case Long:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(Long.class).make();
                        break;
                    case Double:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(Double.class).make();
                        break;
                    case Boolean:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(Boolean.class).make();
                        break;
                    case Date:
                    case Datetime:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(java.util.Date.class).make();
                        break;
                    default:
                        propertyKey = management.makePropertyKey(property.getCode()).dataType(String.class).make();
                        break;
                }
                log.info("Created property key: {} with type: {}", property.getCode(), property.getDataType());
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
            String indexName = index.getName();
            if (!management.containsGraphIndex(indexName)) {
                // 创建混合索引
                if (index.getPropertyNames() != null && !index.getPropertyNames().isEmpty()) {
                    // 获取第一个属性键作为示例创建索引
                    String propertyName = index.getPropertyNames().get(0);
                    PropertyKey key = management.getPropertyKey(propertyName);
                    
                    if (key != null) {
                        // 创建混合索引
                        management.buildIndex(indexName, org.janusgraph.core.JanusGraphVertex.class)
                                .addKey(key)
                                .buildMixedIndex("search");
                        log.info("Created mixed index: {} on property: {}", indexName, propertyName);
                    }
                }
            }
        }
    }
}