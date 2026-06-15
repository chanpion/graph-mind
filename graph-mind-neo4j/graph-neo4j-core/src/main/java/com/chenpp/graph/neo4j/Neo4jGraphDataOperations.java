package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.neo4j.util.Neo4jUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Neo4j数据操作实现
 *
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
        log.info("Adding vertex with label: {}, uid: {}", vertex.getLabel(), vertex.getUid());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("CREATE (n:%s {uid: $uid}) SET n += $properties RETURN n", vertex.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(vertex);
            Record record = session.executeWrite(tx -> tx.run(cypher, parameters).single());
            Node node = record.get(0).asNode();
            return Neo4jUtil.parseVertex(node);
        } catch (Exception e) {
            log.error("Failed to add vertex to Neo4j: {}", vertex, e);
            throw new GraphException("Failed to add vertex to Neo4j", e);
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        log.info("Updating vertex with label: {}, uid: {}", vertex.getLabel(), vertex.getUid());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format(
                    "MATCH (n:%s) WHERE n.uid = $uid OR elementId(n) = $uid SET n += $properties, n.uid = $uid RETURN n",
                    vertex.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(vertex);
            Record record = session.executeWrite(tx -> tx.run(cypher, parameters).single());
            Node node = record.get(0).asNode();
            return Neo4jUtil.parseVertex(node);
        } catch (Exception e) {
            log.error("Failed to update vertex in Neo4j: {}", vertex, e);
            throw new GraphException("Failed to update vertex in Neo4j", e);
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            log.warn("Vertices collection is empty, skipping batch insert");
            return;
        }
        log.info("Batch adding {} vertices", vertices.size());
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
            log.error("Failed to add vertices to Neo4j, vertex count: {}", vertices.size(), e);
            throw new GraphException("Failed to add vertices to Neo4j", e);
        }
    }

    @Override
    public boolean deleteVertex(GraphVertex vertex) throws GraphException {
        log.info("Deleting vertex with label: {}, uid: {}", vertex.getLabel(), vertex.getUid());
        String cypher = String.format("MATCH (n:%s) WHERE n.uid = $uid OR elementId(n) = $uid DETACH DELETE n", vertex.getLabel());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            int deleted = session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of("uid", vertex.getUid()));
                return result.consume().counters().nodesDeleted();
            });
            return deleted > 0;
        } catch (Exception e) {
            log.error("Failed to delete vertex from Neo4j: {}", vertex, e);
            throw new GraphException("Failed to delete vertex", e);
        }
    }

    @Override
    public GraphEdge addEdge(GraphEdge edge) throws GraphException {
        log.info("Adding edge with label: {}, startUid: {}, endUid: {}", edge.getLabel(), edge.getStartUid(), edge.getEndUid());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format(
                    "MATCH (a) WHERE a.uid = $startUid OR elementId(a) = $startUid "
                    + "MATCH (b) WHERE b.uid = $endUid OR elementId(b) = $endUid "
                    + "CREATE (a)-[r:%s]->(b) SET r += $properties RETURN r",
                    edge.getLabel());
            Map<String, Object> parameters = Neo4jUtil.convertToMap(edge);
            session.executeWrite(tx -> tx.run(cypher, parameters).single());
            return edge;
        } catch (Exception e) {
            log.error("Failed to create relationship in Neo4j: {}", edge, e);
            throw new GraphException("Failed to create relationship in Neo4j", e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            log.warn("Edges collection is empty, skipping batch insert");
            return;
        }
        log.info("Batch adding {} edges", edges.size());
        Map<String, List<GraphEdge>> labelEdgesMap = edges.stream().collect(Collectors.groupingBy(e -> String.format("%s-%s-%s", e.getLabel(), e.getStartLabel(), e.getEndLabel())));
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            session.executeWrite(tx -> {
                labelEdgesMap.forEach((label, edgeList) -> {
                    String[] labelArr = label.split("-");
                    String cypher = String.format("UNWIND $edges AS edge "
                            + "MATCH (a) WHERE a.uid = edge.startUid OR elementId(a) = edge.startUid "
                            + "MATCH (b) WHERE b.uid = edge.endUid OR elementId(b) = edge.endUid "
                            + "CREATE (a)-[r:%s]->(b) SET r += edge.properties",
                            labelArr[0]);
                    log.info("Executing cypher: {}", cypher);
                    Map<String, Object> parameters = Map.of("edges", edgeList.stream().map(Neo4jUtil::convertToMap).collect(Collectors.toList()));
                    Result rs = tx.run(cypher, parameters);
                    log.info("Created {} relationships", rs.consume().counters().relationshipsCreated());
                });

                return null;
            });
        } catch (Exception e) {
            log.error("Failed to add edges to Neo4j, edge count: {}", edges.size(), e);
            throw new GraphException("Failed to add edges to Neo4j", e);
        }
    }

    @Override
    public GraphEdge updateEdge(GraphEdge edge) throws GraphException {
        log.info("Updating edge with label: {}, uid: {}", edge.getLabel(), edge.getUid());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format(
                    "MATCH ()-[r:%s]->() WHERE r.uid = $uid OR elementId(r) = $uid SET r += $properties, r.uid = $uid RETURN r",
                    edge.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(edge);
            session.executeWrite(tx -> tx.run(cypher, parameters).consume());
            return edge;
        } catch (Exception e) {
            log.error("Failed to update relationship in Neo4j: {}", edge, e);
            throw new GraphException("Failed to update relationship in Neo4j", e);
        }
    }

    @Override
    public boolean deleteEdge(GraphEdge edge) throws GraphException {
        log.info("Deleting edge with label: {}, uid: {}", edge.getLabel(), edge.getUid());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH ()-[r:%s]->() WHERE r.uid=$uid OR elementId(r)=$uid DELETE r", edge.getLabel());
            Map<String, Object> parameters = Map.of("uid", edge.getUid());
            int count = session.executeWrite(tx -> tx.run(cypher, parameters).consume().counters().relationshipsDeleted());
            return count > 0;
        } catch (Exception e) {
            log.error("Failed to delete relationship in Neo4j: {}", edge, e);
            throw new GraphException("Failed to delete relationship in Neo4j", e);
        }
    }

    public GraphData query(String cypher) throws GraphException {
        log.info("Executing Cypher query: {}", cypher);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher);
                return Neo4jUtil.parseResult(result);
            });

        } catch (Exception e) {
            log.error("Failed to execute query in Neo4j: {}", cypher, e);
            throw new GraphException(ErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    @Override
    public GraphData expand(String vertexId, int depth) throws GraphException {
        log.info("Expanding vertex: {}, depth: {}", vertexId, depth);
        String cypher = String.format("MATCH p = (n)-[*1..%d]-(m) WHERE n.uid = $vertexId OR elementId(n) = $vertexId RETURN p", depth);
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Values.parameters("vertexId", vertexId));
                return Neo4jUtil.parseResult(result);
            });
        } catch (Exception e) {
            log.error("Failed to expand vertex: {}", vertexId, e);
            throw new GraphException("Failed to expand vertex: " + vertexId, e);
        }
    }

    @Override
    public GraphData findPath(String startVertexId, String endVertexId, int maxDepth) throws GraphException {
        log.info("Finding path from: {} to: {}, maxDepth: {}", startVertexId, endVertexId, maxDepth);
        String cypher = String.format(
                "MATCH p = shortestPath((a {uid: $startVertexId})-[*..%d]-(b {uid: $endVertexId})) RETURN p", maxDepth);
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Values.parameters("startVertexId", startVertexId, "endVertexId", endVertexId));
                return Neo4jUtil.parseResult(result);
            });
        } catch (Exception e) {
            log.error("Failed to find path from {} to {}", startVertexId, endVertexId, e);
            throw new GraphException("Failed to find path from " + startVertexId + " to " + endVertexId, e);
        }
    }


    @Override
    public GraphSummary getSummary() throws GraphException {
        log.info("Getting graph summary");
        GraphSummary summary = new GraphSummary();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String nodeCountCypher = "MATCH (n) RETURN count(n) AS count";
            Result nodeCountResult = session.run(nodeCountCypher);
            if (nodeCountResult.hasNext()) {
                summary.setVertexCount(nodeCountResult.next().get("count").asInt());
            }

            String edgeCountCypher = "MATCH ()-[r]->() RETURN count(r) AS count";
            Result edgeCountResult = session.run(edgeCountCypher);
            if (edgeCountResult.hasNext()) {
                summary.setEdgeCount(edgeCountResult.next().get("count").asInt());
            }

            // 获取各标签节点数量统计
            String nodeLabelCountCypher = "MATCH (n) RETURN DISTINCT labels(n) AS labels, count(n) AS count";
            Result nodeLabelResult = session.run(nodeLabelCountCypher);

            Map<String, Integer> vertexLabelCount = nodeLabelResult.stream()
                    .collect(Collectors.toMap(r -> String.join(",", r.get("labels").asList(Value::asString)),
                            r -> r.get("count").asInt()));
            summary.setVertexLabelCount(vertexLabelCount);

            // 获取各类型边数量统计
            String edgeLabelCountCypher = "MATCH ()-[r]->() RETURN type(r) AS type, count(r) AS count";
            Result edgeLabelResult = session.run(edgeLabelCountCypher);
            Map<String, Integer> edgeLabelCount = edgeLabelResult.stream().collect(Collectors.toMap(
                    r -> r.get("type").asString(),
                    r -> r.get("count").asInt()
            ));
            summary.setEdgeLabelCount(edgeLabelCount);

            return summary;
        } catch (Exception e) {
            log.error("Failed to get graph summary from Neo4j", e);
            throw new GraphException("Failed to get graph summary from Neo4j", e);
        }
    }

    @Override
    public long countVertices(String label) throws GraphException {
        log.info("Counting vertices with label: {}", label);
        String cypher = String.format("MATCH (n:`%s`) RETURN count(n) AS count", label);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher);
                if (result.hasNext()) {
                    return result.next().get("count").asLong();
                }
                return 0L;
            });
        } catch (Exception e) {
            log.error("Failed to count vertices with label {} from Neo4j", label, e);
            throw new GraphException("Failed to count vertices", e);
        }
    }

    @Override
    public long countEdges(String label) throws GraphException {
        log.info("Counting edges with label: {}", label);
        String cypher = String.format("MATCH ()-[r:`%s`]->() RETURN count(r) AS count", label);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher);
                if (result.hasNext()) {
                    return result.next().get("count").asLong();
                }
                return 0L;
            });
        } catch (Exception e) {
            log.error("Failed to count edges with label {} from Neo4j", label, e);
            throw new GraphException("Failed to count edges", e);
        }
    }
}