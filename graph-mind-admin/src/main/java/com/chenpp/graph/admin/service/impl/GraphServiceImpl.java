package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphDao;
import com.chenpp.graph.admin.mapper.GraphEdgeDefDao;
import com.chenpp.graph.admin.mapper.GraphVertexDefDao;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.BusinessException;
import com.chenpp.graph.core.model.GraphConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 图管理服务实现类
 *
 * @author April.Chen
 * @date 2025/8/1 17:00
 */
@Slf4j
@Service
public class GraphServiceImpl extends ServiceImpl<GraphDao, Graph> implements GraphService {

    @Autowired
    private GraphVertexDefService vertexDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphPropertyDefService propertyDefService;

    @Autowired
    private GraphConnectionService connectionService;

    @Autowired
    private GraphVertexDefDao graphVertexDefDao;

    @Autowired
    private GraphEdgeDefDao graphEdgeDefDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 根据连接状态批量标记图状态（避免 N+1 远程连接检查）
     */
    private void fillGraphStatus(List<Graph> graphs) {
        if (graphs == null || graphs.isEmpty()) {
            return;
        }

        // 收集所有不重复的 connectionId
        Set<Long> connIds = graphs.stream()
                .map(Graph::getConnectionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (connIds.isEmpty()) {
            return;
        }

        // 批量加载连接信息（单次查询）
        Map<Long, GraphConnection> connMap = connectionService.listByIds(connIds).stream()
                .collect(Collectors.toMap(GraphConnection::getId, c -> c, (a, b) -> a));

        // 根据连接状态标记图状态
        for (Graph graph : graphs) {
            if (graph.getConnectionId() == null) {
                continue;
            }
            GraphConnection conn = connMap.get(graph.getConnectionId());
            // 连接不存在或状态异常 → 图状态异常
            graph.setStatus(conn != null && conn.getStatus() != null && conn.getStatus() == 1 ? 0 : 1);
        }
    }

    private void fillGraphCounts(List<Graph> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 按 graph_id 分组统计本地记录（不限制 status，统计所有定义）
        List<Long> graphIds = records.stream().map(Graph::getId).collect(Collectors.toList());
        List<GraphVertexDef> allNodes = graphVertexDefDao.selectList(
                new QueryWrapper<GraphVertexDef>().in("graph_id", graphIds).select("graph_id"));
        Map<Long, Long> nodeCountMap = allNodes.stream()
                .collect(Collectors.groupingBy(GraphVertexDef::getGraphId, Collectors.counting()));
        List<GraphEdgeDef> allEdges = graphEdgeDefDao.selectList(
                new QueryWrapper<GraphEdgeDef>().in("graph_id", graphIds).select("graph_id"));
        Map<Long, Long> edgeCountMap = allEdges.stream()
                .collect(Collectors.groupingBy(GraphEdgeDef::getGraphId, Collectors.counting()));
        for (Graph graph : records) {
            graph.setVertexTypeCount(nodeCountMap.getOrDefault(graph.getId(), 0L).intValue());
            graph.setEdgeTypeCount(edgeCountMap.getOrDefault(graph.getId(), 0L).intValue());
        }
    }

    @Override
    public Page<Graph> queryGraphs(Page<Graph> page, String keyword) {
        QueryWrapper<Graph> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword).or().like("code", keyword);
        }
        queryWrapper.orderByDesc("create_time");
        Page<Graph> pageResult = this.page(page, queryWrapper);
        fillGraphCounts(pageResult.getRecords());
        fillGraphStatus(pageResult.getRecords());
        return pageResult;
    }

    @Override
    public Page<Graph> queryGraphsByConnectionId(Long connectionId, Page<Graph> page) {
        QueryWrapper<Graph> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("connection_id", connectionId);
        queryWrapper.orderByDesc("create_time");
        Page<Graph> pageResult = this.page(page, queryWrapper);

        // 标记本地图为平台创建
        for (Graph g : pageResult.getRecords()) {
            g.setSourceType("PLATFORM");
        }
        fillGraphCounts(pageResult.getRecords());

        // 从图数据库发现已有图
        List<Graph> discovered = discoverRemoteGraphs(connectionId);
        if (!discovered.isEmpty()) {
            Set<String> localCodes = pageResult.getRecords().stream().map(Graph::getCode).collect(Collectors.toSet());
            List<Graph> merged = new ArrayList<>(pageResult.getRecords());
            for (Graph remote : discovered) {
                if (!localCodes.contains(remote.getCode())) {
                    merged.add(remote);
                }
            }
            Page<Graph> resultPage = new Page<>(page.getCurrent(), page.getSize(), merged.size());
            int ps = (int) page.getSize();
            int from = (int) ((page.getCurrent() - 1) * ps);
            int to = Math.min(from + ps, merged.size());
            resultPage.setRecords(from < merged.size() ? merged.subList(from, to) : List.of());
            fillGraphStatus(resultPage.getRecords());
            return resultPage;
        }

        fillGraphStatus(pageResult.getRecords());
        return pageResult;
    }

    /**
     * 从图数据库发现已有的图，并获取节点/边类型数量
     */
    private List<Graph> discoverRemoteGraphs(Long connectionId) {
        List<Graph> result = new ArrayList<>();
        try {
            GraphConnection connection = connectionService.getById(connectionId);
            if (connection == null) {
                return result;
            }

            Graph graph = new Graph();
            graph.setConnectionId(connectionId);
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);

            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                GraphOperations graphOperations = graphClient.opsForGraph();
                List<com.chenpp.graph.core.schema.Graph> remoteGraphs = graphOperations.listGraphs(graphConf);

                AtomicLong idCounter = new AtomicLong(-1);
                for (com.chenpp.graph.core.schema.Graph rg : remoteGraphs) {
                    Graph g = new Graph();
                    Long negId = idCounter.decrementAndGet();
                    g.setId(negId);
                    g.setCode(rg.getCode());
                    g.setName(rg.getName());
                    g.setConnectionId(connectionId);
                    g.setGraphType(connection.getGraphType());
                    g.setSourceType("EXISTING");
                    g.setStatus(0);

                    // 尝试从远程图数据库获取节点/边类型数量（复用已打开的客户端）
                    try {
                        // 构造该远程图的 GraphConf，指定 graphCode
                        Graph remoteGraph = new Graph();
                        remoteGraph.setCode(rg.getCode());
                        GraphConf schemaConf = GraphClientFactory.createGraphConf(connection, remoteGraph);
                        com.chenpp.graph.core.schema.GraphSchema schema =
                                graphOperations.getPublishedSchema(schemaConf);
                        if (schema != null) {
                            g.setVertexTypeCount(schema.getEntities() != null ? schema.getEntities().size() : 0);
                            g.setEdgeTypeCount(schema.getRelations() != null ? schema.getRelations().size() : 0);
                        }
                    } catch (Exception e) {
                        log.debug("获取远程图类型数量失败，code={}: {}", rg.getCode(), e.getMessage());
                    }

                    result.add(g);
                }
            }
        } catch (Exception e) {
            log.warn("发现远程图失败，connectionId={}: {}", connectionId, e.getMessage());
        }
        return result;
    }

    @Override
    public boolean save(Graph graph) {
        GraphConnection connection = connectionService.getById(graph.getConnectionId());
        if (connection == null) {
            throw new BusinessException("图数据库连接不存在");
        }
        graph.setGraphType(connection.getGraphType());
        return super.save(graph);
    }

    @Override
    public boolean removeGraph(Long graphId, Long connectionId, String graphCode) {
        Graph graph = null;
        GraphConnection connection = null;

        // 1) 优先通过本地图记录获取图标识和连接
        if (graphId != null && graphId > 0) {
            graph = this.getById(graphId);
            if (graph != null) {
                graphCode = graph.getCode();
                connection = connectionService.getById(graph.getConnectionId());
            }
        }

        // 2) 如果没有本地记录，用传入的 connectionId 构造连接
        if (graph == null && connectionId != null) {
            connection = connectionService.getById(connectionId);
        }

        // 3) 删除图数据库中的远程图（非事务，先执行 fail-fast）
        if (connection != null && graphCode != null && !graphCode.isEmpty()) {
            Graph tempGraph = new Graph();
            tempGraph.setCode(graphCode);
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, tempGraph);
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphOperations graphOperations = graphClient.opsForGraph();
            graphOperations.dropGraph(graphConf);
            log.info("已删除图数据库中的图，code={}, connectionId={}", graphCode, connection.getId());
        } else {
            log.warn("缺少图数据库连接或图标识，跳过删除远程图，graphId={}", graphId);
        }

        // 4) 删除本地元数据（事务内）
        if (graph != null && graph.getId() != null && graph.getId() > 0) {
            Long gId = graph.getId();
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                vertexDefService.remove(new QueryWrapper<GraphVertexDef>().eq("graph_id", gId));
                edgeDefService.remove(new QueryWrapper<GraphEdgeDef>().eq("graph_id", gId));
                propertyDefService.remove(new QueryWrapper<GraphPropertyDef>().eq("graph_id", gId));
                boolean result = removeById(gId);
                log.info("已删除本地图元数据，graphId={}, result={}", gId, result);
                return result;
            }));
        }

        return true;
    }
}