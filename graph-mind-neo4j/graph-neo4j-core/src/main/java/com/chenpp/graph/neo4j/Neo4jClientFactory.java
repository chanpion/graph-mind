package com.chenpp.graph.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author April.Chen
 * @date 2024/5/21 10:03
 */
@Slf4j
public class Neo4jClientFactory {
    private static Map<String, Driver> CACHE = new ConcurrentHashMap<>();

    public static Driver createNeo4jDriver(Neo4jConf neo4jConf) {
        Driver driver = CACHE.get(neo4jConf.getGraphCode());
        if (driver != null) {
            try {
                driver.verifyConnectivity();
            } catch (Exception e) {
                driver = null;
                log.error("Neo4j连接异常", e);
            }
        }
        if (driver == null) {
            driver = GraphDatabase.driver(neo4jConf.getUri(), AuthTokens.basic(neo4jConf.getUsername(), neo4jConf.getPassword()));
            driver.verifyConnectivity();
            CACHE.put(neo4jConf.getGraphCode(), driver);
        }

        return driver;
    }
}
