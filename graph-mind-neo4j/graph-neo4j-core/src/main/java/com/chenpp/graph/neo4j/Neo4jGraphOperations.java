package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author April.Chen
 * @date 2025/7/14 09:46
 */
@Slf4j
public class Neo4jGraphOperations implements GraphOperations {
    private Neo4jConf neo4jConf;
    private Driver driver;

    public Neo4jGraphOperations(Neo4jConf neo4jConf, Driver driver) {
        this.neo4jConf = neo4jConf;
        this.driver = driver;
    }

    @Override
    public void createGraph(GraphConf graphConf) {
        log.info("Creating graph in Neo4j: {}", graphConf.getGraphCode());
        // Neo4j中数据库的创建通常由管理员完成，这里仅记录日志
    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {
        log.info("Dropping graph in Neo4j: {}", graphConf.getGraphCode());
        // Neo4j中数据库的删除通常由管理员完成，这里仅记录日志
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) {
        log.info("Listing graphs in Neo4j");
        // Neo4j中数据库列表的获取通常由管理员完成，这里返回空列表
        return new ArrayList<>();
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) {
        log.info("Applying schema to graph: {}", graphConf.getGraphCode());
        // Neo4j是无模式数据库，这里仅记录日志
        if (CollectionUtils.isNotEmpty(graphSchema.getIndexes())) {
            graphSchema.getIndexes().forEach(this::createIndex);
        }
    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        GraphSchema schema = new GraphSchema();

        try {
            // 获取节点标签信息
            List<GraphEntity> entities = getNodeLabels();
            schema.setEntities(entities);

            // 获取关系类型信息
            List<GraphRelation> relations = getRelationshipTypes();
            schema.setRelations(relations);

            // 获取索引信息
            List<GraphIndex> indexes = getIndexes();
            schema.setIndexes(indexes);

            log.debug("Retrieved schema from Neo4j: {} entities, {} relations, {} indexes",
                    entities.size(), relations.size(), indexes.size());
        } catch (Exception e) {
            log.error("Failed to get published schema from Neo4j", e);
            throw new GraphException("Failed to get published schema from Neo4j", e);
        }

        return schema;
    }

    public List<GraphEntity> getNodeLabels() throws GraphException {
        String cypher = "CALL db.labels()";
        List<GraphEntity> entities = new ArrayList<>();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                String label = result.next().get(0).asString();
                GraphEntity entity = new GraphEntity();
                entity.setLabel(label);
                entities.add(entity);
            }
        } catch (Exception e) {
            log.error("Failed to get node labels from Neo4j", e);
            throw new GraphException("Failed to get node labels", e);
        }

        log.debug("Retrieved {} node labels from Neo4j", entities.size());
        return entities;
    }

    public List<GraphRelation> getRelationshipTypes() throws GraphException {
        String cypher = "CALL db.relationshipTypes()";
        List<GraphRelation> relations = new ArrayList<>();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                String type = result.next().get(0).asString();
                GraphRelation relation = new GraphRelation();
                relation.setLabel(type);
                relations.add(relation);
            }
        } catch (Exception e) {
            log.error("Failed to get relationship types from Neo4j", e);
            throw new GraphException("Failed to get relationship types", e);
        }

        log.debug("Retrieved {} relationship types from Neo4j", relations.size());
        return relations;
    }

    public List<GraphIndex> getIndexes() throws GraphException {
        String cypher = "SHOW INDEXES";
        List<GraphIndex> indexes = new ArrayList<>();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                GraphIndex index = new GraphIndex();
                index.setName(result.next().get("name").asString());
                indexes.add(index);
            }
        } catch (Exception e) {
            log.error("Failed to get indexes from Neo4j", e);
            throw new GraphException("Failed to get indexes", e);
        }

        log.debug("Retrieved {} indexes from Neo4j", indexes.size());
        return indexes;
    }

    public void createIndex(GraphIndex graphIndex) {
        String cypher = CypherBuilder.buildCreateSingleIndex(graphIndex.getName(), graphIndex.getLabel(), graphIndex.getProperty());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            log.info("Created index: {}", graphIndex.getName());
            result.consume();
        } catch (Exception e) {
            log.error("Failed to create index: {}", graphIndex.getName(), e);
        }
    }


    public void dropIndex(String indexName) {
        String cypher = String.format("DROP INDEX %s", indexName);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            log.info("Dropped index: {}", indexName);
            result.consume();
        } catch (Exception e) {
            log.error("Failed to drop index: {}", indexName, e);
        }
    }
}