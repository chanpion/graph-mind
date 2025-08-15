package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.GraphEdgeDef;

import java.util.List;

/**
 * 图边定义服务接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:45
 */
public interface GraphEdgeDefService extends IService<GraphEdgeDef> {
    
    /**
     * 根据图ID获取边定义列表
     *
     * @param graphId 图ID
     * @param status 状态
     * @return 边定义列表
     */
    List<GraphEdgeDef> getEdgeDefsByGraphId(Long graphId, Integer status);
    
    /**
     * 保存边定义及其属性
     * @param edgeDef 边定义
     * @return 是否保存成功
     */
    boolean saveEdgeDefWithProperties(GraphEdgeDef edgeDef);
    
    /**
     * 更新边定义及其属性
     * @param edgeDef 边定义
     * @return 是否更新成功
     */
    boolean updateEdgeDefWithProperties(GraphEdgeDef edgeDef);
    
    /**
     * 删除边定义及其属性
     * @param id 边定义ID
     * @return 是否删除成功
     */
    boolean deleteEdgeDefWithProperties(Long id);
}