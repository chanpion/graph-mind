package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphConnectionDao;
import com.chenpp.graph.admin.enums.ConnectionStatusEnum;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.model.GraphConf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like("name", keyword).or().like("hosts", keyword);
        }
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq("graph_type", type);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }


    @Override
    public boolean testConnection(Long id) {
        GraphConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }
        try {
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, connection.getGraphType());
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            boolean result = graphClient.checkConnection();
            connection.setStatus(result ? ConnectionStatusEnum.CONNECTED.getCode() : ConnectionStatusEnum.FAILED.getCode());
            return result;
        } catch (Exception e) {
            log.error("连接测试异常", e);
            connection.setStatus(ConnectionStatusEnum.FAILED.getCode());
            return false;
        } finally {
            this.updateById(connection);
        }
    }


    @Override
    public boolean save(GraphConnection entity) {
        entity.setStatus(ConnectionStatusEnum.UNCHECKED.getCode());
        return super.save(entity);
    }
}