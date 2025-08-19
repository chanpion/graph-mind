package com.chenpp.graph.admin.service;

import com.chenpp.graph.admin.model.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图数据服务接口
 * 专门处理图数据的导入、查询等操作
 *
 * @author April.Chen
 * @date 2025/8/11 15:30
 */
public interface GraphDataService {

    /**
     * 导入节点数据到图数据库
     *
     * @param graphId    图ID
     * @param nodeTypeId 节点类型ID
     * @param file       CSV文件
     * @param headers    表头信息
     * @param mapping    字段映射关系
     * @param data       数据内容
     * @return 导入结果
     */
    ImportResult importNodeData(Long graphId, Long nodeTypeId, MultipartFile file, String headers, String mapping, String data);

    /**
     * 导入边数据到图数据库
     *
     * @param graphId    图ID
     * @param edgeTypeId 边类型ID
     * @param file       CSV文件
     * @param headers    表头信息
     * @param mapping    字段映射关系
     * @param data       数据内容
     * @return 导入结果
     */
    ImportResult importEdgeData(Long graphId, Long edgeTypeId, MultipartFile file, String headers, String mapping, String data);

    /**
     * 删除图数据库中的节点
     *
     * @param graphId 图ID
     * @param nodeId  节点ID
     * @param label   节点标签
     * @return 删除结果
     */
    boolean deleteNode(Long graphId, String nodeId, String label);

    /**
     * 批量删除图数据库中的节点
     *
     * @param graphId 图ID
     * @param nodeIds 节点ID列表
     * @param label   节点标签
     * @return 删除结果
     */
    boolean deleteNodes(Long graphId, List<String> nodeIds, String label);
}