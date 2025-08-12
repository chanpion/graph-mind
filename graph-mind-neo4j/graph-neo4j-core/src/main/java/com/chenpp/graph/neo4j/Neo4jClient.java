package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import org.neo4j.driver.Driver;

/**
 * @author April.Chen
 * @date 2025/7/14 09:44
 */
public class Neo4jClient implements GraphClient {
    private Neo4jConf neo4jConf;
    private Driver driver;

    public Neo4jClient(Neo4jConf neo4jConf) {
        this.neo4jConf = neo4jConf;
        this.driver = Neo4jClientFactory.createNeo4jDriver(neo4jConf);
    }

    @Override
    public GraphOperations opsForGraph() {
        return null;
    }

    @Override
    public GraphDataOperations opsForGraphData() {
        return new Neo4jGraphDataOperations(neo4jConf, driver);
    }
}
