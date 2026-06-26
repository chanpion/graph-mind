package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.mapper.GraphVertexDefDao;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.core.constant.GraphConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图节点定义服务实现类
 *
 * @author April.Chen
 * @date 2025/8/4 15:50
 */
@Service
public class GraphVertexDefServiceImpl extends AbstractEntityDefService<GraphVertexDefDao, GraphVertexDef>
        implements GraphVertexDefService {

    @Override
    protected String getPropertyType() {
        return GraphConstants.VERTEX;
    }

    @Override
    public List<GraphVertexDef> getVertexDefsByGraphId(Long graphId, Integer status) {
        return getEntityDefsByGraphId(graphId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveVertexDefWithProperties(GraphVertexDef vertexDef) {
        return saveEntityDefWithProperties(vertexDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVertexDefWithProperties(GraphVertexDef vertexDef) {
        return updateEntityDefWithProperties(vertexDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVertexDefWithProperties(Long id) {
        return deleteEntityDefWithProperties(id);
    }
}
