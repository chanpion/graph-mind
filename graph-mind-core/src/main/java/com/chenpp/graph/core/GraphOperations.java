package com.chenpp.graph.core;

import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphSchema;

import java.util.List;

/**
 * 图操作接口
 *
 * @author April.Chen
 * @date 2025/4/7 17:43
 */
public interface GraphOperations {

    /**
     * 创建图
     *
     * @param graphConf 图配置信息
     */
    void createGraph(GraphConf graphConf);

    /**
     * 删除图
     *
     * @param graphConf 图配置信息
     * @throws GraphException 图操作异常
     */
    void dropGraph(GraphConf graphConf) throws GraphException;


    /**
     * 列出图
     *
     * @param graphConf 图配置信息
     * @return 图谱列表
     */
    List<Graph> listGraphs(GraphConf graphConf);

    /**
     * 应用图谱schema
     *
     * @param graphConf   图配置信息
     * @param graphSchema 图谱schema
     */
    void applySchema(GraphConf graphConf, GraphSchema graphSchema);

    /**
     * 查询已发布的图谱schema
     *
     * @param graphConf 图配置信息
     * @return 图谱schema
     * @throws GraphException 图操作异常
     */
    GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException;
}