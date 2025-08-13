package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphSchema;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

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
