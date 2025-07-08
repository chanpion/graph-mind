package com.chenpp.graph.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

/**
 * @author April.Chen
 * @date 2024/5/21 10:03
 */
public class Neo4jClientFactory {

    public static Driver createNeo4jDriver(Neo4jConf neo4jConf) {
        Driver driver = GraphDatabase.driver(neo4jConf.getUri(), AuthTokens.basic(neo4jConf.getUsername(), neo4jConf.getPassword()));
        driver.verifyConnectivity();
        return driver;
    }
}
