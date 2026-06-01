package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.GraphVertexDef;

import java.util.List;

/**
 * 图节点定义服务接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:45
 */
public interface GraphVertexDefService extends IService<GraphVertexDef> {

    /**
     * 根据图ID获取节点定义列表
     *
     * @param graphId 图ID
     * @param status  节点状态
     * @return 节点定义列表
     */
    List<GraphVertexDef> getVertexDefsByGraphId(Long graphId, Integer status);

    /**
     * 保存节点定义及其属性
     *
     * @param vertexDef 节点定义
     * @return 是否保存成功
     */
    boolean saveVertexDefWithProperties(GraphVertexDef vertexDef);

    /**
     * 更新节点定义及其属性
     *
     * @param vertexDef 节点定义
     * @return 是否更新成功
     */
    boolean updateVertexDefWithProperties(GraphVertexDef vertexDef);

    /**
     * 删除节点定义及其属性
     *
     * @param id 节点定义ID
     * @return 是否删除成功
     */
    boolean deleteVertexDefWithProperties(Long id);
}