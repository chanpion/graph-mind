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
     * @return true 删除成功，false 节点不存在
     * @throws GraphException 删除异常
     */
    boolean deleteVertex(GraphVertex vertex) throws GraphException;

    /**
     * 添加边
     *
     * @param edge 边
     * @return 边（含数据库生成的 ID）
     * @throws GraphException 插入异常
     */
    GraphEdge addEdge(GraphEdge edge) throws GraphException;

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
     * @return 更新后的边
     * @throws GraphException 更新异常
     */
    GraphEdge updateEdge(GraphEdge edge) throws GraphException;

    /**
     * 删除边
     *
     * @param edge 边
     * @return true 删除成功，false 边不存在
     * @throws GraphException 删除异常
     */
    boolean deleteEdge(GraphEdge edge) throws GraphException;

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
     * @param vertexId 节点ID
     * @param depth  展开深度
     * @return GraphData 包含节点及其邻居信息的图数据
     * @throws GraphException 查询异常
     */
    GraphData expand(String vertexId, int depth) throws GraphException;

    /**
     * 查找两个节点之间的路径
     *
     * @param startVertexId 起始节点ID
     * @param endVertexId   目标节点ID
     * @param maxDepth    最大搜索深度
     * @return GraphData 包含路径信息的图数据
     * @throws GraphException 查询异常
     */
    GraphData findPath(String startVertexId, String endVertexId, int maxDepth) throws GraphException;

    /**
     * 获取图数据统计信息
     *
     * @return GraphSummary 图数据统计信息
     * @throws GraphException 查询异常
     */
    GraphSummary getSummary() throws GraphException;

    /**
     * 统计指定标签的节点数量
     *
     * @param label 节点标签
     * @return 节点总数
     * @throws GraphException 查询异常
     */
    long countVertices(String label) throws GraphException;

    /**
     * 统计指定类型的关系数量
     *
     * @param label 关系类型标签
     * @return 关系总数
     * @throws GraphException 查询异常
     */
    long countEdges(String label) throws GraphException;

    /**
     * 根据节点类型、属性、属性值查询节点数据
     *
     * @param label     节点类型标签
     * @param property  属性名
     * @param value     属性值
     * @return 匹配的节点，如果未找到返回null
     * @throws GraphException 查询异常
     */
    GraphVertex findVertex(String label, String property, String value) throws GraphException;
}