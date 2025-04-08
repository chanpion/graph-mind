package com.chenpp.graph.core;

import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphSchema;

import java.util.List;

/**
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
     * @throws GraphException
     */
    void dropGraph(GraphConf graphConf) throws GraphException;


    /**
     * 列出图
     *
     * @return 图谱列表
     */
    List<Graph> listGraphs();

    /**
     * 应用图谱schema
     *
     * @param graphSchema 图谱schema
     */
    void applySchema(GraphSchema graphSchema);
}
