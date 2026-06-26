package com.chenpp.graph.admin.config;

import com.chenpp.graph.neo4j.Neo4jClientFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ShutdownConfig {

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down graph database drivers...");
        Neo4jClientFactory.closeAllDrivers();
        log.info("Graph database drivers shutdown complete");
    }
}
