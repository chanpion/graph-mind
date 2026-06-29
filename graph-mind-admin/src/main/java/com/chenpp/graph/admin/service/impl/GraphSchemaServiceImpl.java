package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphEntityDef;
import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.core.schema.IndexType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;


/**
 * @author April.Chen
 * @date 2025/8/12 15:46
 */
@Slf4j
@Service
public class GraphSchemaServiceImpl implements GraphSchemaService {


    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphConnectionService connectionService;

    @Autowired
    private GraphVertexDefService graphVertexDefService;

    @Autowired
    private GraphEdgeDefService graphEdgeDefService;

    @Autowired
    private GraphPropertyDefService graphPropertyDefService;

    @Autowired
    private TransactionTemplate transactionTemplate;


    @Override
    public GraphSchema discoverSchema(Long connectionId, String graphCode, Long graphId) {
        if (graphId != null && graphId > 0) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                throw new GraphException("图不存在");
            }
            graphCode = graphInfo.getCode();
            connectionId = graphInfo.getConnectionId();
        }
        GraphConnection connection = connectionService.getById(connectionId);
        if (connection == null) {
            throw new GraphException("图数据库连接不存在");
        }

        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphCode);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphOperations graphOperations = graphClient.opsForGraph();
        return graphOperations.getPublishedSchema(graphConf);
    }


    @Override
    public List<GraphVertexDef> discoverVertexDefs(Long graphId, Long connectionId, String graphCode) {
        GraphSchema schema = discoverSchema(connectionId, graphCode, graphId);
        if (schema == null || schema.getEntities() == null || schema.getEntities().isEmpty()) {
            return new ArrayList<>();
        }
        AtomicLong idCounter = new AtomicLong(-1);
        return schema.getEntities().stream().map(entity -> {
            GraphVertexDef vertexDef = new GraphVertexDef();
            vertexDef.setId(idCounter.decrementAndGet());
            vertexDef.setGraphId(graphId);
            vertexDef.setLabel(entity.getLabel());
            vertexDef.setName(entity.getLabel());
            vertexDef.setStatus(1);
            if (entity.getProperties() != null) {
                List<GraphPropertyDef> props = entity.getProperties().stream()
                        .map(this::buildPropertyDef)
                        .collect(Collectors.toList());
                vertexDef.setProperties(props);
            }
            return vertexDef;
        }).collect(Collectors.toList());
    }

    @Override
    public List<GraphEdgeDef> discoverEdgeDefs(Long graphId, Long connectionId, String graphCode) {
        GraphSchema schema = discoverSchema(connectionId, graphCode, graphId);
        if (schema == null || schema.getRelations() == null || schema.getRelations().isEmpty()) {
            return new ArrayList<>();
        }
        AtomicLong idCounter = new AtomicLong(-1000);
        return schema.getRelations().stream().map(relation -> {
            GraphEdgeDef edgeDef = new GraphEdgeDef();
            edgeDef.setId(idCounter.decrementAndGet());
            edgeDef.setGraphId(graphId);
            edgeDef.setLabel(relation.getLabel());
            edgeDef.setName(relation.getLabel());
            edgeDef.setStatus(1);
            edgeDef.setMultiple(relation.getMultiple());
            edgeDef.setStartLabel(relation.getStartLabel());
            edgeDef.setEndLabel(relation.getEndLabel());
            if (relation.getProperties() != null) {
                List<GraphPropertyDef> props = relation.getProperties().stream()
                        .map(this::buildPropertyDef)
                        .collect(Collectors.toList());
                edgeDef.setProperties(props);
            }
            return edgeDef;
        }).collect(Collectors.toList());
    }

    @Override
    public void mergeDiscoveredVertexProperties(List<GraphVertexDef> vertexDefs, Long graphId, Long connectionId, String graphCode) {
        mergeDiscoveredEntityProperties(vertexDefs, graphId, connectionId, graphCode, true);
    }

    @Override
    public void mergeDiscoveredEdgeProperties(List<GraphEdgeDef> edgeDefs, Long graphId, Long connectionId, String graphCode) {
        mergeDiscoveredEntityProperties(edgeDefs, graphId, connectionId, graphCode, false);
    }

    private <T extends GraphEntityDef> void mergeDiscoveredEntityProperties(
            List<T> defs, Long graphId, Long connectionId, String graphCode, boolean isVertex) {
        GraphSchema schema = discoverSchema(connectionId, graphCode, graphId);
        if (schema == null) {
            return;
        }
        Map<String, List<GraphProperty>> labelPropsMap;
        if (isVertex) {
            List<GraphEntity> entities = schema.getEntities();
            if (entities == null) {
                return;
            }
            labelPropsMap = entities.stream()
                    .collect(Collectors.toMap(GraphEntity::getLabel, GraphEntity::getProperties, (a, b) -> a));
        } else {
            List<GraphRelation> relations = schema.getRelations();
            if (relations == null) {
                return;
            }
            labelPropsMap = relations.stream()
                    .collect(Collectors.toMap(GraphRelation::getLabel, GraphRelation::getProperties, (a, b) -> a));
        }
        for (T def : defs) {
            List<GraphProperty> discovered = labelPropsMap.get(def.getLabel());
            if (discovered != null && !discovered.isEmpty()) {
                List<GraphPropertyDef> existing = def.getProperties() != null
                        ? def.getProperties() : new ArrayList<>();
                for (GraphProperty p : discovered) {
                    boolean exists = existing.stream().anyMatch(e -> p.getCode().equals(e.getCode()));
                    if (!exists) {
                        existing.add(buildPropertyDef(p));
                    }
                }
                def.setProperties(existing);
            }
        }
    }

    private GraphPropertyDef buildPropertyDef(GraphProperty p) {
        GraphPropertyDef prop = new GraphPropertyDef();
        prop.setCode(p.getCode());
        prop.setName(p.getName());
        prop.setType(p.getDataType() != null ? p.getDataType().name() : "String");
        prop.setStatus(1);
        prop.setIndexed(false);
        return prop;
    }

    @Override
    public GraphSchema getGraphSchema(Long graphId) {
        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo == null) {
            log.warn("图不存在，返回空Schema，graphId={}", graphId);
            return new GraphSchema();
        }
        List<GraphVertexDef> vertices = graphVertexDefService.getVertexDefsByGraphId(graphId, null);
        List<GraphEdgeDef> edges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);
        GraphSchema graphSchema = new GraphSchema();
        graphSchema.setGraphCode(graphInfo.getCode());

        List<GraphIndex> indexes = new ArrayList<>();

        List<GraphEntity> entities = vertices.stream().map(vertex -> {
            GraphEntity entity = new GraphEntity();
            entity.setLabel(vertex.getLabel());
            List<GraphProperty> properties = transformGraphProperty(vertex.getProperties());
            properties.forEach(p -> {
                if (p.getIndexed()) {
                    GraphIndex index = transformGraphIndex(graphInfo.getCode(), entity.getLabel(), GraphConstants.VERTEX, p);
                    indexes.add(index);
                }
            });

            entity.setProperties(properties);
            return entity;
        }).toList();

        List<GraphRelation> relations = edges.stream().map(edge -> {
            GraphRelation relation = new GraphRelation();
            relation.setLabel(edge.getLabel());
            relation.setStartLabel(edge.getStartLabel());
            relation.setEndLabel(edge.getEndLabel());
            relation.setMultiple(edge.getMultiple());
            List<GraphProperty> properties = transformGraphProperty(edge.getProperties());
            properties.forEach(p -> {
                if (p.getIndexed()) {
                    GraphIndex index = transformGraphIndex(graphInfo.getCode(), edge.getLabel(), GraphConstants.EDGE, p);
                    indexes.add(index);
                }
            });

            relation.setProperties(properties);

            return relation;
        }).toList();
        graphSchema.setEntities(entities);
        graphSchema.setRelations(relations);
        graphSchema.setIndexes(indexes);
        return graphSchema;
    }

    @Override
    public void publishSchema(Long graphId, Long connectionId, String graphCode) {
        log.info("发布图Schema: graphId={}, connectionId={}, graphCode={}", graphId, connectionId, graphCode);

        GraphConf graphConf;
        GraphInfo graphInfo;
        GraphConnection connection = null;

        if (graphId != null && graphId > 0) {
            // 本地管理的图
            graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                log.warn("图不存在，跳过发布Schema，graphId={}", graphId);
                return;
            }
            connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                return;
            }
            graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
        } else {
            graphInfo = null;
            if (connectionId == null || graphCode == null) {
                log.warn("Discovered graph 需要 connectionId 和 graphCode，跳过发布Schema");
                return;
            }
            graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        }

        List<GraphVertexDef> vertices = graphVertexDefService.getVertexDefsByGraphId(graphId, null);
        List<GraphEdgeDef> edges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);

        GraphSchema graphSchema = getGraphSchema(graphId);

        log.info("开始发布Schema，graphId={}, graphCode={}, 图数据库类型={}, 顶点类型数={}, 边类型数={}",
                graphId, graphConf.getGraphCode(), graphConf.getType(),
                graphSchema.getEntities() != null ? graphSchema.getEntities().size() : 0,
                graphSchema.getRelations() != null ? graphSchema.getRelations().size() : 0);

        // Step 1: 查询远程已发布的 Schema，对比后仅发布变更
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphOperations graphOperations = graphClient.opsForGraph();

        GraphSchema remoteSchema;
        try {
            remoteSchema = graphOperations.getPublishedSchema(graphConf);
        } catch (Exception e) {
            log.warn("获取远程Schema失败，将全量发布: {}", e.getMessage());
            remoteSchema = new GraphSchema();
        }

        Set<String> remoteLabelNames = remoteSchema.getEntities() != null
                ? remoteSchema.getEntities().stream().map(GraphEntity::getLabel).collect(Collectors.toSet())
                : Set.of();
        Set<String> remoteEdgeNames = remoteSchema.getRelations() != null
                ? remoteSchema.getRelations().stream().map(GraphRelation::getLabel).collect(Collectors.toSet())
                : Set.of();
        Set<String> remoteIndexNames = remoteSchema.getIndexes() != null
                ? remoteSchema.getIndexes().stream().map(GraphIndex::getName).collect(Collectors.toSet())
                : Set.of();

        List<GraphEntity> newEntities = new ArrayList<>();
        List<GraphEntity> alterEntities = new ArrayList<>();
        if (graphSchema.getEntities() != null) {
            for (GraphEntity entity : graphSchema.getEntities()) {
                if (remoteLabelNames.contains(entity.getLabel())) {
                    alterEntities.add(entity);
                } else {
                    newEntities.add(entity);
                }
            }
        }

        List<GraphRelation> newRelations = new ArrayList<>();
        List<GraphRelation> alterRelations = new ArrayList<>();
        if (graphSchema.getRelations() != null) {
            for (GraphRelation relation : graphSchema.getRelations()) {
                if (remoteEdgeNames.contains(relation.getLabel())) {
                    alterRelations.add(relation);
                } else {
                    newRelations.add(relation);
                }
            }
        }

        List<GraphIndex> newIndexes = new ArrayList<>();
        if (graphSchema.getIndexes() != null) {
            for (GraphIndex idx : graphSchema.getIndexes()) {
                if (!remoteIndexNames.contains(idx.getName())) {
                    newIndexes.add(idx);
                }
            }
        }

        Set<String> newEntityLabels = newEntities.stream()
                .map(GraphEntity::getLabel)
                .collect(Collectors.toSet());
        Set<String> newRelationLabels = newRelations.stream()
                .map(GraphRelation::getLabel)
                .collect(Collectors.toSet());
        List<GraphIndex> newEntityIndexes = new ArrayList<>();
        List<GraphIndex> alterEntityIndexes = new ArrayList<>();
        for (GraphIndex idx : newIndexes) {
            boolean isNewEntity = GraphConstants.VERTEX.equalsIgnoreCase(idx.getSchemaType())
                    ? newEntityLabels.contains(idx.getLabel())
                    : newRelationLabels.contains(idx.getLabel());
            if (isNewEntity) {
                newEntityIndexes.add(idx);
            } else {
                alterEntityIndexes.add(idx);
            }
        }

        log.info("Schema diff: newTags={}, alterTags={}, newEdges={}, alterEdges={}, newIndexes={}, alterIndexes={}",
                newEntities.size(), alterEntities.size(), newRelations.size(), alterRelations.size(), newEntityIndexes.size(), alterEntityIndexes.size());

        GraphSchema newSchema = new GraphSchema();
        newSchema.setEntities(newEntities);
        newSchema.setRelations(newRelations);
        newSchema.setIndexes(newEntityIndexes);

        GraphSchema alterSchema = new GraphSchema();
        alterSchema.setEntities(alterEntities);
        alterSchema.setRelations(alterRelations);

        boolean hasNew = !newEntities.isEmpty() || !newRelations.isEmpty() || !newEntityIndexes.isEmpty();
        boolean hasAlter = !alterEntities.isEmpty() || !alterRelations.isEmpty() || !alterEntityIndexes.isEmpty();

        if (hasNew) {
            graphOperations.applySchema(graphConf, newSchema);
        }
        if (hasAlter) {
            graphOperations.alterSchema(graphConf, alterSchema);
        }
        if (!alterEntityIndexes.isEmpty()) {
            GraphSchema alterIndexSchema = new GraphSchema();
            alterIndexSchema.setIndexes(alterEntityIndexes);
            // 重试机制：schema 变更后索引创建可能需要等待属性就绪
            int maxRetries = 3;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    graphOperations.applySchema(graphConf, alterIndexSchema);
                    break;
                } catch (Exception e) {
                    if (attempt < maxRetries - 1) {
                        log.debug("索引创建失败，第{}次重试: {}", attempt + 1, e.getMessage());
                        try {
                            TimeUnit.MILLISECONDS.sleep(200L * (attempt + 1));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        throw e;
                    }
                }
            }
        }
        if (!hasNew && !hasAlter) {
            log.info("Schema 无变更，跳过发布，graphId={}", graphId);
        }
        log.info("Schema 已应用到图数据库，graphId={}", graphId);

        // Step 2: 更新 MySQL 元数据状态为已发布（仅对本地管理的图，discovered graph 无需更新）
        if (graphInfo != null) {
            List<GraphPropertyDef> propertyList = new ArrayList<>();
            transactionTemplate.executeWithoutResult(status -> {
                vertices.forEach(vertex -> {
                    vertex.setStatus(1);
                    propertyList.addAll(vertex.getProperties());
                });
                graphVertexDefService.updateBatchById(vertices);

                edges.forEach(edge -> {
                    edge.setStatus(1);
                    propertyList.addAll(edge.getProperties());
                });
                graphEdgeDefService.updateBatchById(edges);
                propertyList.forEach(p -> p.setStatus(1));
                graphPropertyDefService.updateBatchById(propertyList);

                graphInfo.setStatus(1);
                graphService.updateById(graphInfo);
            });
            log.info("Schema 发布完成，graphId={}", graphId);
        } else {
            log.info("Discovered graph Schema 已发布（无需更新 MySQL 元数据），graphId={}", graphId);
        }
    }

    @Override
    public void publishVertexDef(Long graphId, Long connectionId, String graphCode, String label) {
        // 未传 connectionId/graphCode 时从 graphId 解析
        if ((connectionId == null || graphCode == null) && graphId != null) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo != null) {
                connectionId = graphInfo.getConnectionId();
                graphCode = graphInfo.getCode();
            }
        }
        GraphVertexDef vertexDef = null;
        if (graphId != null && label != null) {
            vertexDef = graphVertexDefService.getOne(
                    new QueryWrapper<GraphVertexDef>().eq("graph_id", graphId).eq("label", label));
        }

        GraphEntity entity = new GraphEntity();
        entity.setLabel(label);
        if (vertexDef != null && vertexDef.getProperties() != null) {
            entity.setProperties(transformGraphProperty(vertexDef.getProperties()));
        }

        GraphSchema schema = new GraphSchema();
        schema.setEntities(List.of(entity));

        publishSingleSchema(connectionId, graphCode, schema);

        if (vertexDef != null) {
            vertexDef.setStatus(1);
            graphVertexDefService.updateById(vertexDef);
        }
    }

    @Override
    public void publishEdgeDef(Long graphId, Long connectionId, String graphCode, String label) {
        // 未传 connectionId/graphCode 时从 graphId 解析
        if ((connectionId == null || graphCode == null) && graphId != null) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo != null) {
                connectionId = graphInfo.getConnectionId();
                graphCode = graphInfo.getCode();
            }
        }
        GraphEdgeDef edgeDef = null;
        if (graphId != null && label != null) {
            edgeDef = graphEdgeDefService.getOne(
                    new QueryWrapper<GraphEdgeDef>().eq("graph_id", graphId).eq("label", label));
        }

        GraphRelation relation = new GraphRelation();
        relation.setLabel(label);
        if (edgeDef != null) {
            relation.setStartLabel(edgeDef.getStartLabel());
            relation.setEndLabel(edgeDef.getEndLabel());
            relation.setMultiple(edgeDef.getMultiple());
            if (edgeDef.getProperties() != null) {
                relation.setProperties(transformGraphProperty(edgeDef.getProperties()));
            }
        }

        GraphSchema schema = new GraphSchema();
        schema.setRelations(List.of(relation));

        publishSingleSchema(connectionId, graphCode, schema);

        if (edgeDef != null) {
            edgeDef.setStatus(1);
            graphEdgeDefService.updateById(edgeDef);
        }
    }

    private void publishSingleSchema(Long connectionId, String graphCode, GraphSchema schema) {
        GraphConnection connection = connectionService.getById(connectionId);
        if (connection == null) {
            log.error("图数据库连接不存在，connectionId={}", connectionId);
            return;
        }
        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphCode);

        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphOperations graphOperations = graphClient.opsForGraph();

        // 获取远端已发布的 Schema 来判断新标签还是变更
        GraphSchema remoteSchema;
        try {
            remoteSchema = graphOperations.getPublishedSchema(graphConf);
        } catch (Exception e) {
            log.warn("获取远程Schema失败，将全量发布: {}", e.getMessage());
            remoteSchema = new GraphSchema();
        }

        boolean hasNew = false;
        boolean hasAlter = false;
        GraphSchema newSchema = new GraphSchema();
        GraphSchema alterSchema = new GraphSchema();

        if (schema.getEntities() != null && !schema.getEntities().isEmpty()) {
            Set<String> remoteLabels = remoteSchema.getEntities() != null
                    ? remoteSchema.getEntities().stream().map(GraphEntity::getLabel).collect(Collectors.toSet())
                    : Set.of();
            List<GraphEntity> newEntities = new ArrayList<>();
            List<GraphEntity> alterEntities = new ArrayList<>();
            for (GraphEntity entity : schema.getEntities()) {
                if (remoteLabels.contains(entity.getLabel())) {
                    alterEntities.add(entity);
                } else {
                    newEntities.add(entity);
                }
            }
            newSchema.setEntities(newEntities);
            alterSchema.setEntities(alterEntities);
            if (!newEntities.isEmpty()) hasNew = true;
            if (!alterEntities.isEmpty()) hasAlter = true;
        }

        if (schema.getRelations() != null && !schema.getRelations().isEmpty()) {
            Set<String> remoteEdges = remoteSchema.getRelations() != null
                    ? remoteSchema.getRelations().stream().map(GraphRelation::getLabel).collect(Collectors.toSet())
                    : Set.of();
            List<GraphRelation> newRelations = new ArrayList<>();
            List<GraphRelation> alterRelations = new ArrayList<>();
            for (GraphRelation relation : schema.getRelations()) {
                if (remoteEdges.contains(relation.getLabel())) {
                    alterRelations.add(relation);
                } else {
                    newRelations.add(relation);
                }
            }
            newSchema.setRelations(newRelations);
            alterSchema.setRelations(alterRelations);
            if (!newRelations.isEmpty()) hasNew = true;
            if (!alterRelations.isEmpty()) hasAlter = true;
        }

        if (hasNew) {
            graphOperations.applySchema(graphConf, newSchema);
        }
        if (hasAlter) {
            graphOperations.alterSchema(graphConf, alterSchema);
        }

        log.info("增量 Schema 发布完成");
    }

    public List<GraphProperty> transformGraphProperty(List<GraphPropertyDef> properties) {
        return properties.stream().map(prop -> {
            GraphProperty property = new GraphProperty();
            property.setName(prop.getName());
            property.setCode(prop.getCode());
            String typeStr = prop.getType();
            DataType dataType = DataType.instanceOf(typeStr);
            if (dataType == null) {
                if (typeStr != null && !typeStr.isEmpty()) {
                    log.warn("Failed to parse data type '{}' for property '{}', using String as default", typeStr, prop.getCode());
                }
                dataType = DataType.String; // 设置默认值，避免NPE
            }
            property.setDataType(dataType);
            property.setIndexed(prop.getIndexed());
            return property;
        }).collect(Collectors.toList());
    }

    @Override
    public SchemaExportDTO exportSchema(Long graphId) {
        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo == null) {
            throw new GraphException("图不存在");
        }
        List<GraphVertexDef> vertices = graphVertexDefService.getVertexDefsByGraphId(graphId, null);
        List<GraphEdgeDef> edges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);

        SchemaExportDTO dto = new SchemaExportDTO();
        dto.setExportedAt(LocalDateTime.now().toString());
        dto.setGraphId(graphId);
        dto.setGraphCode(graphInfo.getCode());
        dto.setVertices(vertices);
        dto.setEdges(edges);
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importSchema(Long graphId, SchemaImportDTO importDTO) {
        if (importDTO == null) {
            throw new GraphException("导入数据为空");
        }

        List<GraphVertexDef> importVertices = importDTO.getVertices();
        List<GraphEdgeDef> importEdges = importDTO.getEdges();
        if (CollectionUtils.isEmpty(importVertices) && CollectionUtils.isEmpty(importEdges)) {
            throw new GraphException("导入数据为空");
        }

        String mode = importDTO.getMode();
        boolean replace = "replace".equalsIgnoreCase(mode);

        if (replace) {
            List<GraphVertexDef> existingVertices = graphVertexDefService.getVertexDefsByGraphId(graphId, null);
            for (GraphVertexDef vertex : existingVertices) {
                graphVertexDefService.deleteVertexDefWithProperties(vertex.getId());
            }
            List<GraphEdgeDef> existingEdges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);
            for (GraphEdgeDef edge : existingEdges) {
                graphEdgeDefService.deleteEdgeDefWithProperties(edge.getId());
            }
        }

        Map<String, Long> existingVertexLabelIdMap;
        if (replace) {
            existingVertexLabelIdMap = Map.of();
        } else {
            List<GraphVertexDef> existingVertices = graphVertexDefService.getVertexDefsByGraphId(graphId, null);
            existingVertexLabelIdMap = existingVertices.stream()
                    .filter(n -> n.getLabel() != null)
                    .collect(Collectors.toMap(GraphVertexDef::getLabel, GraphVertexDef::getId, (a, b) -> a));
        }

        if (importVertices != null) {
            for (GraphVertexDef vertex : importVertices) {
                if (!replace && existingVertexLabelIdMap.containsKey(vertex.getLabel())) {
                    continue;
                }
                vertex.setGraphId(graphId);
                vertex.setId(null);
                vertex.setStatus(0);
                graphVertexDefService.saveVertexDefWithProperties(vertex);
            }
        }


        if (importEdges != null) {
            Map<String, Boolean> existingEdgeLabelMap;
            if (replace) {
                existingEdgeLabelMap = Map.of();
            } else {
                existingEdgeLabelMap = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null).stream()
                        .filter(e -> e.getLabel() != null)
                        .collect(Collectors.toMap(GraphEdgeDef::getLabel, e -> true, (a, b) -> a));
            }
            for (GraphEdgeDef edge : importEdges) {
                if (!replace && existingEdgeLabelMap.containsKey(edge.getLabel())) {
                    continue;
                }
                edge.setGraphId(graphId);
                edge.setId(null);
                edge.setStatus(0);
                graphEdgeDefService.saveEdgeDefWithProperties(edge);
            }
        }

        log.info("Schema导入完成，graphId={}, mode={}, vertices={}, edges={}", graphId, mode,
                importVertices != null ? importVertices.size() : 0,
                importEdges != null ? importEdges.size() : 0);
    }

    @Override
    public Long createGraphInDatabase(GraphInfo graphInfo) {
        if (graphInfo == null) {
            throw new GraphException("图信息为空");
        }
        GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
        if (connection == null) {
            throw new GraphException("图数据库连接不存在");
        }

        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphOperations graphOperations = graphClient.opsForGraph();

        List<com.chenpp.graph.core.schema.Graph> existingGraphs = graphOperations.listGraphs(graphConf);
        boolean graphExists = existingGraphs.stream()
                .anyMatch(g -> graphInfo.getCode().equals(g.getCode()));
        if (!graphExists) {
            graphOperations.createGraph(graphConf);
            log.info("已在图数据库中创建图，graphCode={}", graphInfo.getCode());
        } else {
            log.info("图已存在于图数据库中，跳过创建，graphCode={}", graphInfo.getCode());
        }

        graphInfo.setStatus(0);
        graphInfo.setGraphType(connection.getGraphTypeEnum());
        graphService.save(graphInfo);
        log.info("已保存图到 MySQL，graphId={}", graphInfo.getId());

        return graphInfo.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteVertexDef(Long graphId, Long vertexId) {
        log.info("删除节点定义: graphId={}, vertexId={}", graphId, vertexId);

        GraphVertexDef vertexDef = graphVertexDefService.getById(vertexId);
        if (vertexDef == null) {
            log.warn("顶点定义不存在，vertexId={}", vertexId);
            return false;
        }

        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo != null && graphInfo.getConnectionId() != null) {
            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection != null) {
                GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
                GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
                GraphOperations graphOperations = graphClient.opsForGraph();

                graphOperations.dropVertexLabel(graphInfo.getCode(), vertexDef.getLabel());
                log.info("已在图数据库中删除顶点标签: {}", vertexDef.getLabel());
            }
        }

        return graphVertexDefService.deleteVertexDefWithProperties(vertexId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteEdgeDef(Long graphId, Long edgeId) {
        log.info("删除边定义: graphId={}, edgeId={}", graphId, edgeId);

        GraphEdgeDef edgeDef = graphEdgeDefService.getById(edgeId);
        if (edgeDef == null) {
            log.warn("边定义不存在，edgeId={}", edgeId);
            return false;
        }

        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo != null && graphInfo.getConnectionId() != null) {
            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection != null) {
                GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
                GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
                GraphOperations graphOperations = graphClient.opsForGraph();

                graphOperations.dropEdgeLabel(graphInfo.getCode(), edgeDef.getLabel());
                log.info("已在图数据库中删除边类型: {}", edgeDef.getLabel());
            }
        }

        return graphEdgeDefService.deleteEdgeDefWithProperties(edgeId);
    }

    private GraphIndex transformGraphIndex(String graphCode, String label, String schemaType, GraphProperty property) {
        GraphIndex index = new GraphIndex();
        index.setLabel(label);
        index.setProperty(property.getCode());
        index.setType(IndexType.COMPOSITE.code());
        index.setSchemaType(schemaType);
        index.setPropertyNames(List.of(property.getCode()));
        index.setName(String.format("idx_%s_%s_%s", graphCode, label, property.getCode()));
        index.setProperties(List.of(property));
        return index;
    }
}
