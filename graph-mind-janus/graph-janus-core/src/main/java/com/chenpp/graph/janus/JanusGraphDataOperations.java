package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.PathQuery;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.BulkSet;
import org.apache.tinkerpop.gremlin.structure.Direction;
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
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            JanusGraphVertex janusVertex = tx.addVertex(vertex.getLabel());
            janusVertex.property(GraphConstants.UID, vertex.getUid());
            if (vertex.getProperties() != null) {
                vertex.getProperties().forEach((key, value) -> {
                    if (value != null) {
                        janusVertex.property(key, value);
                    }
                });
            }

            tx.commit();
            if (janusVertex.id() != null) {
                vertex.setId(janusVertex.id().toString());
            }
            return vertex;
        } catch (Exception e) {
            log.error("Failed to add vertex: {}", vertex, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to add vertex: " + vertex.getUid(), e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            Iterator<JanusGraphVertex> vertices = tx.query().has(GraphConstants.UID, vertex.getUid()).vertices().iterator();
            if (vertices.hasNext()) {
                JanusGraphVertex janusVertex = vertices.next();

                if (vertex.getProperties() != null) {
                    vertex.getProperties().forEach((key, value) -> {
                        if (value != null) {
                            janusVertex.property(key, value);
                        }
                    });
                }

                tx.commit();
                return vertex;
            } else {
                throw new GraphException("Vertex not found with uid: " + vertex.getUid());
            }
        } catch (Exception e) {
            log.error("Failed to update vertex: {}", vertex, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to update vertex: " + vertex.getUid(), e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            log.info("Vertices collection is empty, skipping batch insert");
            return;
        }

        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            for (GraphVertex vertex : vertices) {
                JanusGraphVertex janusVertex = tx.addVertex(vertex.getLabel());
                janusVertex.property(GraphConstants.UID, vertex.getUid());
                if (vertex.getProperties() != null) {
                    vertex.getProperties().forEach((key, value) -> {
                        if (value != null) {
                            janusVertex.property(key, value);
                        }
                    });
                }
                if (janusVertex.id() != null) {
                    vertex.setId(janusVertex.id().toString());
                }
            }
            tx.commit();
            log.info("Batch inserted {} vertices", vertices.size());
        } catch (Exception e) {
            log.error("Failed to batch add vertices", e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to batch add vertices", e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public boolean deleteVertex(GraphVertex vertex) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            Iterator<JanusGraphVertex> vertices = tx.query().has(GraphConstants.UID, vertex.getUid()).vertices().iterator();
            if (vertices.hasNext()) {
                JanusGraphVertex janusVertex = vertices.next();
                janusVertex.remove();
                tx.commit();
                return true;
            } else {
                throw new GraphException("Vertex not found with uid: " + vertex.getUid());
            }
        } catch (Exception e) {
            log.error("Failed to delete vertex: {}", vertex, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to delete vertex: " + vertex.getUid(), e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public GraphEdge addEdge(GraphEdge edge) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            // 查找起始顶点
            Iterator<JanusGraphVertex> startVertices = tx.query().has(GraphConstants.UID, edge.getStartUid()).vertices().iterator();
            if (!startVertices.hasNext()) {
                throw new GraphException("Start vertex not found with uid: " + edge.getStartUid());
            }
            JanusGraphVertex startVertex = startVertices.next();

            // 查找结束顶点
            Iterator<JanusGraphVertex> endVertices = tx.query().has(GraphConstants.UID, edge.getEndUid()).vertices().iterator();
            if (!endVertices.hasNext()) {
                throw new GraphException("End vertex not found with uid: " + edge.getEndUid());
            }
            JanusGraphVertex endVertex = endVertices.next();

            // 创建边
            JanusGraphEdge janusEdge = startVertex.addEdge(edge.getLabel(), endVertex);

            if (edge.getUid() != null) {
                janusEdge.property(GraphConstants.UID, edge.getUid());
            }

            if (edge.getProperties() != null) {
                edge.getProperties().forEach((key, value) -> {
                    if (value != null) {
                        janusEdge.property(key, value);
                    }
                });
            }

            tx.commit();
            return edge;
        } catch (Exception e) {
            log.error("Failed to add edge from {} to {}: {}", edge.getStartUid(), edge.getEndUid(), e.getMessage(), e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to add edge from " + edge.getStartUid() + " to " + edge.getEndUid(), e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            log.info("Edges collection is empty, skipping batch insert");
            return;
        }

        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            for (GraphEdge edge : edges) {
                Iterator<JanusGraphVertex> startVertices = tx.query().has(GraphConstants.UID, edge.getStartUid()).vertices().iterator();
                if (!startVertices.hasNext()) {
                    throw new GraphException("Start vertex not found with uid: " + edge.getStartUid());
                }
                JanusGraphVertex startVertex = startVertices.next();

                Iterator<JanusGraphVertex> endVertices = tx.query().has(GraphConstants.UID, edge.getEndUid()).vertices().iterator();
                if (!endVertices.hasNext()) {
                    throw new GraphException("End vertex not found with uid: " + edge.getEndUid());
                }
                JanusGraphVertex endVertex = endVertices.next();

                JanusGraphEdge janusEdge = startVertex.addEdge(edge.getLabel(), endVertex);
                if (edge.getProperties() != null) {
                    edge.getProperties().forEach((key, value) -> {
                        if (value != null) {
                            janusEdge.property(key, value);
                        }
                    });
                }
                if (janusEdge.id() != null) {
                    edge.setId(janusEdge.id().toString());
                }
            }
            tx.commit();
            log.info("Batch inserted {} edges", edges.size());
        } catch (Exception e) {
            log.error("Failed to batch add edges", e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to batch add edges", e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public GraphEdge updateEdge(GraphEdge edge) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            Iterator<JanusGraphEdge> edges = tx.query().has(GraphConstants.UID, edge.getUid()).edges().iterator();
            if (edges.hasNext()) {
                JanusGraphEdge janusEdge = edges.next();

                if (edge.getProperties() != null) {
                    edge.getProperties().forEach((key, value) -> {
                        if (value != null) {
                            janusEdge.property(key, value);
                        }
                    });
                }

                tx.commit();
                return edge;
            } else {
                throw new GraphException("Edge not found with uid: " + edge.getUid());
            }
        } catch (Exception e) {
            log.error("Failed to update edge: {}", edge, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to update edge: " + edge.getUid(), e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public boolean deleteEdge(GraphEdge edge) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            Iterator<JanusGraphEdge> edges = tx.query().has(GraphConstants.UID, edge.getUid()).edges().iterator();
            if (edges.hasNext()) {
                JanusGraphEdge janusEdge = edges.next();
                janusEdge.remove();
                tx.commit();
                return true;
            } else {
                throw new GraphException("Edge not found with uid: " + edge.getUid());
            }
        } catch (Exception e) {
            log.error("Failed to delete edge: {}", edge, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to delete edge: " + edge.getUid(), e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public GraphData query(String gremlinQuery) throws GraphException {
        log.info("Gremlin query: {}", gremlinQuery);
        if (StringUtils.isBlank(gremlinQuery)) {
            log.info("Gremlin query is blank, returning empty GraphData");
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
            if (result instanceof Iterable) {
                List<CacheVertex> vertexList = new ArrayList<>();
                List<CacheEdge> edgeList = new ArrayList<>();
                for (Object item : (Iterable<?>) result) {
                    buildVertexAndEdgeCollection(item, vertexList, edgeList, true);
                }
                return convertToGraphData(vertexList, edgeList);
            }
            if (result instanceof Element) {
                List<CacheVertex> vertexList = new ArrayList<>();
                List<CacheEdge> edgeList = new ArrayList<>();
                buildVertexAndEdgeCollection(result, vertexList, edgeList, true);
                return convertToGraphData(vertexList, edgeList);
            }
            log.warn("Gremlin query returned unexpected result type: {}", result.getClass().getName());
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

        // 如果只返回了顶点而没有边，补全顶点关联的边（仅当顶点标签相同时才添加对端顶点，避免污染结果）
        if (edgeList.isEmpty() && !vertexList.isEmpty()) {
            // 获取查询结果的顶点标签
            String queryVertexLabel = vertexList.get(0).label();
            List<CacheVertex> additionalVertices = new ArrayList<>();
            for (CacheVertex vertex : vertexList) {
                vertex.edges(Direction.BOTH).forEachRemaining(edge -> {
                    CacheEdge cacheEdge = (CacheEdge) edge;
                    if (!edgeList.contains(cacheEdge)) {
                        edgeList.add(cacheEdge);
                        // 只添加标签相同的对端顶点，避免引入不同标签的顶点
                        CacheVertex outV = (CacheVertex) cacheEdge.outVertex();
                        CacheVertex inV = (CacheVertex) cacheEdge.inVertex();
                        if (queryVertexLabel != null) {
                            if (queryVertexLabel.equals(outV.label())) {
                                additionalVertices.add(outV);
                            }
                            if (queryVertexLabel.equals(inV.label())) {
                                additionalVertices.add(inV);
                            }
                        }
                    }
                });
            }
            vertexList.addAll(additionalVertices);
        }

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
        } else if (element instanceof HashMap) {
           // todo
        }
    }

    private GraphData convertToGraphData(List<CacheVertex> vertexList, List<CacheEdge> edgeList) {
        GraphData graphData = new GraphData();
        List<GraphVertex> vertices = convertVertex(vertexList);
        Map<String, GraphVertex> idVertexMap = new HashMap<>();
        for (GraphVertex v : vertices) {
            if (v.getId() != null && !v.getId().isEmpty()) {
                idVertexMap.put(v.getId(), v);
            }
            if (v.getUid() != null && !v.getUid().isEmpty()) {
                idVertexMap.put(v.getUid(), v);
            }
        }

        List<GraphEdge> edges = convertEdge(edgeList);
        edges.forEach(edge -> {
            GraphVertex startVertex = findVertex(edge.getStartUid(), idVertexMap, vertices);
            GraphVertex endVertex = findVertex(edge.getEndUid(), idVertexMap, vertices);
            if (startVertex != null) {
                String uid = startVertex.getUid();
                if (uid != null && !uid.isEmpty()) {
                    edge.setStartUid(uid);
                }
                edge.setStartLabel(startVertex.getLabel());
            }
            if (endVertex != null) {
                String uid = endVertex.getUid();
                if (uid != null && !uid.isEmpty()) {
                    edge.setEndUid(uid);
                }
                edge.setEndLabel(endVertex.getLabel());
            }
        });

        graphData.setVertices(vertices);
        graphData.setEdges(edges);
        return graphData;
    }

    /**
     * 通过 uid 或 id 查找顶点，支持用 uid 或 JanusGraph 内部 ID 匹配
     */
    private GraphVertex findVertex(String vertexKey, Map<String, GraphVertex> idVertexMap, List<GraphVertex> vertices) {
        if (vertexKey == null || vertexKey.isEmpty()) {
            return null;
        }
        GraphVertex v = idVertexMap.get(vertexKey);
        if (v != null) {
            return v;
        }
        for (GraphVertex gv : vertices) {
            if (vertexKey.equals(gv.getId()) || vertexKey.equals(gv.getUid())) {
                return gv;
            }
        }
        return null;
    }

    /**
     * 转换成vertex
     *
     * @param detachedVertexList CacheVertex list
     * @return Multimap，以uid作为key，value为统一uid的集合。
     */
    private List<GraphVertex> convertVertex(List<CacheVertex> detachedVertexList) {
        List<GraphVertex> vertexList = Lists.newArrayList();
        Set<String> idSet = new HashSet<>();
        for (CacheVertex detachedVertex : detachedVertexList) {
            String id = valueOf(detachedVertex.id());
            if (idSet.contains(id)) {
                continue;
            } else {
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

            JanusGraphVertex outVertex = detachedEdge.outVertex();
            JanusGraphVertex inVertex = detachedEdge.inVertex();
            graphEdge.setStartUid(getVertexUid(outVertex));
            graphEdge.setEndUid(getVertexUid(inVertex));

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
     * 从TinkerPop顶点获取uid属性值，支持 detached vertex（从 CacheEdge 获取的顶点需刷新）
     */
    private String getVertexUid(Vertex vertex) {
        // 尝试直接从属性读取
        try {
            if (vertex.property(GraphConstants.UID).isPresent()) {
                Object uid = vertex.property(GraphConstants.UID).value();
                if (uid != null && !uid.toString().isEmpty()) {
                    return uid.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get uid directly, will try to refresh vertex: {}", e.getMessage());
        }

        // 对于 detached vertex（如 CacheEdge 的 incident vertex），从图数据库刷新获取
        try {
            Iterator<Vertex> refreshed = graph.vertices(vertex.id());
            if (refreshed.hasNext()) {
                Vertex freshVertex = refreshed.next();
                if (freshVertex.property(GraphConstants.UID).isPresent() && !freshVertex.property(GraphConstants.UID).value().toString().isEmpty()) {
                    return freshVertex.property(GraphConstants.UID).value().toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to refresh vertex for uid: {}", e.getMessage());
        }

        return vertex.id().toString();
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
            Iterator<JanusGraphVertex> vertices = tx.query().has(GraphConstants.UID, Contain.IN, vertexIds).vertices().iterator();
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

        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            // 查询边
            Iterator<JanusGraphEdge> edges = tx.query().has(GraphConstants.UID, org.janusgraph.core.attribute.Contain.IN, edgeIds).edges().iterator();
            List<GraphEdge> result = new ArrayList<>();
            while (edges.hasNext()) {
                JanusGraphEdge edge = edges.next();
                GraphEdge graphEdge = parseEdge(edge);
                result.add(graphEdge);
            }

            // 提交事务
            tx.commit();
            return result;
        } catch (Exception e) {
            log.error("Failed to get edges by ids: {}", edgeIds, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to get edges by ids", e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public GraphData expand(String vertexId, int depth) throws GraphException {
        String gremlinQuery = String.format("g.V().has('%s', '%s').repeat(bothE().bothV().simplePath()).times(%d).path()",
                GraphConstants.UID, vertexId, depth);
        log.debug("Executing expand query: {}", gremlinQuery);
        return query(gremlinQuery);
    }

    @Override
    public GraphData expand(String label, String property, String value, int depth, int limit) throws GraphException {
        String gremlinQuery = String.format(
                "g.V().hasLabel('%s').has('%s', '%s').repeat(bothE().bothV().simplePath()).times(%d).path().limit(%d)",
                label, property, value, depth, limit);
        log.debug("Executing expand by property query: {}", gremlinQuery);
        return query(gremlinQuery);
    }

    @Override
    public GraphData findPath(String startVertexId, String endVertexId, int maxDepth) throws GraphException {
        String gremlinQuery = String.format("g.V().has('%s', '%s').repeat(bothE().bothV().simplePath()).until(has('%s', '%s')).limit(1).path()",
                GraphConstants.UID, startVertexId, GraphConstants.UID, endVertexId);
        log.debug("Executing findPath query: {}", gremlinQuery);
        return query(gremlinQuery);
    }

    @Override
    public GraphData findPath(PathQuery pathQuery) throws GraphException {
        PathQuery.Condition start = pathQuery.getStartProperty();
        PathQuery.Condition end = pathQuery.getEndProperty();
        String gremlinQuery = String.format(
                "g.V().hasLabel('%s').has('%s', '%s')"
                        + ".repeat(bothE().inV().simplePath())"
                        + ".until(hasLabel('%s').has('%s', '%s').or().loops().is(%d))"
                        + ".hasLabel('%s').has('%s', '%s')"
                        + ".path().limit(%d)",
                start.getLabel(), start.getProperty(), start.getValue(),
                end.getLabel(), end.getProperty(), end.getValue(), pathQuery.getMaxDepth(),
                end.getLabel(), end.getProperty(), end.getValue(), pathQuery.getLimit());
        log.debug("Executing findPath by property query: {}", gremlinQuery);
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

        // 获取UID，优先使用uid属性，否则使用内部ID
        if (vertex.property(GraphConstants.UID).isPresent()) {
            graphVertex.setUid(vertex.property(GraphConstants.UID).value().toString());
        } else {
            graphVertex.setUid(vertex.id().toString());
        }
        // 获取ID
        graphVertex.setId(vertex.id().toString());
        // 获取标签
        graphVertex.setLabel(vertex.label());

        // 获取其他属性
        Map<String, Object> properties = new HashMap<>();
        vertex.keys().forEach(key -> {
            if (vertex.property(key).isPresent() && !GraphConstants.UID.equals(key)) {
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

        // 获取UID，优先使用uid属性，否则使用内部ID
        if (edge.property(GraphConstants.UID).isPresent()) {
            graphEdge.setUid((String) edge.property(GraphConstants.UID).value());
        } else {
            graphEdge.setUid(edge.id().toString());
        }

        // 获取标签
        graphEdge.setLabel(edge.label());

        // 获取起始和结束顶点的UID
        JanusGraphVertex outVertex = edge.outVertex();
        JanusGraphVertex inVertex = edge.inVertex();

        if (outVertex.property(GraphConstants.UID).isPresent()) {
            graphEdge.setStartUid((String) outVertex.property(GraphConstants.UID).value());
        }

        if (inVertex.property(GraphConstants.UID).isPresent()) {
            graphEdge.setEndUid((String) inVertex.property(GraphConstants.UID).value());
        }

        // 获取其他属性
        Map<String, Object> properties = new HashMap<>();
        edge.keys().forEach(key -> {
            if (edge.property(key).isPresent() && !GraphConstants.UID.equals(key)) {
                properties.put(key, edge.property(key).value());
            }
        });
        graphEdge.setProperties(properties);

        return graphEdge;
    }

    @Override
    public GraphSummary getSummary() throws GraphException {
        JanusGraphTransaction tx = null;
        GraphSummary summary = new GraphSummary();

        try {
            tx = graph.newTransaction();
            // 获取节点总数
            long nodeCount = tx.traversal().V().count().next();
            summary.setVertexCount((int) nodeCount);

            // 获取边总数
            long edgeCount = tx.traversal().E().count().next();
            summary.setEdgeCount((int) edgeCount);

            // 获取各标签节点数量统计
            Map<String, Integer> vertexLabelCount = new HashMap<>();
            tx.traversal().V().label().groupCount().next().forEach((label, count) -> {
                vertexLabelCount.put(label.toString(), count.intValue());
            });
            summary.setVertexLabelCount(vertexLabelCount);

            // 获取各类型边数量统计
            Map<String, Integer> edgeLabelCount = new HashMap<>();
            tx.traversal().E().label().groupCount().next().forEach((label, count) -> {
                edgeLabelCount.put(label.toString(), count.intValue());
            });
            summary.setEdgeLabelCount(edgeLabelCount);

            tx.commit();
            log.debug("Retrieved graph summary: {} vertices, {} edges", nodeCount, edgeCount);
            return summary;
        } catch (Exception e) {
            log.error("Failed to get graph summary from JanusGraph", e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            throw new GraphException("Failed to get graph summary from JanusGraph", e);
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public long countVertices(String label) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            long count = tx.traversal().V().hasLabel(label).count().next();
            tx.commit();
            return count;
        } catch (Exception e) {
            log.error("Failed to count vertices with label {} from JanusGraph", label, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            return 0L;
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public long countEdges(String label) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();
            long count = tx.traversal().E().hasLabel(label).count().next();
            tx.commit();
            return count;
        } catch (Exception e) {
            log.error("Failed to count edges with label {} from JanusGraph", label, e);
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            return 0L;
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }

    @Override
    public GraphVertex findVertex(String label, String property, String value) throws GraphException {
        JanusGraphTransaction tx = null;
        try {
            tx = graph.newTransaction();

            // 使用 Gremlin 查询，先按 uid 查找
            List<Vertex> vertices = tx.traversal().V().has(GraphConstants.UID, value).hasLabel(label).toList();
            if (CollectionUtils.isNotEmpty(vertices)) {
                JanusGraphVertex vertex = (JanusGraphVertex) vertices.get(0);
                tx.commit();
                return parseVertex(vertex);
            }

            // 按 label + property + value 查找
            List<Vertex> labelVertices = tx.traversal().V().hasLabel(label).has(property, value).toList();
            if (CollectionUtils.isNotEmpty(labelVertices)) {
                JanusGraphVertex vertex = (JanusGraphVertex) labelVertices.get(0);
                tx.commit();
                return parseVertex(vertex);
            }

            tx.commit();
            return null;
        } catch (Exception e) {
            log.warn("Failed to find vertex by property: label={}, property={}, value={}, error={}",
                    label, property, value, e.getMessage());
            if (tx != null && tx.isOpen()) {
                tx.rollback();
            }
            return null;
        } finally {
            if (tx != null && tx.isOpen()) {
                tx.close();
            }
        }
    }
}