package com.chenpp.graph.janus.util;

import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.janus.CassandraConf;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.OptionsMap;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/8/18 17:05
 */
@Slf4j
public class CassandraClient {

    private final CqlSession session;

    public CassandraClient(CassandraConf cassandraConf) {
        if (cassandraConf == null) {
            throw new IllegalArgumentException("CassandraConf cannot be null");
        }
        
        try {
            DriverConfigLoader loader = DriverConfigLoader.fromMap(OptionsMap.driverDefaults());
            this.session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(cassandraConf.getHostname(), cassandraConf.getPort()))
                    .withLocalDatacenter("datacenter1")
                    .withAuthCredentials(cassandraConf.getUsername(), cassandraConf.getPassword())
                    .withConfigLoader(loader).build();
            log.info("Successfully created Cassandra session to {}:{}", cassandraConf.getHostname(), cassandraConf.getPort());
        } catch (Exception e) {
            log.error("Failed to create Cassandra session to {}:{} with user: {}", 
                    cassandraConf.getHostname(), cassandraConf.getPort(), cassandraConf.getUsername(), e);
            throw new GraphException("Failed to create Cassandra session", e);
        }
    }

    /**
     * 创建 keyspace
     *
     * @param keyspaceName      keyspace 名称
     * @param strategy          复制策略 (SimpleStrategy/NetworkTopologyStrategy)
     * @param replicationConfig 复制配置
     * @param durableWrites     是否启用持久化写入
     */
    public void createKeyspace(String keyspaceName,
                               String strategy,
                               Map<String, Object> replicationConfig,
                               boolean durableWrites) {
        try {
            String replicationConfigStr = buildReplicationConfig(strategy, replicationConfig);

            String query = String.format(
                    "CREATE KEYSPACE IF NOT EXISTS %s " +
                            "WITH replication = %s " +
                            "AND durable_writes = %s",
                    keyspaceName, replicationConfigStr, durableWrites);

            executeQuery(query);
            log.info("Created keyspace: {}", keyspaceName);
        } catch (Exception e) {
            log.error("Failed to create keyspace: {}", keyspaceName, e);
            throw new GraphException("Failed to create keyspace: " + keyspaceName, e);
        }
    }

    /**
     * 删除 keyspace
     *
     * @param keyspaceName keyspace 名称
     */
    public void dropKeyspace(String keyspaceName) {
        try {
            String query = String.format("DROP KEYSPACE IF EXISTS %s", keyspaceName);
            executeQuery(query);
            log.info("Dropped keyspace: {}", keyspaceName);
        } catch (Exception e) {
            log.error("Failed to drop keyspace: {}", keyspaceName, e);
            throw new GraphException("Failed to drop keyspace: " + keyspaceName, e);
        }
    }

    /**
     * 检查 keyspace 是否存在
     *
     * @param keyspaceName keyspace 名称
     * @return 是否存在
     */
    public boolean keyspaceExists(String keyspaceName) {
        try {
            String query = String.format(
                    "SELECT keyspace_name FROM system_schema.keyspaces " +
                            "WHERE keyspace_name = '%s'", keyspaceName);

            return !session.execute(SimpleStatement.newInstance(query)).all().isEmpty();
        } catch (Exception e) {
            log.error("Failed to check if keyspace exists: {}", keyspaceName, e);
            throw new GraphException("Failed to check if keyspace exists: " + keyspaceName, e);
        }
    }

    private String buildReplicationConfig(String strategy, Map<String, Object> replicationConfig) {
        StringBuilder sb = new StringBuilder("{'class': '")
                .append(strategy).append("'");

        replicationConfig.forEach((k, v) ->
                sb.append(", '").append(k).append("': ").append(v));

        sb.append("}");
        return sb.toString();
    }


    private void executeQuery(String query) {
        try {
            session.execute(SimpleStatement.newInstance(query));
        } catch (Exception e) {
            log.error("Failed to execute query: {}", query, e);
            throw new GraphException("Failed to execute query: " + query, e);
        }
    }

    public void close() {
        if (session != null) {
            try {
                session.close();
                log.info("Closed Cassandra session");
            } catch (Exception e) {
                log.warn("Error closing Cassandra session", e);
            }
        }
    }

    public static void main(String[] args) {
        CassandraConf cassandraConf = new CassandraConf();
        cassandraConf.setHostname("10.58.12.60");
        cassandraConf.setPort(9042);
        cassandraConf.setUsername("cassandra");
        cassandraConf.setPassword("cassandra");
        cassandraConf.setKeyspace("cpp_test");

        CassandraClient manager = new CassandraClient(cassandraConf);

        try {
            // 创建测试 keyspace
            manager.createKeyspace("test_ks",
                    "SimpleStrategy",
                    Map.of("replication_factor", 1),
                    true);

            // 创建生产 keyspace，仅使用存在的数据中心
            manager.createKeyspace("prod_ks",
                    "NetworkTopologyStrategy",
                    Map.of("datacenter1", 3),
                    true);

            // 检查 keyspace 是否存在
            System.out.println("test_ks exists: " + manager.keyspaceExists("test_ks"));

        } finally {
            manager.close();
        }
    }
}