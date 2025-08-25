package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphEdgeDefDao;
import com.chenpp.graph.admin.mapper.GraphPropertyDefDao;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图边定义服务实现类
 *
 * @author April.Chen
 * @date 2025/8/4 16:00
 */
@Service
public class GraphEdgeDefServiceImpl extends ServiceImpl<GraphEdgeDefDao, GraphEdgeDef> implements GraphEdgeDefService {
    
    @Autowired
    private GraphPropertyDefDao propertyDao;
    
    @Override
    public List<GraphEdgeDef> getEdgeDefsByGraphId(Long graphId, Integer status) {
        QueryWrapper<GraphEdgeDef> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("graph_id", graphId);
        if (status != null){
            queryWrapper.eq("status", status);
        }
        List<GraphEdgeDef> edgeDefs = this.list(queryWrapper);
        
        // 查询并设置每个边定义的属性列表
        for (GraphEdgeDef edgeDef : edgeDefs) {
            QueryWrapper<GraphPropertyDef> propertyQueryWrapper = new QueryWrapper<>();
            propertyQueryWrapper.eq("entity_id", edgeDef.getId());
            propertyQueryWrapper.eq("property_type", "edge");
            if (status != null){
                propertyQueryWrapper.eq("status", status);
            }
            edgeDef.setProperties(propertyDao.selectList(propertyQueryWrapper));
        }
        
        return edgeDefs;
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveEdgeDefWithProperties(GraphEdgeDef edgeDef) {
        // 设置创建和更新时间
        edgeDef.setCreateTime(LocalDateTime.now());
        edgeDef.setUpdateTime(LocalDateTime.now());
        
        // 保存边定义
        boolean saved = this.save(edgeDef);
        
        if (saved && edgeDef.getProperties() != null) {
            // 保存边属性
            for (GraphPropertyDef property : edgeDef.getProperties()) {
                property.setEntityId(edgeDef.getId());
                property.setPropertyType("edge");
                property.setGraphId(edgeDef.getGraphId());
                propertyDao.insert(property);
            }
        }
        
        return saved;
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateEdgeDefWithProperties(GraphEdgeDef edgeDef) {
        // 设置更新时间
        edgeDef.setUpdateTime(LocalDateTime.now());
        
        // 更新边定义
        boolean updated = this.updateById(edgeDef);
        
        if (updated) {
            // 删除原有的属性
            QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("entity_id", edgeDef.getId());
            deleteWrapper.eq("property_type", "edge");
            propertyDao.delete(deleteWrapper);
            
            // 重新保存边属性
            if (edgeDef.getProperties() != null) {
                for (GraphPropertyDef property : edgeDef.getProperties()) {
                    property.setEntityId(edgeDef.getId());
                    property.setPropertyType("edge");
                    property.setGraphId(edgeDef.getGraphId());
                    propertyDao.insert(property);
                }
            }
        }
        
        return updated;
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteEdgeDefWithProperties(Long id) {
        // 删除边属性
        QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("entity_id", id);
        deleteWrapper.eq("property_type", "edge");
        propertyDao.delete(deleteWrapper);
        
        // 删除边定义
        return this.removeById(id);
    }
}