package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.GraphVertexDefDao;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.core.constant.GraphConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图节点定义服务实现类
 *
 * @author April.Chen
 * @date 2025/8/4 15:50
 */
@Service
public class GraphVertexDefServiceImpl extends ServiceImpl<GraphVertexDefDao, GraphVertexDef> implements GraphVertexDefService {


    @Resource
    private GraphPropertyDefService graphPropertyDefService;

    @Override
    public List<GraphVertexDef> getVertexDefsByGraphId(Long graphId, Integer status) {
        QueryWrapper<GraphVertexDef> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("graph_id", graphId);
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        List<GraphVertexDef> vertexDefs = this.list(queryWrapper);

        if (!vertexDefs.isEmpty()) {
            List<Long> vertexDefIds = vertexDefs.stream().map(GraphVertexDef::getId).collect(Collectors.toList());
            QueryWrapper<GraphPropertyDef> propertyQueryWrapper = new QueryWrapper<>();
            propertyQueryWrapper.in("entity_id", vertexDefIds);
            propertyQueryWrapper.eq("property_type", GraphConstants.VERTEX);
            if (status != null) {
                propertyQueryWrapper.eq("status", status);
            }
            List<GraphPropertyDef> allProperties = graphPropertyDefService.list(propertyQueryWrapper);
            Map<Long, List<GraphPropertyDef>> propertyMap = allProperties.stream()
                    .collect(Collectors.groupingBy(GraphPropertyDef::getEntityId));
            for (GraphVertexDef vertexDef : vertexDefs) {
                vertexDef.setProperties(propertyMap.getOrDefault(vertexDef.getId(), List.of()));
            }
        }

        return vertexDefs;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveVertexDefWithProperties(GraphVertexDef vertexDef) {
        // 保存节点定义
        boolean saved = this.save(vertexDef);

        if (saved && vertexDef.getProperties() != null) {
            // 保存节点属性
            for (GraphPropertyDef property : vertexDef.getProperties()) {
                property.setGraphId(vertexDef.getGraphId());
                property.setEntityId(vertexDef.getId());
                property.setPropertyType(GraphConstants.VERTEX);
                if (property.getCode() == null || property.getCode().isEmpty()) {
                    property.setCode(property.getName());
                }
                graphPropertyDefService.saveOrUpdate(property);
            }
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateVertexDefWithProperties(GraphVertexDef vertexDef) {
        boolean updated = false;

        // 先尝试更新，如果记录不存在则插入
        if (vertexDef.getId() != null && this.getById(vertexDef.getId()) != null) {
            updated = this.updateById(vertexDef);
        } else {
            // 记录不存在，执行插入（新增顶点定义时也走此逻辑）
            updated = this.save(vertexDef);
        }

        if (updated) {
            // 删除原有的属性
            QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("entity_id", vertexDef.getId());
            deleteWrapper.eq("property_type", GraphConstants.VERTEX);
            graphPropertyDefService.remove(deleteWrapper);

            // 重新保存节点属性
            if (vertexDef.getProperties() != null) {
                for (GraphPropertyDef property : vertexDef.getProperties()) {
                    property.setGraphId(vertexDef.getGraphId());
                    property.setEntityId(vertexDef.getId());
                    property.setPropertyType(GraphConstants.VERTEX);
                    if (property.getCode() == null || property.getCode().isEmpty()) {
                        property.setCode(property.getName());
                    }
                    graphPropertyDefService.saveOrUpdate(property);
                }
            }
        }

        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteVertexDefWithProperties(Long id) {
        QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("entity_id", id);
        deleteWrapper.eq("property_type", GraphConstants.VERTEX);
        graphPropertyDefService.remove(deleteWrapper);

        return this.removeById(id);
    }
}