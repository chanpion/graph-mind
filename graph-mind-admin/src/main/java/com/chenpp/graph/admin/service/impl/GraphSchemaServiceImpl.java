package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.admin.service.GraphService;
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
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * @author April.Chen
 * @date 2025/8/12 15:46
 */
@Slf4j
@Service
public class GraphSchemaServiceImpl implements GraphSchemaService {


    @Resource
    private GraphService graphService;

    @Resource
    private GraphConnectionService connectionService;

    @Resource
    private GraphVertexDefService graphVertexDefService;

    @Resource
    private GraphEdgeDefService graphEdgeDefService;

    @Resource
    private GraphPropertyDefService graphPropertyDefService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public GraphSchema discoverSchema(Long connectionId, String graphCode) {
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
    public GraphSchema discoverSchema(Long graphId) {
        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo == null) {
            throw new GraphException("图不存在");
        }
        // 获取图数据库连接信息
        GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
        if (connection == null) {
            throw new GraphException("图数据库连接不存在");
        }

        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphOperations graphOperations = graphClient.opsForGraph();
        return graphOperations.getPublishedSchema(graphConf);
    }

    @Override
    public List<GraphVertexDef> discoverVertexDefs(Long graphId, Long connectionId, String graphCode) {
        GraphSchema schema;
        if (connectionId != null && graphCode != null) {
            schema = discoverSchema(connectionId, graphCode);
        } else {
            schema = discoverSchema(graphId);
        }
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
        GraphSchema schema;
        if (connectionId != null && graphCode != null) {
            schema = discoverSchema(connectionId, graphCode);
        } else {
            schema = discoverSchema(graphId);
        }
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
        GraphSchema schema;
        if (connectionId != null && graphCode != null) {
            schema = discoverSchema(connectionId, graphCode);
        } else {
            schema = discoverSchema(graphId);
        }
        if (schema == null || schema.getEntities() == null) {
            return;
        }
        Map<String, List<GraphProperty>> labelPropsMap = schema.getEntities().stream()
                .collect(Collectors.toMap(GraphEntity::getLabel, GraphEntity::getProperties, (a, b) -> a));
        for (GraphVertexDef vertexDef : vertexDefs) {
            List<GraphProperty> discovered = labelPropsMap.get(vertexDef.getLabel());
            if (discovered != null && !discovered.isEmpty()) {
                List<GraphPropertyDef> existing = vertexDef.getProperties() != null
                        ? vertexDef.getProperties() : new ArrayList<>();
                for (GraphProperty p : discovered) {
                    boolean exists = existing.stream().anyMatch(e -> p.getCode().equals(e.getCode()));
                    if (!exists) {
                        existing.add(buildPropertyDef(p));
                    }
                }
                vertexDef.setProperties(existing);
            }
        }
    }

