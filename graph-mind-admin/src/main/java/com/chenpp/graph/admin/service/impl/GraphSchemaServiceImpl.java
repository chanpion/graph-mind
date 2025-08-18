package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
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
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
}
