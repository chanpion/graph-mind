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


    /**
     * 检查连接
     *
     * @return true:连接正常, false:连接异常
     */
    boolean checkConnection();

    /**
     * 关闭连接
     *
     * @return true:关闭成功, false:关闭失败
     */
    void close();
}
