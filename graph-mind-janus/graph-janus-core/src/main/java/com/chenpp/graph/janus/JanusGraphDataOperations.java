package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.json.StringEscapeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.BulkSet;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphEdge;
import org.janusgraph.core.JanusGraphTransaction;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.attribute.Contain;
import org.janusgraph.graphdb.relations.CacheEdge;
import org.janusgraph.graphdb.vertices.CacheVertex;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

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
        try (JanusGraphTransaction tx = graph.newTransaction()) {
            try {
                // 创建顶点
                JanusGraphVertex janusVertex = tx.addVertex(vertex.getLabel());
                // 设置UID
                janusVertex.property(GraphConstants.UID, vertex.getUid());
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
                if (janusVertex.id() != null) {
                    vertex.setId(janusVertex.id().toString());
                }
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
        try (JanusGraphTransaction tx = graph.newTransaction()) {
            try {
                // 查找顶点
                Iterator<JanusGraphVertex> vertices = tx.query().has(GraphConstants.UID, vertex.getUid()).vertices().iterator();
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
                throw new GraphException("Vertex not found with uid: " + vertex.getUid(), e);
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
        try (JanusGraphTransaction tx = graph.newTransaction()) {
            try {
                // 查找顶点
                Iterator<JanusGraphVertex> vertices = tx.query().has(GraphConstants.UID, vertex.getUid()).vertices().iterator();
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
        JanusGraphTransaction tx = graph.newTransaction();
        try {
            // 查找起始顶点
            Iterator<JanusGraphVertex> startVertices = tx.query().has(GraphConstants.UID, edge.getStartUid()).vertices().iterator();
            if (!startVertices.hasNext()) {
                tx.rollback();
                throw new GraphException("Start vertex not found with uid: " + edge.getStartUid());
            }
            JanusGraphVertex startVertex = startVertices.next();

            // 查找结束顶点
            Iterator<JanusGraphVertex> endVertices = tx.query().has(GraphConstants.UID, edge.getEndUid()).vertices().iterator();
            if (!endVertices.hasNext()) {
                tx.rollback();
                throw new GraphException("End vertex not found with uid: " + edge.getEndUid());
            }
            JanusGraphVertex endVertex = endVertices.next();

            // 创建边
            JanusGraphEdge janusEdge = startVertex.addEdge(edge.getLabel(), endVertex);

            // 设置UID
            if (edge.getUid() != null) {
                janusEdge.property(GraphConstants.UID, edge.getUid());
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
            if (tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to add edge from " + edge.getStartUid() + " to " + edge.getEndUid(), e);
        } finally {
            // 确保事务已关闭
            if (tx.isOpen()) {
                tx.close();
            }
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
        try (JanusGraphTransaction tx = graph.newTransaction()) {
            try {
                // 在JanusGraph中更新边的属性
                Iterator<JanusGraphEdge> edges = tx.query().has(GraphConstants.UID, edge.getUid()).edges().iterator();
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
                    return 1;
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
        try (JanusGraphTransaction tx = graph.newTransaction()) {
            try {
                // 查找边
                Iterator<JanusGraphEdge> edges = tx.query().has(GraphConstants.UID, edge.getUid()).edges().iterator();
                if (edges.hasNext()) {
                    JanusGraphEdge janusEdge = edges.next();
                    // 删除边
                    janusEdge.remove();
                    // 提交事务
                    tx.commit();
                    return 1;
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
    public GraphData query(String gremlinQuery) throws GraphException {
        if (StringUtils.isBlank(gremlinQuery)) {
            return new GraphData();
        }
        GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
        engine.put("graph", graph);
        engine.put("g", graph.traversal());
        try {
            Object result = engine.eval(gremlinQuery);
            if (result instanceof GraphTraversal) {
                return traversalResult((GraphTraversal) result);
            }
        } catch (ScriptException e) {
            log.error("Error executing Gremlin query: {}", gremlinQuery, e);
            throw new GraphException("Failed to execute Gremlin query: " + gremlinQuery, e);
        } finally {
            graph.tx().rollback();
        }
        return new GraphData();
    }

    private GraphData traversalResult(GraphTraversal<?, ?> traversal) {
        long start = System.currentTimeMillis();

        List<CacheVertex> vertexList = Lists.newArrayList();
        List<CacheEdge> edgeList = Lists.newArrayList();
        while (traversal.hasNext()) {
            Object object = traversal.next();
            if (object instanceof Map) {
                //noinspection unchecked
                ((Map) object).values().stream().flatMap(sets -> ((BulkSet) sets).stream()).filter(t -> t instanceof Element)
                        .forEach(item -> buildVertexAndEdgeCollection(item, vertexList, edgeList, true));
            }
            if (object instanceof Path) {
                Path path = (Path) object;
                if (CollectionUtils.isNotEmpty(path.objects())) {
                    for (Object obj : path.objects()) {
                        buildVertexAndEdgeCollection(obj, vertexList, edgeList, false);
                    }
                }
            }
            if (object instanceof Element) {
                buildVertexAndEdgeCollection(object, vertexList, edgeList, true);
            }
        }
        log.info("Iterate over the result set returned by gremlin server, time (ms)={}", System.currentTimeMillis() - start);


        GraphData graphData = convertToGraphData(vertexList, edgeList);
        long time = System.currentTimeMillis() - start;
        log.info("Total time of gremlin query (ms)={}", time);
        return graphData;
    }


    private void buildVertexAndEdgeCollection(Object element, List<CacheVertex> vertexList, List<CacheEdge> edgeList, boolean addVertexFromEdge) {
        if (element instanceof CacheVertex) {
            vertexList.add((CacheVertex) element);
        } else if (element instanceof CacheEdge edge) {
            edgeList.add((CacheEdge) element);
            if (addVertexFromEdge) {
                vertexList.add((CacheVertex) edge.outVertex());
                vertexList.add((CacheVertex) edge.inVertex());
            }
        }
    }

    private GraphData convertToGraphData(List<CacheVertex> vertexList, List<CacheEdge> edgeList) {
        GraphData graphData = new GraphData();
        List<GraphVertex> vertices = convertVertex(vertexList);
        // 处理重复key的情况，当key冲突时保留第一个值
        Map<String, GraphVertex> idVertexMap = vertices.stream()
                .collect(Collectors.toMap(GraphVertex::getId, v -> v, (existing, replacement) -> existing));

        List<GraphEdge> edges = convertEdge(edgeList);
        edges.forEach(edge -> {
            GraphVertex startVertex = idVertexMap.get(edge.getStartUid());
            GraphVertex endVertex = idVertexMap.get(edge.getEndUid());
            if (startVertex != null) {
                edge.setStartUid(startVertex.getUid());
                edge.setStartLabel(startVertex.getLabel());
            }
            if (endVertex != null) {
                edge.setEndUid(endVertex.getUid());
                edge.setEndLabel(endVertex.getLabel());
            }
        });

        graphData.setVertices(vertices);
        graphData.setEdges(edges);
        return graphData;
    }

    /**
     * 转换成vertex
     *
     * @param detachedVertexList CacheVertex list
     * @return Multimap，以uid作为key，value为统一uid的集合。
     */
    private List<GraphVertex> convertVertex(List<CacheVertex> detachedVertexList) {
        List<GraphVertex> vertexList = Lists.newArrayList();
        Set< String> idSet = new HashSet<>();
        for (CacheVertex detachedVertex : detachedVertexList) {
            String id = valueOf(detachedVertex.id());
            if (idSet.contains(id)){
                continue;
            }else {
                idSet.add(id);
            }
            GraphVertex graphVertex = new GraphVertex();
            graphVertex.setId(id);
            graphVertex.setLabel(detachedVertex.label());
            Map<String, Object> map = Maps.newHashMap();
            for (String key : detachedVertex.keys()) {
                Iterator<VertexProperty<Object>> iterator = detachedVertex.properties(key);
                List<Object> valueList = Lists.newArrayList(iterator).stream().map(p -> {
                    if (p.value() instanceof Date) {
                        return DateFormatUtils.format((Date) p.value(), "yyyy-MM-dd HH:mm:ss");
                    }
                    return p.value();
                }).collect(toList());
                map.put(key, valueList.size() > 1 ? valueList : valueList.get(0));
            }
            graphVertex.setProperties(map);

            String uid = graphVertex.getProperties().getOrDefault(GraphConstants.UID, "").toString();
            graphVertex.setUid(uid);
            vertexList.add(graphVertex);
        }
        return vertexList;
    }

    /**
     * 转换成edge
     *
     * @param detachedEdgeList CacheEdge list
     * @return EdgeDTO list
     */
    private List<GraphEdge> convertEdge(List<CacheEdge> detachedEdgeList) {
        List<GraphEdge> edgeList = Lists.newArrayList();
        for (CacheEdge detachedEdge : detachedEdgeList) {
            GraphEdge graphEdge = new GraphEdge();
            graphEdge.setId(valueOf(detachedEdge.id()));

            Object inId = detachedEdge.inVertex().id();
            Object outId = detachedEdge.outVertex().id();
            graphEdge.setStartUid(valueOf(inId));
            graphEdge.setEndUid(valueOf(outId));

            graphEdge.setLabel(detachedEdge.label());
            Map<String, Object> map = Maps.newHashMap();
            for (String key : detachedEdge.keys()) {
                Iterator<Property<Object>> iterator = detachedEdge.properties(key);
                List<Object> valueList = Lists.newArrayList(iterator).stream().map(p -> {
                    if (p.value() instanceof Date) {
                        return DateFormatUtils.format((Date) p.value(), "yyyy-MM-dd HH:mm:ss");
                    }
                    return p.value();
                }).collect(toList());
                map.put(key, valueList.size() > 1 ? valueList : valueList.get(0));
            }
            graphEdge.setProperties(map);
            graphEdge.setUid(map.getOrDefault(GraphConstants.UID, "").toString());
            edgeList.add(graphEdge);
        }
        return edgeList;
    }

    /**
     * 处理查询结果
     *
     * @param result    查询结果
     * @param graphData 图数据容器
     */
    private void processQueryResult(Object result, GraphData graphData) {
        if (result == null) {
            return;
        }

        // 处理不同类型的查询结果
        if (result instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) result;
            for (Object item : iterable) {
                processResultItem(item, graphData);
            }
        } else {
            processResultItem(result, graphData);
        }
    }

    /**
     * 处理单个查询结果项
     *
     * @param item      结果项
     * @param graphData 图数据容器
     */
    private void processResultItem(Object item, GraphData graphData) {
        if (item instanceof Vertex) {
            graphData.addVertex(convertVertex((Vertex) item));
        } else if (item instanceof Edge) {
            graphData.addEdge(convertEdge((Edge) item));
        } else if (item instanceof Map) {
            // 处理Map类型的结果，可能是属性
            // 这里可以进一步扩展处理逻辑
            log.debug("Map result item: {}", item);
        } else {
            // 其他类型的结果，暂时忽略
            log.debug("Unknown result item type: {}", item.getClass());
        }
    }

    /**
     * 转换TinkerPop Vertex为GraphVertex
     *
     * @param vertex TinkerPop顶点
     * @return GraphVertex
     */
    private GraphVertex convertVertex(Vertex vertex) {
        GraphVertex graphVertex = new GraphVertex();
        graphVertex.setId(vertex.id().toString());

        // 获取标签
        graphVertex.setLabel(vertex.label());

        // 获取UID（如果存在）
        if (vertex.properties("uid").hasNext()) {
            graphVertex.setUid(vertex.property("uid").value().toString());
        }

        // 获取属性
        Map<String, Object> properties = new HashMap<>();
        vertex.keys().forEach(key -> {
            Object value = vertex.property(key).orElse(null);
            if (value != null) {
                properties.put(key, value);
            }
        });

        graphVertex.setProperties(properties);
        return graphVertex;
    }

    /**
     * 转换TinkerPop Edge为GraphEdge
     *
     * @param edge TinkerPop边
     * @return GraphEdge
     */
    private GraphEdge convertEdge(Edge edge) {
        GraphEdge graphEdge = new GraphEdge();
        graphEdge.setId(edge.id().toString());

        // 获取标签
        graphEdge.setLabel(edge.label());

        // 获取起始和结束顶点ID
        graphEdge.setStartUid(edge.outVertex().id().toString());
        graphEdge.setEndUid(edge.inVertex().id().toString());

        // 获取UID（如果存在）
        if (edge.properties("uid").hasNext()) {
            graphEdge.setUid(edge.property("uid").value().toString());
        }

        // 获取属性
        Map<String, Object> properties = new HashMap<>();
        edge.keys().forEach(key -> {
            Object value = edge.property(key).orElse(null);
            if (value != null) {
                properties.put(key, value);
            }
        });

        graphEdge.setProperties(properties);
        return graphEdge;
    }

    /**
     * 根据顶点ID列表查询顶点
     *
     * @param vertexIds 顶点ID列表
     * @return 顶点列表
     * @throws GraphException 查询异常
     */
    public List<GraphVertex> getVerticesByIds(List<String> vertexIds) throws GraphException {
        if (CollectionUtils.isEmpty(vertexIds)) {
            return new ArrayList<>();
        }
        List<GraphVertex> result = new ArrayList<>();

        try (JanusGraphTransaction tx = graph.newTransaction()) {
            // 查询顶点
            Iterator<JanusGraphVertex> vertices = tx.query().has("uid", Contain.IN, vertexIds).vertices().iterator();
            while (vertices.hasNext()) {
                JanusGraphVertex vertex = vertices.next();
                GraphVertex graphVertex = parseVertex(vertex);
                result.add(graphVertex);
            }

            // 提交事务
            tx.commit();
            return result;
        } catch (Exception e) {
            throw new GraphException("Failed to get vertices by ids", e);
        }
    }

    /**
     * 根据边ID列表查询边
     *
     * @param edgeIds 边ID列表
     * @return 边列表
     * @throws GraphException 查询异常
     */
    public List<GraphEdge> getEdgesByIds(List<String> edgeIds) throws GraphException {
        if (CollectionUtils.isEmpty(edgeIds)) {
            return new ArrayList<>();
        }

        try {
            List<GraphEdge> result = new ArrayList<>();
            // 开启事务
            org.janusgraph.core.JanusGraphTransaction tx = graph.newTransaction();
            try {
                // 查询边
                Iterator<JanusGraphEdge> edges = tx.query().has(GraphConstants.UID, org.janusgraph.core.attribute.Contain.IN, edgeIds).edges().iterator();
                while (edges.hasNext()) {
                    JanusGraphEdge edge = edges.next();
                    GraphEdge graphEdge = parseEdge(edge);
                    result.add(graphEdge);
                }

                // 提交事务
                tx.commit();
                return result;
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new GraphException("Failed to get edges by ids", e);
        }
    }

    @Override
    public GraphData expand(String nodeId, int depth) throws GraphException {
        String gremlinQuery = String.format("g.V().has('%s', '%s').repeat(bothE().bothV().simplePath()).times(%d).path()", 
                GraphConstants.UID, nodeId, depth);
        return query(gremlinQuery);
    }

    @Override
    public GraphData findPath(String startNodeId, String endNodeId, int maxDepth) throws GraphException {
        String gremlinQuery = String.format("g.V().has('%s', '%s').repeat(bothE().bothV().simplePath()).until(has('%s', '%s')).limit(1).path()",
                GraphConstants.UID, startNodeId, GraphConstants.UID, endNodeId);
        return query(gremlinQuery);
    }

    /**
     * 解析JanusGraphVertex为GraphVertex
     *
     * @param vertex JanusGraphVertex对象
     * @return GraphVertex对象
     */
    private GraphVertex parseVertex(JanusGraphVertex vertex) {
        GraphVertex graphVertex = new GraphVertex();

        // 获取UID
        if (vertex.property(GraphConstants.UID).isPresent()) {
            graphVertex.setUid(vertex.property("uid").value().toString());
        }
        // 获取ID
        graphVertex.setId(vertex.id().toString());
        // 获取标签
        graphVertex.setLabel(vertex.label());

        // 获取其他属性
        Map<String, Object> properties = new HashMap<>();
        vertex.keys().forEach(key -> {
            if (vertex.property(key).isPresent() && !"uid".equals(key)) {
                properties.put(key, vertex.property(key).value());
            }
        });
        graphVertex.setProperties(properties);

        return graphVertex;
    }

    /**
     * 解析JanusGraphEdge为GraphEdge
     *
     * @param edge JanusGraphEdge对象
     * @return GraphEdge对象
     */
    private GraphEdge parseEdge(JanusGraphEdge edge) {
        GraphEdge graphEdge = new GraphEdge();

        // 获取UID
        if (edge.property("uid").isPresent()) {
            graphEdge.setUid((String) edge.property("uid").value());
        }

        // 获取标签
        graphEdge.setLabel(edge.label());

        // 获取起始和结束顶点的UID
        JanusGraphVertex outVertex = edge.outVertex();
        JanusGraphVertex inVertex = edge.inVertex();

        if (outVertex.property("uid").isPresent()) {
            graphEdge.setStartUid((String) outVertex.property("uid").value());
        }

        if (inVertex.property("uid").isPresent()) {
            graphEdge.setEndUid((String) inVertex.property("uid").value());
        }

        // 获取其他属性
        Map<String, Object> properties = new HashMap<>();
        edge.keys().forEach(key -> {
            if (edge.property(key).isPresent() && !"uid".equals(key)) {
                properties.put(key, edge.property(key).value());
            }
        });
        graphEdge.setProperties(properties);

        return graphEdge;
    }
}