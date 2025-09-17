package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphDatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author April.Chen
 * @date 2024/5/21 10:03
 */
@Slf4j
public class Neo4jClientFactory {
    private static final Map<String, Driver> CACHE = new ConcurrentHashMap<>();

    public static Driver createNeo4jDriver(Neo4jConf neo4jConf) {
        if (neo4jConf == null) {
            log.error("Neo4jConf is null");
            throw new IllegalArgumentException("Neo4jConf cannot be null");
        }

        if (!neo4jConf.isValid()) {
            log.error("Neo4jConf is invalid: {}", neo4jConf);
            throw new IllegalArgumentException("Invalid Neo4jConf: " + neo4jConf);
        }

        String cacheKey = generateCacheKey(neo4jConf);
        Driver driver = CACHE.get(cacheKey);

        if (driver != null) {
            try {
                driver.verifyConnectivity();
                log.debug("Reusing existing Neo4j driver for: {}", neo4jConf.getUri());
            } catch (Exception e) {
                log.warn("Existing Neo4j driver is not connected, creating new one: {}", e.getMessage());
                driver = null;
                CACHE.remove(cacheKey);
            }
        }

        if (driver == null) {
            try {
                Config config = Config.builder()
                        .withConnectionTimeout(neo4jConf.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                        .withMaxConnectionPoolSize(neo4jConf.getMaxConnectionPoolSize())
                        .build();

                driver = GraphDatabase.driver(
                        neo4jConf.getUri(),
                        AuthTokens.basic(neo4jConf.getUsername(), neo4jConf.getPassword()),
                        config
                );

                driver.verifyConnectivity();
                CACHE.put(cacheKey, driver);
                log.info("Created new Neo4j driver for: {}", neo4jConf.getUri());
            } catch (Exception e) {
                log.error("Failed to create Neo4j driver for: {}", neo4jConf.getUri(), e);
                throw new GraphDatabaseException(ErrorCode.CONNECTION_FAILED, e);
            }
        }

        return driver;
    }

    private static String generateCacheKey(Neo4jConf conf) {
        return conf.getUri() + "|" + conf.getUsername() + "|" + conf.getGraphCode();
    }

    /**
     * 关闭并清理所有缓存的驱动程序
     */
    public static void closeAllDrivers() {
        CACHE.values().forEach(driver -> {
            try {
                driver.close();
                log.info("Closed Neo4j driver");
            } catch (Exception e) {
                log.warn("Error closing Neo4j driver", e);
            }
        });
        CACHE.clear();
        log.info("Cleared Neo4j driver cache");
    }
}