package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphEdgeDefDao;
import com.chenpp.graph.admin.mapper.GraphEdgePropertyDao;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphEdgeProperty;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private GraphEdgePropertyDao edgePropertyDao;
    
    @Override
    public List<GraphEdgeDef> getEdgeDefsByGraphId(Long graphId) {
        QueryWrapper<GraphEdgeDef> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("graph_id", graphId);
        List<GraphEdgeDef> edgeDefs = this.list(queryWrapper);
        
        // 查询并设置每个边定义的属性列表
        for (GraphEdgeDef edgeDef : edgeDefs) {
            QueryWrapper<GraphEdgeProperty> propertyQueryWrapper = new QueryWrapper<>();
            propertyQueryWrapper.eq("edge_def_id", edgeDef.getId());
            edgeDef.setProperties(edgePropertyDao.selectList(propertyQueryWrapper));
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
            for (GraphEdgeProperty property : edgeDef.getProperties()) {
                property.setEdgeDefId(edgeDef.getId());
                edgePropertyDao.insert(property);
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
            QueryWrapper<GraphEdgeProperty> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("edge_def_id", edgeDef.getId());
            edgePropertyDao.delete(deleteWrapper);
            
            // 重新保存边属性
            if (edgeDef.getProperties() != null) {
                for (GraphEdgeProperty property : edgeDef.getProperties()) {
                    property.setEdgeDefId(edgeDef.getId());
                    edgePropertyDao.insert(property);
                }
            }
        }
        
        return updated;
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteEdgeDefWithProperties(Long id) {
        // 删除边属性
        QueryWrapper<GraphEdgeProperty> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("edge_def_id", id);
        edgePropertyDao.delete(deleteWrapper);
        
        // 删除边定义
        return this.removeById(id);
    }
    
    @Override
    public boolean importEdgeData(Long graphId, Long edgeTypeId, MultipartFile file, String headers, String mapping, String data) {
        // TODO: 实现边数据导入逻辑
        // 这里应该解析CSV文件，根据映射关系将数据导入到图数据库中
        // 可以使用graphId获取图数据库连接信息
        // 可以使用edgeTypeId获取边类型定义信息
        return true;
    }
}