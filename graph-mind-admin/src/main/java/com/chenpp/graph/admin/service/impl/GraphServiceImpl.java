package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphDao;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphNodeDefService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.BusinessException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.janus.JanusClient;
import com.chenpp.graph.neo4j.Neo4jClient;
import com.chenpp.graph.nebula.NebulaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private GraphNodeDefService nodeDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphDatabaseConnectionService connectionService;

    @Override
    public Page<Graph> queryGraphs(Page<Graph> page, String keyword) {
        QueryWrapper<Graph> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword).or().like("code", keyword);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    @Override
    public Page<Graph> queryGraphsByConnectionId(Long connectionId, Page<Graph> page) {
        QueryWrapper<Graph> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("connection_id", connectionId);
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
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
            GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
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
                    closeGraphClient(graphClient);
                } catch (Exception e) {
                    log.warn("删除图数据库中的数据和schema失败，graphId={}, error={}", graphId, e.getMessage());
                    // 即使删除图数据库中的数据失败，也继续删除本地数据
                }
            }

            // 删除图关联的节点定义
            nodeDefService.remove(new QueryWrapper<GraphNodeDef>().eq("graph_id", graphId));

            // 删除图关联的边定义
            edgeDefService.remove(new QueryWrapper<GraphEdgeDef>().eq("graph_id", graphId));

            // 删除关联的属性定义


            // 删除图本身
            return this.removeById(graphId);
        } catch (Exception e) {
            log.error("删除图失败，graphId={}", graphId, e);
            throw new BusinessException("删除图失败", e);
        }
    }

    /**
     * 关闭图客户端连接
     *
     * @param graphClient 图客户端
     */
    private void closeGraphClient(GraphClient graphClient) {
        try {
            if (graphClient instanceof JanusClient) {
                ((JanusClient) graphClient).close();
            } else if (graphClient instanceof Neo4jClient) {
                // Neo4jClient没有close方法，不需要特殊处理
            } else if (graphClient instanceof NebulaClient) {
                // NebulaClient没有close方法，不需要特殊处理
            }
        } catch (Exception e) {
            log.warn("关闭图客户端连接失败: {}", e.getMessage());
        }
    }
}