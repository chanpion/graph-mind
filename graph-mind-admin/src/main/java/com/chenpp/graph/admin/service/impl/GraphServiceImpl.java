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
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 批量填充图列表的节点类型数和边类型数（替代 N+1 循环查询）
     */
    /**
     * 批量检查图数据库连接状态，更新图状态
     */
    private void fillGraphStatus(List<Graph> graphs) {
        for (Graph graph : graphs) {
            if (graph.getConnectionId() == null) {
                continue;
            }
            try {
                GraphConnection connection = connectionService.getById(graph.getConnectionId());
                if (connection == null) {
                    continue;
                }
                Graph g = new Graph();
                g.setCode(graph.getCode());
                GraphConf graphConf = GraphClientFactory.createGraphConf(connection, g);
                try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                    boolean connected = graphClient.checkConnection();
                    graph.setStatus(connected ? 0 : 1);
                }
            } catch (Exception e) {
                log.warn("检查图状态失败: graphId={}, code={}", graph.getId(), graph.getCode());
                graph.setStatus(1);
            }
        }
    }

    private void fillGraphCounts(List<Graph> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> graphIds = records.stream().map(Graph::getId).collect(Collectors.toList());
        // 一次查询全部节点定义，按 graph_id 分组计数
        List<GraphVertexDef> allNodes = graphVertexDefDao.selectList(
                new QueryWrapper<GraphVertexDef>().in("graph_id", graphIds).eq("status", 1).select("graph_id"));
        Map<Long, Long> nodeCountMap = allNodes.stream()
                .collect(Collectors.groupingBy(GraphVertexDef::getGraphId, Collectors.counting()));
        // 一次查询全部边定义，按 graph_id 分组计数
        List<GraphEdgeDef> allEdges = graphEdgeDefDao.selectList(
                new QueryWrapper<GraphEdgeDef>().in("graph_id", graphIds).eq("status", 1).select("graph_id"));
        Map<Long, Long> edgeCountMap = allEdges.stream()
                .collect(Collectors.groupingBy(GraphEdgeDef::getGraphId, Collectors.counting()));
        // 填充统计信息
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
            Set<String> localCodes = pageResult.getRecords().stream()
                    .map(Graph::getCode).collect(Collectors.toSet());
            List<Graph> merged = new ArrayList<>(pageResult.getRecords());
            for (Graph remote : discovered) {
                if (!localCodes.contains(remote.getCode())) {
                    merged.add(remote);
                }
            }
            // 重建分页结果（扩大 total 以包含发现的图）
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
     * 从图数据库发现已有的图
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
                    g.setId(idCounter.decrementAndGet());
                    g.setCode(rg.getCode());
                    g.setName(rg.getName());
                    g.setConnectionId(connectionId);
                    g.setGraphType(connection.getGraphType());
                    g.setSourceType("EXISTING");
                    g.setStatus(0);
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeGraph(Long graphId) {
        try {
            // 获取图信息
            Graph graph = this.getById(graphId);
            if (graph == null) {
                log.warn("图不存在，graphId={}", graphId);
                return false;
            }

            // 获取图数据库连接信息
            GraphConnection connection = connectionService.getById(graph.getConnectionId());
            if (connection == null) {
                log.warn("图数据库连接不存在，connectionId={}", graph.getConnectionId());
                // 即使连接不存在，也继续删除本地数据
            } else {
                // 创建图客户端并删除图数据库中的数据和schema
                try {
                    GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);
                    GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
                    GraphOperations graphOperations = graphClient.opsForGraph();

                    // 删除图数据库中的图
                    graphOperations.dropGraph(graphConf);

                    // 关闭图客户端
                    graphClient.close();
                } catch (Exception e) {
                    log.warn("删除图数据库中的数据和schema失败，graphId={}, error={}", graphId, e.getMessage());
                    // 即使删除图数据库中的数据失败，也继续删除本地数据
                }
            }

            // 删除图关联的节点定义
            vertexDefService.remove(new QueryWrapper<GraphVertexDef>().eq("graph_id", graphId));

            // 删除图关联的边定义
            edgeDefService.remove(new QueryWrapper<GraphEdgeDef>().eq("graph_id", graphId));

            // 删除关联的属性定义
            propertyDefService.remove(new QueryWrapper<GraphPropertyDef>().eq("graph_id", graphId));

            // 删除图本身
            return this.removeById(graphId);
        } catch (Exception e) {
            log.error("删除图失败，graphId={}", graphId, e);
            throw new BusinessException("删除图失败", e);
        }
    }
}