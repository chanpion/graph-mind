package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphDao;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.service.GraphService;
import org.springframework.stereotype.Service;

/**
 * 图管理服务实现类
 *
 * @author April.Chen
 * @date 2025/8/1 17:00
 */
@Service
public class GraphServiceImpl extends ServiceImpl<GraphDao, Graph> implements GraphService {
    
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
}