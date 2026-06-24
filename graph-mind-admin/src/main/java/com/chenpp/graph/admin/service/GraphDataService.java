package com.chenpp.graph.admin.service;

import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
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
     * 导入顶点数据到图数据库
     *
     * @param graphId      图ID（可选，与 connectionId 二选一）
     * @param vertexTypeId 节点类型ID（可选，与 label 二选一）
     * @param connectionId 连接ID（可选，与 graphId 二选一）
     * @param graphCode    图编码（可选，配合 connectionId 使用）
     * @param label        标签名称（可选，配合 connectionId 使用）
     * @param file         CSV文件
     * @param config       导入配置JSON（含 delimiter、hasHeader 等）
     * @return 导入结果
     */
    ImportResult importVertexData(Long graphId, Long vertexTypeId, Long connectionId, String graphCode, String label, MultipartFile file, String config);

    /**
     * 导入边数据到图数据库
     *
     * @param graphId      图ID（可选，与 connectionId 二选一）
     * @param edgeTypeId   边类型ID（可选，与 label 二选一）
     * @param connectionId 连接ID（可选，与 graphId 二选一）
     * @param graphCode    图编码（可选，配合 connectionId 使用）
     * @param label        标签名称（可选，配合 connectionId 使用）
     * @param file         CSV文件
     * @param config       导入配置JSON（含 delimiter、hasHeader 等）
     * @return 导入结果
     */
    ImportResult importEdgeData(Long graphId, Long edgeTypeId, Long connectionId, String graphCode, String label, MultipartFile file, String config);

    /**
     * 查询节点数据列表
     *
     * @param graphId    图ID
     * @param vertexTypeId 节点类型ID
     * @param label      节点标签（发现的图为负ID时传入）
     * @param page       页码
     * @param size       每页大小
     * @return 分页节点数据
     */
    PageResult<GraphVertex> queryVertexDataList(Long graphId, Long vertexTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode);

    /**
     * 查询边数据列表
     *
     * @param graphId    图ID
     * @param edgeTypeId 边类型ID
     * @param label      边类型标签（发现的图为负ID时传入）
     * @param page       页码
     * @param size       每页大小
     * @return 分页边数据
     */
    PageResult<Map<String, Object>> queryEdgeDataList(Long graphId, Long edgeTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode);

    /**
     * 获取节点数据详情
     *
     * @param graphId 图ID
     * @param vertexId  节点ID
     * @return 节点数据详情
     */
    GraphVertex getVertexData(Long graphId, String vertexId);

    /**
     * 获取边数据详情
     *
     * @param graphId 图ID
     * @param edgeId  边ID
     * @return 边数据详情
     */
    GraphEdge getEdgeData(Long graphId, String edgeId);

    /**
     * 新增节点数据
     *
     * @param graphId    图ID
     * @param vertexTypeId 节点类型ID
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @param data       节点数据
     * @return 是否成功
     */
    boolean addVertexData(Long graphId, Long vertexTypeId, Long connectionId, String graphCode, Map<String, Object> data);

    /**
     * 新增边数据
     *
     * @param graphId    图ID
     * @param edgeTypeId 边类型ID
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @param data       边数据
     * @return 是否成功
     */
    boolean addEdgeData(Long graphId, Long edgeTypeId, Long connectionId, String graphCode, Map<String, Object> data);

    /**
     * 更新节点数据
     *
     * @param graphId 图ID
     * @param vertexId  节点ID
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @param data    节点数据
     * @return 是否成功
     */
    boolean updateVertexData(Long graphId, String vertexId, Long connectionId, String graphCode, Map<String, Object> data);

    /**
     * 更新边数据
     *
     * @param graphId 图ID
     * @param edgeId  边ID
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @param data    边数据
     * @return 是否成功
     */
    boolean updateEdgeData(Long graphId, String edgeId, Long connectionId, String graphCode, Map<String, Object> data);

    /**
     * 删除图数据库中的节点
     *
     * @param graphId 图ID
     * @param vertexId  节点ID
     * @param label   节点标签
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @return 删除结果
     */
    boolean deleteVertex(Long graphId, String vertexId, String label, Long connectionId, String graphCode);

    /**
     * 批量删除图数据库中的节点
     *
     * @param graphId 图ID
     * @param vertexIds 节点ID列表
     * @param label   节点标签
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @return 删除结果
     */
    boolean deleteVertices(Long graphId, List<String> vertexIds, String label, Long connectionId, String graphCode);

    /**
     * 删除图数据库中的边
     *
     * @param graphId 图ID
     * @param edgeId  边ID
     * @param label   边标签
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @return 删除结果
     */
    boolean deleteEdge(Long graphId, String edgeId, String label, Long connectionId, String graphCode);

    /**
     * 批量删除图数据库中的边
     *
     * @param graphId 图ID
     * @param edgeIds 边ID列表
     * @param label   边标签
     * @param connectionId 连接ID（发现的图使用）
     * @param graphCode   图代码（发现的图使用）
     * @return 删除结果
     */
    boolean deleteEdges(Long graphId, List<String> edgeIds, String label, Long connectionId, String graphCode);
    
    /**
     * 获取图统计信息
     *
     * @param graphId 图ID
     * @return 图统计信息
     */
    GraphSummary getGraphSummary(Long graphId, Long connectionId, String graphCode);
}