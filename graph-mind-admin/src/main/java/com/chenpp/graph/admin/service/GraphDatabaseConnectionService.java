package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;

/**
 * 图数据库连接服务接口
 *
 * @author April.Chen
 * @date 2025/8/1 16:30
 */
public interface GraphDatabaseConnectionService extends IService<GraphDatabaseConnection> {
    
    /**
     * 分页查询连接列表
     * @param page 分页对象
     * @param keyword 搜索关键词
     * @return 连接列表
     */
    Page<GraphDatabaseConnection> queryConnections(Page<GraphDatabaseConnection> page, String keyword);
    
    /**
     * 测试连接
     * @param id 连接ID
     * @return 测试结果
     */
    boolean testConnection(Long id);
    
    /**
     * 连接数据库
     * @param id 连接ID
     * @return 是否连接成功
     */
    boolean connectDatabase(Long id);
    
    /**
     * 断开数据库连接
     * @param id 连接ID
     * @return 是否断开成功
     */
    boolean disconnectDatabase(Long id);
}