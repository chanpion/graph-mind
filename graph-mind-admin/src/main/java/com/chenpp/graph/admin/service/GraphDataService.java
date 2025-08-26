package com.chenpp.graph.admin.service;

import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.core.model.GraphSummary;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
     * 查询节点数据列表
     *
     * @param graphId    图ID
     * @param nodeTypeId 节点类型ID
     * @param page       页码
     * @param size       每页大小
     * @return 节点数据列表
     */
    List<Map<String, Object>> getNodeDataList(Long graphId, Long nodeTypeId, Integer page, Integer size);

    /**
     * 查询边数据列表
     *
     * @param graphId    图ID
     * @param edgeTypeId 边类型ID
     * @param page       页码
     * @param size       每页大小
     * @return 边数据列表
     */
    List<Map<String, Object>> getEdgeDataList(Long graphId, Long edgeTypeId, Integer page, Integer size);

    /**
     * 获取节点数据详情
     *
     * @param graphId 图ID
     * @param nodeId  节点ID
     * @return 节点数据详情
     */
    Map<String, Object> getNodeData(Long graphId, String nodeId);

    /**
     * 获取边数据详情
     *
     * @param graphId 图ID
     * @param edgeId  边ID
     * @return 边数据详情
     */
    Map<String, Object> getEdgeData(Long graphId, String edgeId);

    /**
     * 新增节点数据
     *
     * @param graphId    图ID
     * @param nodeTypeId 节点类型ID
     * @param data       节点数据
     * @return 是否成功
     */
    boolean addNodeData(Long graphId, Long nodeTypeId, Map<String, Object> data);

    /**
     * 新增边数据
     *
     * @param graphId    图ID
     * @param edgeTypeId 边类型ID
     * @param data       边数据
     * @return 是否成功
     */
    boolean addEdgeData(Long graphId, Long edgeTypeId, Map<String, Object> data);

    /**
     * 更新节点数据
     *
     * @param graphId 图ID
     * @param nodeId  节点ID
     * @param data    节点数据
     * @return 是否成功
     */
    boolean updateNodeData(Long graphId, String nodeId, Map<String, Object> data);

    /**
     * 更新边数据
     *
     * @param graphId 图ID
     * @param edgeId  边ID
     * @param data    边数据
     * @return 是否成功
     */
    boolean updateEdgeData(Long graphId, String edgeId, Map<String, Object> data);

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
    
    /**
     * 获取图统计信息
     *
     * @param graphId 图ID
     * @return 图统计信息
     */
    GraphSummary getGraphSummary(Long graphId);
}