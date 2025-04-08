package com.chenpp.graph.core;

import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;

import java.util.Collection;

/**
 * @author April.Chen
 * @date 2025/4/7 17:43
 */
public interface GraphDataOperations {


    /**
     * 添加节点
     *
     * @param vertex 点
     * @throws GraphException 插入异常
     */
    void addVertex(GraphVertex vertex) throws GraphException;

    /**
     * 批量添加节点
     *
     * @param vertices 点集合
     * @throws GraphException 插入异常
     */
    void addVertices(Collection<GraphVertex> vertices) throws GraphException;


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

}
