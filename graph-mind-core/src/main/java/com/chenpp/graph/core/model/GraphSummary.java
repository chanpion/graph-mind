package com.chenpp.graph.core.model;

import lombok.Data;

import java.util.Map;

/**
 * 图数据统计
 *
 * @author April.Chen
 * @date 2025/8/26 14:25
 */
@Data
public class GraphSummary {

    private String graphCode;
    private String graphName;
    private int vertexCount;
    private int edgeCount;

    private Map<String, Integer> vertexLabelCount;
    private Map<String, Integer> edgeLabelCount;
}
