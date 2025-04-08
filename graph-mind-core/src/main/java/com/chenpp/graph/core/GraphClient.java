package com.chenpp.graph.core;

/**
 * @author April.Chen
 * @date 2025/4/7 17:42
 */
public interface GraphClient {


    /**
     * 图操作
     *
     * @return GraphOperations
     */
    GraphOperations opsForGraph();

    /**
     * 图数据操作
     *
     * @return GraphDataOperations
     */
    GraphDataOperations opsForGraphData();
}
