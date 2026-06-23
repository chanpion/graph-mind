package com.chenpp.graph.admin.service;

import com.chenpp.graph.admin.model.GraphExpandRequest;
import com.chenpp.graph.admin.model.GraphQueryRequest;
import com.chenpp.graph.admin.model.GraphPathRequest;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphSummary;

/**
 * 图查询服务接口
 * 专门处理图数据的查询、展开、路径查找等操作
 *
 * @author April.Chen
 * @date 2026/6/22
 */
public interface GraphQueryService {

    /**
     * 执行图查询
     *
     * @param request 查询请求参数
     * @return 查询结果
     */
    GraphData query(GraphQueryRequest request);

    /**
     * 展开邻居节点
     *
     * @param request 展开请求参数
     * @return 展开结果
     */
    GraphData expand(GraphExpandRequest request);

    /**
     * 查找路径
     *
     * @param request 路径查询请求参数
     * @return 路径查询结果
     */
    GraphData findPath(GraphPathRequest request);

    /**
     * 获取图统计信息
     *
     * @param graphId      图ID
     * @param connectionId 连接ID（可选）
     * @param graphCode    图代码（可选）
     * @return 图统计信息
     */
    GraphSummary getSummary(Long graphId, Long connectionId, String graphCode);
}