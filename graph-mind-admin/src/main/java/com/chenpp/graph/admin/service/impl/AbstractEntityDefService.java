package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.model.GraphEntityDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图实体定义服务抽象基类，消除 VertexDef 和 EdgeDef ServiceImpl 的重复代码
 *
 * @author April.Chen
 * @date 2026/6/26
 */
public abstract class AbstractEntityDefService<M extends BaseMapper<T>, T extends GraphEntityDef>
        extends ServiceImpl<M, T> {

    @Autowired
    protected GraphPropertyDefService graphPropertyDefService;

    /**
     * 返回属性类型标识
     *
     * @return GraphConstants.VERTEX 或 GraphConstants.EDGE
     */
    protected abstract String getPropertyType();

    /**
     * 按图ID和状态查询实体定义，并填充属性列表
     */
    public List<T> getEntityDefsByGraphId(Long graphId, Integer status) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("graph_id", graphId);
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        List<T> entityDefs = this.list(queryWrapper);

        if (!entityDefs.isEmpty()) {
            List<Long> entityDefIds = entityDefs.stream().map(GraphEntityDef::getId).collect(Collectors.toList());
            QueryWrapper<GraphPropertyDef> propertyQueryWrapper = new QueryWrapper<>();
            propertyQueryWrapper.in("entity_id", entityDefIds);
            propertyQueryWrapper.eq("property_type", getPropertyType());
            if (status != null) {
                propertyQueryWrapper.eq("status", status);
            }
            List<GraphPropertyDef> allProperties = graphPropertyDefService.list(propertyQueryWrapper);
            Map<Long, List<GraphPropertyDef>> propertyMap = allProperties.stream()
                    .collect(Collectors.groupingBy(GraphPropertyDef::getEntityId));
            for (T entityDef : entityDefs) {
                entityDef.setProperties(propertyMap.getOrDefault(entityDef.getId(), List.of()));
            }
        }

        return entityDefs;
    }

    /**
     * 保存实体定义及其属性
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEntityDefWithProperties(T entity) {
        boolean saved = this.save(entity);

        if (saved && entity.getProperties() != null) {
            for (GraphPropertyDef property : entity.getProperties()) {
                property.setGraphId(entity.getGraphId());
                property.setEntityId(entity.getId());
                property.setPropertyType(getPropertyType());
                if (property.getCode() == null || property.getCode().isEmpty()) {
                    property.setCode(property.getName());
                }
                graphPropertyDefService.saveOrUpdate(property);
            }
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateEntityDefWithProperties(T entity) {
        boolean updated;

        if (entity.getId() != null && this.getById(entity.getId()) != null) {
            updated = this.updateById(entity);
        } else {
            updated = this.save(entity);
        }

        if (updated) {
            QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("entity_id", entity.getId());
            deleteWrapper.eq("property_type", getPropertyType());
            graphPropertyDefService.remove(deleteWrapper);

            if (entity.getProperties() != null) {
                for (GraphPropertyDef property : entity.getProperties()) {
                    property.setGraphId(entity.getGraphId());
                    property.setEntityId(entity.getId());
                    property.setPropertyType(getPropertyType());
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
    public boolean deleteEntityDefWithProperties(Long id) {
        QueryWrapper<GraphPropertyDef> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("entity_id", id);
        deleteWrapper.eq("property_type", getPropertyType());
        graphPropertyDefService.remove(deleteWrapper);
        return this.removeById(id);
    }
}
