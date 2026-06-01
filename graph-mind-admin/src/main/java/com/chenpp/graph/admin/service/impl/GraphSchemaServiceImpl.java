package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.SchemaExportDTO;
import com.chenpp.graph.admin.model.SchemaImportDTO;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphNodeDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphOperations;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private GraphDatabaseConnectionService connectionService;

    @Resource
    private GraphNodeDefService graphNodeDefService;

    @Resource
    private GraphEdgeDefService graphEdgeDefService;

    @Resource
    private GraphPropertyDefService graphPropertyDefService;

    @Override
    public GraphSchema discoverSchema(Long graphId) {
        // 获取图信息
        Graph graph = graphService.getById(graphId);
        if (graph == null) {
            throw new GraphException("图不存在");
        }
        // 获取图数据库连接信息
        GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
        if (connection == null) {
            throw new GraphException("图数据库连接不存在");
        }

        // 创建图客户端并查询图数据库
        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        GraphOperations graphOperations = graphClient.opsForGraph();

        return graphOperations.getPublishedSchema(graphConf);
    }

    @Override
    public GraphSchema getGraphSchema(Long graphId) {
        // 获取图信息
        Graph graph = graphService.getById(graphId);
        List<GraphNodeDef> nodes = graphNodeDefService.getNodeDefsByGraphId(graphId, 0);
        List<GraphEdgeDef> edges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, 0);
        // 构建图模式
        GraphSchema graphSchema = new GraphSchema();
        graphSchema.setGraphCode(graph.getCode());

        List<GraphEntity> entities = nodes.stream().map(node -> {
            GraphEntity entity = new GraphEntity();
            entity.setLabel(node.getLabel());
            entity.setProperties(transformGraphProperty(node.getProperties()));
            return entity;
        }).toList();

        List<GraphRelation> relations = edges.stream().map(edge -> {
            GraphRelation relation = new GraphRelation();
            relation.setLabel(edge.getLabel());
            relation.setSourceLabel(edge.getFrom());
            relation.setTargetLabel(edge.getTo());
            relation.setProperties(transformGraphProperty(edge.getProperties()));
            relation.setMultiple(edge.getMultiple());
            return relation;
        }).toList();
        graphSchema.setEntities(entities);
        graphSchema.setRelations(relations);


        List<GraphIndex> indexes = new ArrayList<>();
        nodes.forEach(node -> node.getProperties().forEach(p -> {
            if (p.getIndexed()) {
                GraphIndex index = new GraphIndex();
                index.setLabel(node.getLabel());
                index.setProperty(p.getCode());
                index.setType(IndexType.COMPOSITE.code());
                index.setSchemaType("vertex");
                index.setPropertyNames(Collections.singletonList(p.getCode()));
                index.setName(String.format("idx_%s_%s_%s", graph.getCode(), node.getLabel(), p.getCode()));
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
                index.setName(String.format("idx_%s_%s_%s", graph.getCode(), edge.getLabel(), p.getCode()));
                indexes.add(index);
            }
        }));

        graphSchema.setIndexes(indexes);
        return graphSchema;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void publishSchema(Long graphId) {
        log.info("发布图Schema: {}", graphId);
        //todo
        try {
            // 获取图信息
            Graph graph = graphService.getById(graphId);
            if (graph == null) {
                log.error("图不存在，graphId={}", graphId);
                return;
            }
            // 获取图数据库连接信息
            GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graph.getConnectionId());
                return;
            }
            List<GraphNodeDef> nodes = graphNodeDefService.getNodeDefsByGraphId(graphId, 0);
            List<GraphEdgeDef> edges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, 0);

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);

            // 创建图客户端
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphOperations graphOperations = graphClient.opsForGraph();


            GraphSchema graphSchema = getGraphSchema(graphId);
            // 应用图模式
            graphOperations.applySchema(graphConf, graphSchema);

            List<GraphPropertyDef> propertyList = new ArrayList<>();
            // 更新节点定义状态为已发布
            nodes.forEach(node -> {
                node.setStatus(1);
                propertyList.addAll(node.getProperties());
            });
            graphNodeDefService.updateBatchById(nodes);
            // 更新边定义状态为已发布
            edges.forEach(edge -> {
                edge.setStatus(1);
                propertyList.addAll(edge.getProperties());
            });
            graphEdgeDefService.updateBatchById(edges);
            // 更新属性定义状态为已发布
            graphPropertyDefService.updateBatchById(propertyList);
            // 更新图状态为已发布
            graph.setStatus(1);
            graphService.updateById(graph);
        } catch (Exception e) {
            log.error("发布图Schema失败", e);
            throw new GraphException("发布图Schema失败");
        }
    }

    public List<GraphProperty> transformGraphProperty(List<GraphPropertyDef> properties) {
        return properties.stream().map(prop -> {
            GraphProperty property = new GraphProperty();
            property.setName(prop.getName());
            property.setCode(prop.getCode());
            property.setDataType(DataType.instanceOf(prop.getType()));
            return property;
        }).collect(Collectors.toList());
    }

    @Override
    public SchemaExportDTO exportSchema(Long graphId) {
        Graph graph = graphService.getById(graphId);
        if (graph == null) {
            throw new GraphException("图不存在");
        }
        List<GraphNodeDef> nodes = graphNodeDefService.getNodeDefsByGraphId(graphId, null);
        List<GraphEdgeDef> edges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);

        SchemaExportDTO dto = new SchemaExportDTO();
        dto.setVersion("1.0");
        dto.setExportedAt(LocalDateTime.now().toString());
        dto.setGraphId(graphId);
        dto.setGraphCode(graph.getCode());
        dto.setNodes(nodes);
        dto.setEdges(edges);
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importSchema(Long graphId, SchemaImportDTO importDTO) {
        if (importDTO == null) {
            throw new GraphException("导入数据为空");
        }

        List<GraphNodeDef> importNodes = importDTO.getNodes();
        List<GraphEdgeDef> importEdges = importDTO.getEdges();
        if ((importNodes == null || importNodes.isEmpty()) && (importEdges == null || importEdges.isEmpty())) {
            throw new GraphException("导入数据为空");
        }

        String mode = importDTO.getMode();
        boolean replace = "replace".equalsIgnoreCase(mode);

        // 替换模式：先删除现有的节点和边定义
        if (replace) {
            List<GraphNodeDef> existingNodes = graphNodeDefService.getNodeDefsByGraphId(graphId, null);
            for (GraphNodeDef node : existingNodes) {
                graphNodeDefService.deleteNodeDefWithProperties(node.getId());
            }
            List<GraphEdgeDef> existingEdges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);
            for (GraphEdgeDef edge : existingEdges) {
                graphEdgeDefService.deleteEdgeDefWithProperties(edge.getId());
            }
        }

        // 构建现有节点的label->id映射，用于边定义中的from/to引用
        Map<String, Long> existingNodeLabelIdMap;
        if (replace) {
            existingNodeLabelIdMap = Map.of();
        } else {
            List<GraphNodeDef> existingNodes = graphNodeDefService.getNodeDefsByGraphId(graphId, null);
            existingNodeLabelIdMap = existingNodes.stream()
                .filter(n -> n.getLabel() != null)
                .collect(Collectors.toMap(GraphNodeDef::getLabel, GraphNodeDef::getId, (a, b) -> a));
        }

        // 导入节点定义
        if (importNodes != null) {
            for (GraphNodeDef node : importNodes) {
                // 合并模式：检查是否已存在（按label去重）
                if (!replace && existingNodeLabelIdMap.containsKey(node.getLabel())) {
                    continue;
                }
                node.setGraphId(graphId);
                node.setId(null);
                node.setStatus(0);
                // 保存节点定义及其属性
                graphNodeDefService.saveNodeDefWithProperties(node);
            }
        }

        // 重新获取最新的节点定义映射（包含刚导入的）
        Map<String, Long> allNodeLabelIdMap = graphNodeDefService.getNodeDefsByGraphId(graphId, null).stream()
            .filter(n -> n.getLabel() != null)
            .collect(Collectors.toMap(GraphNodeDef::getLabel, GraphNodeDef::getId, (a, b) -> a));

        // 导入边定义
        if (importEdges != null) {
            for (GraphEdgeDef edge : importEdges) {
                // 合并模式：检查是否已存在（按label去重）
                if (!replace) {
                    List<GraphEdgeDef> existingEdges = graphEdgeDefService.getEdgeDefsByGraphId(graphId, null);
                    boolean exists = existingEdges.stream()
                        .anyMatch(e -> Objects.equals(e.getLabel(), edge.getLabel()));
                    if (exists) {
                        continue;
                    }
                }
                edge.setGraphId(graphId);
                edge.setId(null);
                edge.setStatus(0);
                // 处理from/to引用：如果from/to是label字符串，则解析为id
                if (edge.getFrom() != null) {
                    Long fromId = allNodeLabelIdMap.get(edge.getFrom());
                    if (fromId != null) {
                        edge.setFrom(String.valueOf(fromId));
                    }
                }
                if (edge.getTo() != null) {
                    Long toId = allNodeLabelIdMap.get(edge.getTo());
                    if (toId != null) {
                        edge.setTo(String.valueOf(toId));
                    }
                }
                // 保存边定义及其属性
                graphEdgeDefService.saveEdgeDefWithProperties(edge);
            }
        }

        log.info("Schema导入完成，graphId={}, mode={}, nodes={}, edges={}",
            graphId, mode,
            importNodes != null ? importNodes.size() : 0,
            importEdges != null ? importEdges.size() : 0);
    }
}
