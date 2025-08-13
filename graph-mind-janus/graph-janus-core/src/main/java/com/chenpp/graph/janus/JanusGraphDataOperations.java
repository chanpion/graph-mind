package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.JanusGraphEdge;

import java.util.Collection;
import java.util.Iterator;

/**
 * JanusGraph数据操作实现类
 *
 * @author April.Chen
 * @date 2025/8/13 17:00
 */
@Slf4j
public class JanusGraphDataOperations implements GraphDataOperations {
    private JanusGraph graph;

    public JanusGraphDataOperations(JanusGraph graph) {
        this.graph = graph;
    }

    @Override
    public GraphVertex addVertex(GraphVertex vertex) throws GraphException {
        try {
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 创建顶点
                JanusGraphVertex janusVertex = tx.addVertex(vertex.getLabel());
                
                // 设置UID
                if (vertex.getUid() != null) {
                    janusVertex.property("uid", vertex.getUid());
                }
                
                // 设置其他属性
                if (vertex.getProperties() != null) {
                    vertex.getProperties().forEach((key, value) -> {
                        if (value != null) {
                            janusVertex.property(key, value);
                        }
                    });
                }
                
                // 提交事务
                tx.commit();
                
                // 返回创建的顶点
                return vertex;
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to add vertex: " + vertex.getUid(), e);
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        try {
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 查找顶点
                Iterator<JanusGraphVertex> vertices = tx.query().has("uid", vertex.getUid()).vertices().iterator();
                if (vertices.hasNext()) {
                    JanusGraphVertex janusVertex = vertices.next();
                    
                    // 更新属性
                    if (vertex.getProperties() != null) {
                        vertex.getProperties().forEach((key, value) -> {
                            if (value != null) {
                                janusVertex.property(key, value);
                            }
                        });
                    }
                    
                    // 提交事务
                    tx.commit();
                    return vertex;
                } else {
                    tx.rollback();
                    throw new GraphException("Vertex not found with uid: " + vertex.getUid());
                }
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to update vertex: " + vertex.getUid(), e);
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            return;
        }
        
        vertices.forEach(this::addVertex);
    }

    @Override
    public void deleteVertex(GraphVertex vertex) throws GraphException {
        try {
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 查找顶点
                Iterator<JanusGraphVertex> vertices = tx.query().has("uid", vertex.getUid()).vertices().iterator();
                if (vertices.hasNext()) {
                    JanusGraphVertex janusVertex = vertices.next();
                    // 删除顶点
                    janusVertex.remove();
                    // 提交事务
                    tx.commit();
                } else {
                    tx.rollback();
                    throw new GraphException("Vertex not found with uid: " + vertex.getUid());
                }
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to delete vertex: " + vertex.getUid(), e);
        }
    }

    @Override
    public void addEdge(GraphEdge edge) throws GraphException {
        try {
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 查找起始顶点
                Iterator<JanusGraphVertex> startVertices = tx.query().has("uid", edge.getStartUid()).vertices().iterator();
                if (!startVertices.hasNext()) {
                    tx.rollback();
                    throw new GraphException("Start vertex not found with uid: " + edge.getStartUid());
                }
                JanusGraphVertex startVertex = startVertices.next();
                
                // 查找结束顶点
                Iterator<JanusGraphVertex> endVertices = tx.query().has("uid", edge.getEndUid()).vertices().iterator();
                if (!endVertices.hasNext()) {
                    tx.rollback();
                    throw new GraphException("End vertex not found with uid: " + edge.getEndUid());
                }
                JanusGraphVertex endVertex = endVertices.next();
                
                // 创建边
                JanusGraphEdge janusEdge = startVertex.addEdge(edge.getLabel(), endVertex);
                
                // 设置UID
                if (edge.getUid() != null) {
                    janusEdge.property("uid", edge.getUid());
                }
                
                // 设置其他属性
                if (edge.getProperties() != null) {
                    edge.getProperties().forEach((key, value) -> {
                        if (value != null) {
                            janusEdge.property(key, value);
                        }
                    });
                }
                
                // 提交事务
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to add edge from " + edge.getStartUid() + " to " + edge.getEndUid(), e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            return;
        }
        
        edges.forEach(this::addEdge);
    }

    @Override
    public int updateEdge(GraphEdge edge) throws GraphException {
        try {
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 在JanusGraph中更新边的属性
                Iterator<JanusGraphEdge> edges = tx.query().has("uid", edge.getUid()).edges().iterator();
                if (edges.hasNext()) {
                    JanusGraphEdge janusEdge = edges.next();
                    
                    // 更新属性
                    if (edge.getProperties() != null) {
                        edge.getProperties().forEach((key, value) -> {
                            if (value != null) {
                                janusEdge.property(key, value);
                            }
                        });
                    }
                    
                    // 提交事务
                    tx.commit();
                    return 1; // 成功更新一条边
                } else {
                    tx.rollback();
                    throw new GraphException("Edge not found with uid: " + edge.getUid());
                }
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to update edge: " + edge.getUid(), e);
        }
    }

    @Override
    public int deleteEdge(GraphEdge edge) throws GraphException {
        try {
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 查找边
                Iterator<JanusGraphEdge> edges = tx.query().has("uid", edge.getUid()).edges().iterator();
                if (edges.hasNext()) {
                    JanusGraphEdge janusEdge = edges.next();
                    // 删除边
                    janusEdge.remove();
                    // 提交事务
                    tx.commit();
                    return 1; // 成功删除一条边
                } else {
                    tx.rollback();
                    throw new GraphException("Edge not found with uid: " + edge.getUid());
                }
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to delete edge: " + edge.getUid(), e);
        }
    }

    @Override
    public GraphData query(String cypher) throws GraphException {
        // JanusGraph主要使用Gremlin查询语言而不是Cypher
        // 这里可以实现Gremlin查询或者其他自定义查询逻辑
        throw new UnsupportedOperationException("Cypher query is not supported in JanusGraph. Use Gremlin instead.");
    }
}