package com.chenpp.graph.core;

import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.GraphSummary;

import java.util.Collection;

/**
 * 图数据接口定义
 *
 * @author April.Chen
 * @date 2025/4/7 17:43
 */
public interface GraphDataOperations {


    /**
     * 添加节点
     *
     * @param vertex 点
     * @return 点
     * @throws GraphException 插入异常
     */
    GraphVertex addVertex(GraphVertex vertex) throws GraphException;

    /**
     * 更新节点
     *
     * @param vertex 节点
     * @return 点
     * @throws GraphException 更新异常
     */
    GraphVertex updateVertex(GraphVertex vertex) throws GraphException;

    /**
     * 批量添加节点
     *
     * @param vertices 点集合
     * @throws GraphException 插入异常
     */
    void addVertices(Collection<GraphVertex> vertices) throws GraphException;

    /**
     * 删除节点
     *
     * @param vertex 节点
     * @throws GraphException 删除异常
     */
    void deleteVertex(GraphVertex vertex) throws GraphException;

    /**
     * 添加边
     *
     * @param edge 边
     * @throws GraphException 插入异常
     */
    void addEdge(GraphEdge edge) throws GraphException;

    /**
     * 批量添加边
     *
     * @param edges 边集合
     * @throws GraphException 插入异常
     */
    void addEdges(Collection<GraphEdge> edges) throws GraphException;

    /**
     * 更新边
     *
     * @param edge 边
     * @return 边
     * @throws GraphException 更新异常
     */
    int updateEdge(GraphEdge edge) throws GraphException;

    /**
     * 删除边
     *
     * @param edge 边
     * @return 删除的边数量
     * @throws GraphException 删除异常
     */
    int deleteEdge(GraphEdge edge) throws GraphException;

    /**
     * 查询图数据
     *
     * @param query 查询语句
     * @return GraphData 图数据
     * @throws GraphException 查询异常
     */
    GraphData query(String query) throws GraphException;

    /**
     * 展开节点，获取指定节点的邻居信息
     *
     * @param nodeId 节点ID
     * @param depth  展开深度
     * @return GraphData 包含节点及其邻居信息的图数据
     * @throws GraphException 查询异常
     */
    GraphData expand(String nodeId, int depth) throws GraphException;

    /**
     * 查找两个节点之间的路径
     *
     * @param startNodeId 起始节点ID
     * @param endNodeId   目标节点ID
     * @param maxDepth    最大搜索深度
     * @return GraphData 包含路径信息的图数据
     * @throws GraphException 查询异常
     */
    GraphData findPath(String startNodeId, String endNodeId, int maxDepth) throws GraphException;

    /**
     * 获取图数据统计信息
     *
     * @return GraphSummary 图数据统计信息
     * @throws GraphException 查询异常
     */
    GraphSummary getSummary() throws GraphException;
}