    @Override
    public void mergeDiscoveredEdgeProperties(List<GraphEdgeDef> edgeDefs, Long graphId, Long connectionId, String graphCode) {
        GraphSchema schema;
        if (connectionId != null && graphCode != null) {
            schema = discoverSchema(connectionId, graphCode);
        } else {
            schema = discoverSchema(graphId);
        }
        if (schema == null || schema.getRelations() == null) {
            return;
        }
        Map<String, List<GraphProperty>> labelPropsMap = schema.getRelations().stream()
                .collect(Collectors.toMap(GraphRelation::getLabel, GraphRelation::getProperties, (a, b) -> a));
        for (GraphEdgeDef edgeDef : edgeDefs) {
            List<GraphProperty> discovered = labelPropsMap.get(edgeDef.getLabel());
            if (discovered != null && !discovered.isEmpty()) {
                List<GraphPropertyDef> existing = edgeDef.getProperties() != null
                        ? edgeDef.getProperties() : new ArrayList<>();
                for (GraphProperty p : discovered) {
                    boolean exists = existing.stream()
                            .anyMatch(e -> p.getCode().equals(e.getCode()));
                    if (!exists) {
                        existing.add(buildPropertyDef(p));
                    }
                }
                edgeDef.setProperties(existing);
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

        List<GraphEntity> entities = vertices.stream().map(vertex -> {
            GraphEntity entity = new GraphEntity();
            entity.setLabel(vertex.getLabel());
            entity.setProperties(transformGraphProperty(vertex.getProperties()));
            return entity;
        }).toList();

        List<GraphRelation> relations = edges.stream().map(edge -> {
            GraphRelation relation = new GraphRelation();
            relation.setLabel(edge.getLabel());
            relation.setStartLabel(edge.getStartLabel());
            relation.setEndLabel(edge.getEndLabel());
            relation.setProperties(transformGraphProperty(edge.getProperties()));
            relation.setMultiple(edge.getMultiple());
            return relation;
        }).toList();
        graphSchema.setEntities(entities);
        graphSchema.setRelations(relations);


        List<GraphIndex> indexes = new ArrayList<>();
        vertices.forEach(vertex -> vertex.getProperties().forEach(p -> {
            if (p.getIndexed()) {
                GraphIndex index = new GraphIndex();
                index.setLabel(vertex.getLabel());
                index.setProperty(p.getCode());
                index.setType(IndexType.COMPOSITE.code());
                index.setSchemaType("vertex");
                index.setPropertyNames(Collections.singletonList(p.getCode()));
                index.setName(String.format("idx_%s_%s_%s", graphInfo.getCode(), vertex.getLabel(), p.getCode()));
                indexes.add(index);
            }
        }));

        edges.forEach(edge -> edge.getProperties().forEach(p -> {
            if (p.getIndexed()) {
                GraphIndex index = new GraphIndex();
                index.setLabel(edge.getLabel());
                index.setProperty(p.getCode());
                index.setType(IndexType.COMPOSITE.code());
                index.setSchemaType("edge");
                index.setPropertyNames(Collections.singletonList(p.getCode()));
                index.setName(String.format("idx_%s_%s_%s", graphInfo.getCode(), edge.getLabel(), p.getCode()));
                indexes.add(index);
            }
        }));

        graphSchema.setIndexes(indexes);
        return graphSchema;
    }

    @Override
    public void publishSchema(Long graphId) {
        publishSchema(graphId, null, null);
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
            // Discovered graph（graphId < 0），使用 connectionId + graphCode
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

        // 将索引分为新实体索引（在 applySchema 中创建）和变更实体索引（在 alterSchema 之后创建）
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
        // 变更实体的索引在 alterSchema 之后创建，确保属性已存在
        if (!alterEntityIndexes.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!alterEntityIndexes.isEmpty()) {
            GraphSchema alterIndexSchema = new GraphSchema();
            alterIndexSchema.setIndexes(alterEntityIndexes);
            graphOperations.applySchema(graphConf, alterIndexSchema);
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

    public List<GraphProperty> transformGraphProperty(List<GraphPropertyDef> properties) {
        return properties.stream().map(prop -> {
            GraphProperty property = new GraphProperty();
            property.setName(prop.getName());
            property.setCode(prop.getCode());
            property.setDataType(DataType.instanceOf(prop.getType()));
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


        // 导入边定义
        if (importEdges != null) {
            for (GraphEdgeDef edge : importEdges) {
                if (!replace) {
                    List<GraphEdgeDef> existingEdges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);
                    boolean exists = existingEdges.stream().anyMatch(e -> Objects.equals(e.getLabel(), edge.getLabel()));
                    if (exists) {
                        continue;
                    }
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

        graphOperations.createGraph(graphConf);
        log.info("已在图数据库中创建图，graphCode={}", graphInfo.getCode());

        graphInfo.setStatus(0);
        graphInfo.setGraphType(connection.getGraphTypeEnum());
        graphService.save(graphInfo);
        log.info("已保存图到 MySQL，graphId={}", graphInfo.getId());

        return graphInfo.getId();
    }
}
