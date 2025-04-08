package com.chenpp.graph.core.model;

import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author April.Chen
 * @date 2024/5/31 10:47
 */
@Data
public class GraphData implements Serializable {

    private static final long serialVersionUID = -6719770439482247866L;

    private List<GraphVertex> vertices;
    private List<GraphEdge> edges;

    public void addVertex(GraphVertex vertex) {
        if (this.vertices == null) {
            this.vertices = Lists.newArrayList();
        }
        this.vertices.add(vertex);
    }

    public void addEdge(GraphEdge edge) {
        if (this.edges == null) {
            this.edges = Lists.newArrayList();
        }
        this.edges.add(edge);
    }
}
