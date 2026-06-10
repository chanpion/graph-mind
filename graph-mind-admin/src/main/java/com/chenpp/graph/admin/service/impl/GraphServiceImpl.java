package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.enums.GraphTypeEnum;
import com.chenpp.graph.admin.mapper.GraphDao;
import com.chenpp.graph.admin.mapper.GraphEdgeDefDao;
import com.chenpp.graph.admin.mapper.GraphVertexDefDao;
import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.BusinessException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class GraphServiceImpl extends ServiceImpl<GraphDao, GraphInfo> implements GraphService {

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

    private void fillGraphStatus(List<GraphInfo> graphInfos) {
        if (graphInfos == null || graphInfos.isEmpty()) {
            return;
        }

        Set<Long> connIds = graphInfos.stream().map(GraphInfo::getConnectionId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(connIds)) {
            return;
        }

        Map<Long, GraphConnection> connMap = connectionService.listByIds(connIds).stream()
                .collect(Collectors.toMap(GraphConnection::getId, c -> c, (a, b) -> a));

        for (GraphInfo graphInfo : graphInfos) {
            GraphConnection conn = connMap.get(graphInfo.getConnectionId());
            graphInfo.setStatus(conn != null && conn.getStatus() != null && conn.getStatus() == 1 ? 0 : 1);
        }
    }

    private void fillGraphCounts(List<GraphInfo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> graphIds = records.stream().map(GraphInfo::getId).collect(Collectors.toList());
        List<GraphVertexDef> allNodes = graphVertexDefDao.selectList(
                new QueryWrapper<GraphVertexDef>().in("graph_id", graphIds).select("graph_id"));
        Map<Long, Long> nodeCountMap = allNodes.stream()
                .collect(Collectors.groupingBy(GraphVertexDef::getGraphId, Collectors.counting()));
        List<GraphEdgeDef> allEdges = graphEdgeDefDao.selectList(
                new QueryWrapper<GraphEdgeDef>().in("graph_id", graphIds).select("graph_id"));
        Map<Long, Long> edgeCountMap = allEdges.stream()
                .collect(Collectors.groupingBy(GraphEdgeDef::getGraphId, Collectors.counting()));
        for (GraphInfo graphInfo : records) {
            graphInfo.setVertexTypeCount(nodeCountMap.getOrDefault(graphInfo.getId(), 0L).intValue());
            graphInfo.setEdgeTypeCount(edgeCountMap.getOrDefault(graphInfo.getId(), 0L).intValue());
        }
    }

    @Override
    public Page<GraphInfo> queryGraphs(Page<GraphInfo> page, String keyword) {
        QueryWrapper<GraphInfo> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword).or().like("code", keyword);
        }
        queryWrapper.orderByDesc("create_time");
        Page<GraphInfo> pageResult = this.page(page, queryWrapper);
        fillGraphCounts(pageResult.getRecords());
        fillGraphStatus(pageResult.getRecords());
        return pageResult;
    }

    @Override
    public Page<GraphInfo> queryGraphsByConnectionId(Long connectionId, Page<GraphInfo> page) {
        QueryWrapper<GraphInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("connection_id", connectionId);
        queryWrapper.orderByDesc("create_time");
        Page<GraphInfo> pageResult = this.page(page, queryWrapper);

        // 标记本地图为平台创建
        for (GraphInfo g : pageResult.getRecords()) {
            g.setSourceType("PLATFORM");
        }

        // 从图数据库发现已有图并获取所有图的实时类型计数
        List<GraphInfo> allGraphInfos = new ArrayList<>(pageResult.getRecords());
        List<GraphInfo> discovered = discoverRemoteGraphs(connectionId);
        Set<String> localCodes = pageResult.getRecords().stream().map(GraphInfo::getCode).collect(Collectors.toSet());
        for (GraphInfo remote : discovered) {
            if (!localCodes.contains(remote.getCode())) {
                allGraphInfos.add(remote);
            }
        }

        // 从图数据库获取所有图（含 PLATFORM）的实时节点/边类型计数
        fillCountsFromRemote(connectionId, allGraphInfos);

        Page<GraphInfo> resultPage = new Page<>(page.getCurrent(), page.getSize(), allGraphInfos.size());
        int ps = (int) page.getSize();
        int from = (int) ((page.getCurrent() - 1) * ps);
        int to = Math.min(from + ps, allGraphInfos.size());
        resultPage.setRecords(from < allGraphInfos.size() ? allGraphInfos.subList(from, to) : List.of());
        fillGraphStatus(resultPage.getRecords());
        return resultPage;
    }

    /**
     * 从图数据库发现已有的图，并获取节点/边类型数量
     */
    private List<GraphInfo> discoverRemoteGraphs(Long connectionId) {
        List<GraphInfo> result = new ArrayList<>();
        try {
            GraphConnection connection = connectionService.getById(connectionId);
            if (connection == null) {
                return result;
            }

            GraphInfo graphInfo = new GraphInfo();
            graphInfo.setConnectionId(connectionId);
            // graphCode 在此场景下仅作占位，实际图代码从远程列表获取
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, "placeholder");

            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                GraphOperations graphOperations = graphClient.opsForGraph();
                List<Graph> remoteGraphs = graphOperations.listGraphs(graphConf);

                AtomicLong idCounter = new AtomicLong(-1);
                for (Graph rg : remoteGraphs) {
                    GraphInfo g = new GraphInfo();
                    Long negId = idCounter.decrementAndGet();
                    g.setId(negId);
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
    public boolean save(GraphInfo graphInfo) {
        GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
        if (connection == null) {
            throw new BusinessException("图数据库连接不存在");
        }
        graphInfo.setStatus(0);
        graphInfo.setGraphType(connection.getGraphType());
        return super.save(graphInfo);
    }

    @Override
    public boolean removeGraph(Long graphId, Long connectionId, String graphCode) {
        GraphInfo graphInfo = null;
        GraphConnection connection = null;

        if (graphId != null && graphId > 0) {
            graphInfo = this.getById(graphId);
            if (graphInfo != null) {
                graphCode = graphInfo.getCode();
                connection = connectionService.getById(graphInfo.getConnectionId());
            }
        }

        if (graphInfo == null && connectionId != null) {
            connection = connectionService.getById(connectionId);
        }

        if (connection != null && graphCode != null && !graphCode.isEmpty()) {
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphCode);
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphOperations graphOperations = graphClient.opsForGraph();
            graphOperations.dropGraph(graphConf);
            log.info("已删除图数据库中的图，code={}, connectionId={}", graphCode, connection.getId());
        } else {
            log.debug("缺少图数据库连接或图标识，跳过删除远程图，graphId={}", graphId);
        }

        // 4) 删除本地元数据（事务内）
        if (graphInfo != null && graphInfo.getId() != null && graphInfo.getId() > 0) {
            Long gId = graphInfo.getId();
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

    /**
     * 从图数据库获取所有图的实时节点/边类型计数
     */
    private void fillCountsFromRemote(Long connectionId, List<GraphInfo> graphInfos) {
        GraphConnection connection = connectionService.getById(connectionId);
        if (connection == null || graphInfos == null || graphInfos.isEmpty()) {
            return;
        }

        // 保证 discovery 用的 GraphConf 实例不使用任何特定 graphCode，
        // 只携带连接信息即可 — 后面的 getPublishedSchema 用 graphConf 参数指定
        GraphInfo graphInfo = new GraphInfo();
        graphInfo.setConnectionId(connectionId);
        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, "");

        try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
            GraphOperations graphOperations = graphClient.opsForGraph();
            for (GraphInfo g : graphInfos) {
                if (g.getCode() == null) {
                    continue;
                }
                try {
                    GraphConf schemaConf = GraphClientFactory.createGraphConf(connection, g.getCode());
                    com.chenpp.graph.core.schema.GraphSchema schema = graphOperations.getPublishedSchema(schemaConf);
                    if (schema != null) {
                        g.setVertexTypeCount(schema.getEntities() != null ? schema.getEntities().size() : 0);
                        g.setEdgeTypeCount(schema.getRelations() != null ? schema.getRelations().size() : 0);
                    }
                } catch (Exception e) {
                    log.debug("获取图类型数量失败，code={}: {}", g.getCode(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("获取图类型数量失败，connectionId={}: {}", connectionId, e.getMessage());
        }
    }


}