package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.neo4j.util.Neo4jUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/7/14 09:56
 */
@Slf4j
public class Neo4jGraphDataOperations implements GraphDataOperations {
    private final Neo4jConf neo4jConf;
    private final Driver driver;

    public Neo4jGraphDataOperations(Neo4jConf neo4jConf, Driver driver) {
        this.neo4jConf = neo4jConf;
        this.driver = driver;
    }

    @Override
    public GraphVertex addVertex(GraphVertex vertex) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            // 动态构建Cypher语句和参数
            String cypher = String.format("CREATE (n:%s {uid: $uid}) SET n += $properties RETURN n", vertex.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(vertex);
            // 执行写入操作
            Record record = session.executeWrite(tx -> tx.run(cypher, parameters).single());
            Node node = record.get(0).asNode();
            return Neo4jUtil.parseVertex(node);
        } catch (Exception e) {
            throw new GraphException("Failed to add vertex to Neo4j", e);
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH (n:%s {uid: $uid}) SET n += $properties RETURN n", vertex.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(vertex);
            Record record = session.executeWrite(tx -> tx.run(cypher, parameters).single());
            Node node = record.get(0).asNode();
            return Neo4jUtil.parseVertex(node);
        } catch (Exception e) {
            throw new GraphException("Failed to update vertex to Neo4j", e);
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            log.info("vertices is empty");
            return;
        }
        Map<String, List<GraphVertex>> labelVerticesMap = vertices.stream().collect(Collectors.groupingBy(GraphVertex::getLabel));
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            session.executeWrite(tx -> {
                labelVerticesMap.forEach((label, vertexList) -> {
                    String cypher = String.format("UNWIND $vertices AS row CREATE (n:%s {uid: row.uid}) SET n += row.properties", label);
                    Map<String, Object> parameters = Map.of("vertices", vertexList.stream().map(Neo4jUtil::convertToMap).collect(Collectors.toList()));
                    tx.run(cypher, parameters);
                });

                return null;
            });
        } catch (Exception e) {
            throw new GraphException("Failed to add vertices to Neo4j", e);
        }
    }

    @Override
    public void deleteVertex(GraphVertex vertex) throws GraphException {
        String cypher = String.format("MATCH (n:%s {uid: $uid}) DETACH DELETE n", vertex.getLabel());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of("uid", vertex.getUid()));
                result.consume(); // 消费结果以避免ClientException
                return null;
            });
        } catch (Exception e) {
            throw new GraphException("Failed to delete vertex", e);
        }
    }

    @Override
    public void addEdge(GraphEdge edge) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH (a {uid: $source}), (b {uid: $target}) CREATE (a)-[r:%s]->(b) SET r += $properties RETURN r",
                    edge.getLabel());
            Map<String, Object> parameters = Neo4jUtil.convertToMap(edge);
            session.executeWrite(tx -> tx.run(cypher, parameters).single());
        } catch (Exception e) {
            throw new GraphException("Failed to create relationship in Neo4j", e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            log.info("edges is empty");
            return;
        }

        Map<String, List<GraphEdge>> labelEdgesMap = edges.stream().collect(Collectors.groupingBy(e -> String.format("%s-%s-%s", e.getLabel(), e.getStartLabel(), e.getEndLabel())));
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            session.executeWrite(tx -> {
                labelEdgesMap.forEach((label, edgeList) -> {
                    String[] labelArr = label.split("-");
                    String cypher = String.format("UNWIND $edges AS edge MATCH (a:%s {uid: edge.startUid}), (b:%s {uid: edge.endUid}) CREATE (a)-[r:%s]->(b) SET r += edge.properties",
                            labelArr[1], labelArr[2], labelArr[0]);
                    log.info("cypher: {}", cypher);
                    Map<String, Object> parameters = Map.of("edges", edgeList.stream().map(Neo4jUtil::convertToMap).collect(Collectors.toList()));
                    Result rs = tx.run(cypher, parameters);
                    log.info("record: {}", rs.consume().counters());
                });

                return null;
            });
        } catch (Exception e) {
            throw new GraphException("Failed to add edges to Neo4j", e);
        }
    }

    @Override
    public int updateEdge(GraphEdge edge) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH (a {uid: $source})-[r:%s]->(b {uid: $target}) SET r += $properties",
                    edge.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(edge);
            int count = session.executeWrite(tx -> tx.run(cypher, parameters).consume().counters().propertiesSet());
            log.info("Updated {} relationships", count);
            return count;
        } catch (Exception e) {
            throw new GraphException("Failed to update relationship in Neo4j", e);
        }
    }

    @Override
    public int deleteEdge(GraphEdge edge) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH ()-[r:%s]->() WHERE r.uid=$uid  DELETE r", edge.getLabel());
            Map<String, Object> parameters = Map.of("uid", edge.getUid());
            int count = session.executeWrite(tx -> tx.run(cypher, parameters).consume().counters().relationshipsDeleted());
            log.info("Deleted {} relationships", count);
            return count;
        } catch (Exception e) {
            throw new GraphException("Failed to delete relationship in Neo4j", e);
        }
    }

    public GraphData query(String cypher) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher);
                GraphData graphData = new GraphData();
                Map<String, GraphVertex> elementIdVertexMap = new HashMap<>();
                Set<String> edgeIds = new HashSet<>();
                result.list().forEach(record -> {
                    record.values().forEach(value -> {
                        if (value instanceof PathValue) {
                            Path path = value.asPath();
                            path.nodes().forEach(node -> {
                                if (!elementIdVertexMap.containsKey(node.elementId())) {
                                    GraphVertex vertex = Neo4jUtil.parseVertex(node);
                                    graphData.addVertex(vertex);
                                    elementIdVertexMap.put(node.elementId(), vertex);
                                }
                            });
                            path.relationships().forEach(relationship -> {
                                if (!edgeIds.contains(relationship.elementId())) {
                                    GraphEdge edge = Neo4jUtil.parseEdge(relationship);
                                    graphData.addEdge(edge);
                                    edgeIds.add(relationship.elementId());
                                }
                            });
                        }
                        if (value instanceof NodeValue) {
                            Node node = value.asNode();
                            if (!elementIdVertexMap.containsKey(node.elementId())) {
                                GraphVertex vertex = Neo4jUtil.parseVertex(node);
                                graphData.addVertex(vertex);
                                elementIdVertexMap.put(node.elementId(), vertex);
                            }
                        }
                        if (value instanceof RelationshipValue) {
                            Relationship relationship = value.asRelationship();
                            if (!edgeIds.contains(relationship.elementId())) {
                                GraphEdge edge = Neo4jUtil.parseEdge(relationship);
                                graphData.addEdge(edge);
                                edgeIds.add(relationship.elementId());
                            }
                        }
                    });

                });
                graphData.getEdges().forEach(edge -> {
                    GraphVertex start = elementIdVertexMap.get(edge.getStartUid());
                    edge.setStartUid(start.getUid());
                    edge.setStartLabel(start.getLabel());
                    GraphVertex end = elementIdVertexMap.get(edge.getEndUid());
                    edge.setEndUid(end.getUid());
                    edge.setEndLabel(end.getLabel());
                });
                return graphData;
            });

        } catch (Exception e) {
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }

    }

    @Override
    public GraphData expand(String nodeId, int depth) throws GraphException {
        String cypher = "MATCH (n {uid: $nodeId})-[*1..$depth]-(m) " +
                "RETURN n, m, relationships(path) AS rels";
        try (Session session = driver.session()) {
            TransactionWork<GraphData> txWork = tx -> {
                Result result = tx.run(cypher, Values.parameters("nodeId", nodeId, "depth", depth));
                return parseResult(result);
            };
            return session.readTransaction(txWork);
        } catch (Exception e) {
            log.error("Failed to expand node: {}", nodeId, e);
            throw new GraphException("Failed to expand node: " + nodeId, e);
        }
    }

    @Override
    public GraphData findPath(String startNodeId, String endNodeId, int maxDepth) throws GraphException {
        String cypher = "MATCH p = shortestPath((a {uid: $startNodeId})-[*..$maxDepth]-(b {uid: $endNodeId})) " +
                "RETURN p";
        try (Session session = driver.session()) {
            TransactionWork<GraphData> txWork = tx -> {
                Result result = tx.run(cypher, Values.parameters("startNodeId", startNodeId, "endNodeId", endNodeId, "maxDepth", maxDepth));
                return parseResult(result);
            };
            return session.readTransaction(txWork);
        } catch (Exception e) {
            log.error("Failed to find path from {} to {}", startNodeId, endNodeId, e);
            throw new GraphException("Failed to find path from " + startNodeId + " to " + endNodeId, e);
        }
    }

    private GraphData parseResult(Result result) {
        GraphData graphData = new GraphData();
        Set<GraphVertex> vertices = new HashSet<>();
        Set<GraphEdge> edges = new HashSet<>();

        while (result.hasNext()) {
            Record record = result.next();
            List<Value> values = record.values();
            for (Value value : values) {
                // 解析节点
                if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.NODE())) {
                    Node node = value.asNode();
                    GraphVertex vertex = convertNodeToVertex(node);
                    vertices.add(vertex);
                }
                // 解析关系
                else if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP())) {
                    Relationship relationship = value.asRelationship();
                    GraphEdge edge = convertRelationshipToEdge(relationship);
                    edges.add(edge);
                }
                // 解析路径
                else if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.PATH())) {
                    InternalPath path = (InternalPath) value.asPath();
                    for (Node node : path.nodes()) {
                        GraphVertex vertex = convertNodeToVertex(node);
                        vertices.add(vertex);
                    }
                    for (Relationship relationship : path.relationships()) {
                        GraphEdge edge = convertRelationshipToEdge(relationship);
                        edges.add(edge);
                    }
                }
            }
        }

        graphData.setVertices(new ArrayList<>(vertices));
        graphData.setEdges(new ArrayList<>(edges));
        return graphData;
    }

    private GraphVertex convertNodeToVertex(Node node) {
        GraphVertex vertex = new GraphVertex();
        vertex.setId(String.valueOf(node.id()));
        vertex.setUid((String) node.get("uid").asObject());
        vertex.setLabel(String.join(":", node.labels()));
        Map<String, Object> properties = new HashMap<>();
        node.keys().forEach(key -> properties.put(key, node.get(key).asObject()));
        vertex.setProperties(properties);
        return vertex;
    }

    private GraphEdge convertRelationshipToEdge(Relationship relationship) {
        GraphEdge edge = new GraphEdge();
        edge.setId(String.valueOf(relationship.id()));
        edge.setUid(String.valueOf(relationship.id()));
        edge.setLabel(relationship.type());
        edge.setStartUid(String.valueOf(relationship.startNodeId()));
        edge.setEndUid(String.valueOf(relationship.endNodeId()));
        Map<String, Object> properties = new HashMap<>();
        relationship.keys().forEach(key -> properties.put(key, relationship.get(key).asObject()));
        edge.setProperties(properties);
        return edge;
    }

    @Override
    public GraphSummary getSummary() throws GraphException {
        GraphSummary summary = new GraphSummary();
        
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            // 获取节点总数
            String nodeCountCypher = "MATCH (n) RETURN count(n) AS count";
            Result nodeCountResult = session.run(nodeCountCypher);
            if (nodeCountResult.hasNext()) {
                summary.setVertexCount(nodeCountResult.next().get("count").asInt());
            }
            
            // 获取边总数
            String edgeCountCypher = "MATCH ()-[r]->() RETURN count(r) AS count";
            Result edgeCountResult = session.run(edgeCountCypher);
            if (edgeCountResult.hasNext()) {
                summary.setEdgeCount(edgeCountResult.next().get("count").asInt());
            }
            
            // 获取各标签节点数量统计
            String nodeLabelCountCypher = "MATCH (n) RETURN DISTINCT labels(n) AS labels, count(n) AS count";
            Result nodeLabelResult = session.run(nodeLabelCountCypher);
            Map<String, Integer> vertexLabelCount = new HashMap<>();
            while (nodeLabelResult.hasNext()) {
                Record record = nodeLabelResult.next();
                String label = String.join(":", record.get("labels").asList(Value::asString));
                int count = record.get("count").asInt();
                vertexLabelCount.put(label, count);
            }
            summary.setVertexLabelCount(vertexLabelCount);
            
            // 获取各类型边数量统计
            String edgeLabelCountCypher = "MATCH ()-[r]->() RETURN type(r) AS type, count(r) AS count";
            Result edgeLabelResult = session.run(edgeLabelCountCypher);
            Map<String, Integer> edgeLabelCount = new HashMap<>();
            while (edgeLabelResult.hasNext()) {
                Record record = edgeLabelResult.next();
                String type = record.get("type").asString();
                int count = record.get("count").asInt();
                edgeLabelCount.put(type, count);
            }
            summary.setEdgeLabelCount(edgeLabelCount);
            
            return summary;
        } catch (Exception e) {
            throw new GraphException("Failed to get graph summary from Neo4j", e);
        }
    }
}