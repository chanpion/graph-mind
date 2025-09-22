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
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            // 动态构建Cypher语句和参数
            String cypher = String.format("CREATE (n:%s {uid: $uid}) SET n += $properties RETURN n", vertex.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(vertex);
            // 执行写入操作
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
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH (n:%s {uid: $uid}) SET n += $properties RETURN n", vertex.getLabel());

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
            log.info("Vertices collection is empty, skipping batch insert");
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
            log.error("Failed to add vertices to Neo4j, vertex count: {}", vertices.size(), e);
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
            log.error("Failed to delete vertex from Neo4j: {}", vertex, e);
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
            log.error("Failed to create relationship in Neo4j: {}", edge, e);
            throw new GraphException("Failed to create relationship in Neo4j", e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            log.info("Edges collection is empty, skipping batch insert");
            return;
        }

        Map<String, List<GraphEdge>> labelEdgesMap = edges.stream().collect(Collectors.groupingBy(e -> String.format("%s-%s-%s", e.getLabel(), e.getStartLabel(), e.getEndLabel())));
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            session.executeWrite(tx -> {
                labelEdgesMap.forEach((label, edgeList) -> {
                    String[] labelArr = label.split("-");
                    String cypher = String.format("UNWIND $edges AS edge MATCH (a:%s {uid: edge.startUid}), (b:%s {uid: edge.endUid}) CREATE (a)-[r:%s]->(b) SET r += edge.properties",
                            labelArr[1], labelArr[2], labelArr[0]);
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
    public int updateEdge(GraphEdge edge) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH (a {uid: $source})-[r:%s]->(b {uid: $target}) SET r += $properties",
                    edge.getLabel());

            Map<String, Object> parameters = Neo4jUtil.convertToMap(edge);
            int count = session.executeWrite(tx -> tx.run(cypher, parameters).consume().counters().propertiesSet());
            log.debug("Updated {} relationship properties", count);
            return count;
        } catch (Exception e) {
            log.error("Failed to update relationship in Neo4j: {}", edge, e);
            throw new GraphException("Failed to update relationship in Neo4j", e);
        }
    }

    @Override
    public int deleteEdge(GraphEdge edge) throws GraphException {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            String cypher = String.format("MATCH ()-[r:%s]->() WHERE r.uid=$uid  DELETE r", edge.getLabel());
            Map<String, Object> parameters = Map.of("uid", edge.getUid());
            int count = session.executeWrite(tx -> tx.run(cypher, parameters).consume().counters().relationshipsDeleted());
            log.debug("Deleted {} relationships", count);
            return count;
        } catch (Exception e) {
            log.error("Failed to delete relationship in Neo4j: {}", edge, e);
            throw new GraphException("Failed to delete relationship in Neo4j", e);
        }
    }

    public GraphData query(String cypher) throws GraphException {
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
    public GraphData expand(String nodeId, int depth) throws GraphException {
        String cypher = "MATCH p = (n {uid: $nodeId})-[*1..$depth]-(m)  RETURN P";
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Values.parameters("nodeId", nodeId, "depth", depth));
                return Neo4jUtil.parseResult(result);
            });
        } catch (Exception e) {
            log.error("Failed to expand node: {}", nodeId, e);
            throw new GraphException("Failed to expand node: " + nodeId, e);
        }
    }

    @Override
    public GraphData findPath(String startNodeId, String endNodeId, int maxDepth) throws GraphException {
        String cypher = "MATCH p = shortestPath((a {uid: $startNodeId})-[*..$maxDepth]-(b {uid: $endNodeId})) RETURN p";
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Values.parameters("startNodeId", startNodeId, "endNodeId", endNodeId, "maxDepth", maxDepth));
                return Neo4jUtil.parseResult(result);
            });
        } catch (Exception e) {
            log.error("Failed to find path from {} to {}", startNodeId, endNodeId, e);
            throw new GraphException("Failed to find path from " + startNodeId + " to " + endNodeId, e);
        }
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
}