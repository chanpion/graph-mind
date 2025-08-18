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

    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {

    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) {
        return null;
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) {

    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        GraphSchema schema = new GraphSchema();
        
        // 获取节点标签信息
        List<GraphEntity> entities = getNodeLabels();
        schema.setEntities(entities);
        
        // 获取关系类型信息
        List<GraphRelation> relations = getRelationshipTypes();
        schema.setRelations(relations);
        
        // 获取索引信息
        List<GraphIndex> indexes = getIndexes();
        schema.setIndexes(indexes);
        
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
            throw new GraphException("Failed to get node labels", e);
        }
        
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
            throw new GraphException("Failed to get relationship types", e);
        }
        
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
            throw new GraphException("Failed to get indexes", e);
        }
        
        return indexes;
    }

    public void createIndex(GraphIndex graphIndex) {
        String cypher = CypherBuilder.buildCreateSingleIndex(graphIndex.getName(), graphIndex.getLabel(), graphIndex.getProperty());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            log.info("create index result: {}", result);
        }
    }


    public void dropIndex(String indexName) {
        String cypher = String.format("DROP INDEX %s", indexName);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            log.info("drop index result: {}", result);
        }
    }
}