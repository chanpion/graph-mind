package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.GraphNodeDef;

import java.util.List;

/**
 * 图节点定义服务接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:40
 */
public interface GraphNodeDefService extends IService<GraphNodeDef> {
    
    /**
     * 根据图ID获取节点定义列表
     * @param graphId 图ID
     * @return 节点定义列表
     */
    List<GraphNodeDef> getNodeDefsByGraphId(Long graphId);
    
    /**
     * 保存节点定义及其属性
     * @param nodeDef 节点定义
     * @return 是否保存成功
     */
    boolean saveNodeDefWithProperties(GraphNodeDef nodeDef);
    
    /**
     * 更新节点定义及其属性
     * @param nodeDef 节点定义
     * @return 是否更新成功
     */
    boolean updateNodeDefWithProperties(GraphNodeDef nodeDef);
    
    /**
     * 删除节点定义及其属性
     * @param id 节点定义ID
     * @return 是否删除成功
     */
    boolean deleteNodeDefWithProperties(Long id);
}