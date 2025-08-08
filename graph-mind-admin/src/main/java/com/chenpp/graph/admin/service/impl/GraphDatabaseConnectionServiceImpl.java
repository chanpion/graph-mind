package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphDatabaseConnectionDao;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 图数据库连接服务实现类
 *
 * @author April.Chen
 * @date 2025/8/1 16:30
 */
@Service
public class GraphDatabaseConnectionServiceImpl extends ServiceImpl<GraphDatabaseConnectionDao, GraphDatabaseConnection> implements GraphDatabaseConnectionService {
    
    @Override
    public Page<GraphDatabaseConnection> queryConnections(Page<GraphDatabaseConnection> page, String keyword, String type) {
        QueryWrapper<GraphDatabaseConnection> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword).or().like("host", keyword);
        }
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public Page<GraphDatabaseConnection> queryConnections(Page<GraphDatabaseConnection> page, String keyword) {
        return queryConnections(page, keyword, null);
    }
    
    @Override
    public boolean testConnection(Long id) {
        // 模拟测试连接逻辑
        GraphDatabaseConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }
        
        // 这里应该实际测试数据库连接
        // 暂时返回true表示测试成功
        return true;
    }
    
    @Override
    public boolean connectDatabase(Long id) {
        GraphDatabaseConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }
        
        // 模拟连接数据库逻辑
        connection.setStatus(1); // 设置为已连接状态
        connection.setUpdateTime(LocalDateTime.now());
        return this.updateById(connection);
    }
    
    @Override
    public boolean disconnectDatabase(Long id) {
        GraphDatabaseConnection connection = this.getById(id);
        if (connection == null) {
            return false;
        }
        
        // 模拟断开数据库连接逻辑
        connection.setStatus(0); // 设置为已断开状态
        connection.setUpdateTime(LocalDateTime.now());
        return this.updateById(connection);
    }
}