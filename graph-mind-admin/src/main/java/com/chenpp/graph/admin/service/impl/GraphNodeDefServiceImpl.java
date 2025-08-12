package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphNodeDefDao;
import com.chenpp.graph.admin.mapper.GraphPropertyDao;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.service.GraphNodeDefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图节点定义服务实现类
 *
 * @author April.Chen
 * @date 2025/8/4 15:50
 */
@Service
public class GraphNodeDefServiceImpl extends ServiceImpl<GraphNodeDefDao, GraphNodeDef> implements GraphNodeDefService {

    @Autowired
    private GraphPropertyDao propertyDao;

    @Override
    public List<GraphNodeDef> getNodeDefsByGraphId(Long graphId) {
        QueryWrapper<GraphNodeDef> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("graph_id", graphId);
        List<GraphNodeDef> nodeDefs = this.list(queryWrapper);

        // 查询并设置每个节点定义的属性列表
        for (GraphNodeDef nodeDef : nodeDefs) {
            QueryWrapper<GraphPropertyDef> propertyQueryWrapper = new QueryWrapper<>();
            propertyQueryWrapper.eq("entity_id", nodeDef.getId());
            propertyQueryWrapper.eq("property_type", "node");
            nodeDef.setProperties(propertyDao.selectList(propertyQueryWrapper));
        }

        return nodeDefs;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveNodeDefWithProperties(GraphNodeDef nodeDef) {
        // 设置创建和更新时间
        nodeDef.setCreateTime(LocalDateTime.now());
        nodeDef.setUpdateTime(LocalDateTime.now());

        // 保存节点定义
        boolean saved = this.save(nodeDef);

        if (saved && nodeDef.getProperties() != null) {
            // 保存节点属性
            for (GraphPropertyDef property : nodeDef.getProperties()) {
                property.setEntityId(nodeDef.getId());
                property.setPropertyType("node");
                propertyDao.insert(property);
            }
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateNodeDefWithProperties(GraphNodeDef nodeDef) {
        // 设置更新时间
        nodeDef.setUpdateTime(LocalDateTime.now());

        // 更新节点定义
        boolean updated = this.updateById(nodeDef);

        if (updated) {
            // 删除原有的属性
            QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("entity_id", nodeDef.getId());
            deleteWrapper.eq("property_type", "node");
            propertyDao.delete(deleteWrapper);

            // 重新保存节点属性
            if (nodeDef.getProperties() != null) {
                for (GraphPropertyDef property : nodeDef.getProperties()) {
                    property.setEntityId(nodeDef.getId());
                    property.setPropertyType("node");
                    propertyDao.insert(property);
                }
            }
        }

        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteNodeDefWithProperties(Long id) {
        // 删除节点属性
        QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("entity_id", id);
        deleteWrapper.eq("property_type", "node");
        propertyDao.delete(deleteWrapper);

        // 删除节点定义
        return this.removeById(id);
    }
}