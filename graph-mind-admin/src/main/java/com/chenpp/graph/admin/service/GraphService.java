package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.Graph;

/**
 * 图管理服务接口
 *
 * @author April.Chen
 * @date 2025/8/1 17:00
 */
public interface GraphService extends IService<Graph> {

    /**
     * 分页查询图列表
     *
     * @param page    分页对象
     * @param keyword 搜索关键词
     * @return 图列表
     */
    Page<Graph> queryGraphs(Page<Graph> page, String keyword);

    /**
     * 根据连接ID查询图列表
     *
     * @param connectionId 连接ID
     * @param page         分页对象
     * @return 图列表
     */
    Page<Graph> queryGraphsByConnectionId(Long connectionId, Page<Graph> page);

    /**
     * 删除图
     *
     * @param graphId 图ID
     * @return 是否成功
     */
    boolean removeGraph(Long graphId);
}