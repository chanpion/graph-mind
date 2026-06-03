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
     * 删除图 — 支持删除图数据库中已有的图（无本地记录）
     *
     * @param graphId      图ID（本地图记录主键；仅删除远程图时可传 null 或负数）
     * @param connectionId 图数据库连接ID（删除远程图时必传）
     * @param graphCode    图标识（删除远程图时必传，例如 Nebula space 名）
     * @return 是否成功
     */
    boolean removeGraph(Long graphId, Long connectionId, String graphCode);
}