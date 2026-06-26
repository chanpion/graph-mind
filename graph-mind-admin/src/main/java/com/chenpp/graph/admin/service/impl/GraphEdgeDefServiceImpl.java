package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.mapper.GraphEdgeDefDao;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.core.constant.GraphConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图边定义服务实现类
 *
 * @author April.Chen
 * @date 2025/8/4 16:00
 */
@Service
public class GraphEdgeDefServiceImpl extends AbstractEntityDefService<GraphEdgeDefDao, GraphEdgeDef>
        implements GraphEdgeDefService {

    @Override
    protected String getPropertyType() {
        return GraphConstants.EDGE;
    }

    @Override
    public List<GraphEdgeDef> getEdgeDefsByGraphId(Long graphId, Integer status) {
        return getEntityDefsByGraphId(graphId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEdgeDefWithProperties(GraphEdgeDef edgeDef) {
        return saveEntityDefWithProperties(edgeDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEdgeDefWithProperties(GraphEdgeDef edgeDef) {
        return updateEntityDefWithProperties(edgeDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEdgeDefWithProperties(Long id) {
        return deleteEntityDefWithProperties(id);
    }
}
