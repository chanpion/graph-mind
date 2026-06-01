package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphConnectionDao;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.model.GraphConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 图数据库连接服务实现类
 *
 * @author April.Chen
 * @date 2025/8/1 16:30
 */
@Slf4j
@Service
public class GraphConnectionServiceImpl extends ServiceImpl<GraphConnectionDao, GraphConnection> implements GraphConnectionService {

    @Override
    public Page<GraphConnection> queryConnections(Page<GraphConnection> page, String keyword, String type) {
        QueryWrapper<GraphConnection> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword).or().like("host", keyword);
        }
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("graph_type", type);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    @Override
    public Page<GraphConnection> queryConnections(Page<GraphConnection> page, String keyword) {
        return queryConnections(page, keyword, null);
    }

    @Override
    public boolean testConnection(Long id) {
        GraphConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }
        try {
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, new Graph());
            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                boolean result = graphClient.checkConnection();
                connection.setStatus(result ? 1 : 2);
                return result;
            }
        } catch (Exception e) {
            log.error("连接测试异常", e);
            connection.setStatus(2);
            return false;
        } finally {
            this.updateById(connection);
        }
    }

    @Override
    public boolean connectDatabase(Long id) {
        GraphConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }

        // 模拟连接数据库逻辑
        connection.setStatus(1);
        connection.setUpdateTime(LocalDateTime.now());
        return this.updateById(connection);
    }

    @Override
    public boolean disconnectDatabase(Long id) {
        GraphConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }

        // 模拟断开数据库连接逻辑
        connection.setStatus(0);
        connection.setUpdateTime(LocalDateTime.now());
        return this.updateById(connection);
    }

    @Override
    public boolean save(GraphConnection entity) {
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setStatus(0);
        return super.save(entity);
    }
}