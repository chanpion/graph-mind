package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import org.springframework.web.multipart.MultipartFile;

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
     * @param graphId 图ID
     * @return 边定义列表
     */
    List<GraphEdgeDef> getEdgeDefsByGraphId(Long graphId);
    
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
    
    /**
     * 导入边数据
     * @param graphId 图ID
     * @param edgeTypeId 边类型ID
     * @param file CSV文件
     * @param headers 表头信息
     * @param mapping 字段映射关系
     * @param data 数据内容
     * @return 是否导入成功
     */
    boolean importEdgeData(Long graphId, Long edgeTypeId, MultipartFile file, String headers, String mapping, String data);
}