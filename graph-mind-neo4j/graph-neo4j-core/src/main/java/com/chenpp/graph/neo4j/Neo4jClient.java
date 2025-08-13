package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;

/**
 * @author April.Chen
 * @date 2025/7/14 09:44
 */
@Slf4j
public class Neo4jClient implements GraphClient {
    private final Neo4jConf neo4jConf;
    private final Driver driver;

    public Neo4jClient(Neo4jConf neo4jConf) {
        this.neo4jConf = neo4jConf;
        if (neo4jConf.getGraphCode() == null) {
            neo4jConf.setGraphCode("neo4j");
        }
        this.driver = Neo4jClientFactory.createNeo4jDriver(neo4jConf);
    }

    @Override
    public GraphOperations opsForGraph() {
        return new Neo4jGraphOperations(neo4jConf, driver);
    }

    @Override
    public GraphDataOperations opsForGraphData() {
        return new Neo4jGraphDataOperations(neo4jConf, driver);
    }

    @Override
    public boolean checkConnection() {
        try {
            driver.verifyConnectivity();
            log.info("Neo4j连接成功:{}", neo4jConf);
        } catch (Exception e) {
            log.error("Neo4j连接异常", e);
            return false;
        }
        return true;
    }
}
