package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.mapper.GraphEdgeDefDao;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphSchemaService;
import com.chenpp.graph.core.constant.GraphConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    @Autowired
    private GraphSchemaService graphSchemaService;

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
        boolean saved = saveEntityDefWithProperties(edgeDef);
        if (saved && edgeDef.getGraphId() != null) {
            graphSchemaService.publishEdgeDef(edgeDef.getGraphId(), null, null, edgeDef.getLabel());
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEdgeDefWithProperties(GraphEdgeDef edgeDef) {
        boolean updated = updateEntityDefWithProperties(edgeDef);
        if (updated && edgeDef.getGraphId() != null) {
            graphSchemaService.publishEdgeDef(edgeDef.getGraphId(), null, null, edgeDef.getLabel());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEdgeDefWithProperties(Long id) {
        return deleteEntityDefWithProperties(id);
    }
}
