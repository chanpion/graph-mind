package com.chenpp.graph.janus.util;

import com.chenpp.graph.janus.CassandraConf;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.OptionsMap;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/8/18 17:05
 */
public class CassandraClient {

    private final CqlSession session;

    public CassandraClient(CassandraConf cassandraConf) {
        DriverConfigLoader loader = DriverConfigLoader.fromMap(OptionsMap.driverDefaults());
        this.session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandraConf.getHostname(), cassandraConf.getPort()))
                .withLocalDatacenter("datacenter1")
                .withAuthCredentials(cassandraConf.getUsername(), cassandraConf.getPassword())
                .withConfigLoader(loader).build();
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
        String replicationConfigStr = buildReplicationConfig(strategy, replicationConfig);

        String query = String.format(
                "CREATE KEYSPACE IF NOT EXISTS %s " +
                        "WITH replication = %s " +
                        "AND durable_writes = %s",
                keyspaceName, replicationConfigStr, durableWrites);

        executeQuery(query);
        System.out.println("Created keyspace: " + keyspaceName);
    }

    /**
     * 删除 keyspace
     *
     * @param keyspaceName keyspace 名称
     */
    public void dropKeyspace(String keyspaceName) {
        String query = String.format("DROP KEYSPACE IF EXISTS %s", keyspaceName);
        executeQuery(query);
        System.out.println("Dropped keyspace: " + keyspaceName);
    }

    /**
     * 检查 keyspace 是否存在
     *
     * @param keyspaceName keyspace 名称
     * @return 是否存在
     */
    public boolean keyspaceExists(String keyspaceName) {
        String query = String.format(
                "SELECT keyspace_name FROM system_schema.keyspaces " +
                        "WHERE keyspace_name = '%s'", keyspaceName);

        return !session.execute(SimpleStatement.newInstance(query)).all().isEmpty();
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
        session.execute(SimpleStatement.newInstance(query));
    }

    public void close() {
        if (session != null) {
            session.close();
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
                    Map.of("datacenter1", 3), // 移除不存在的datacenter2
                    true);

            // 检查 keyspace 是否存在
            System.out.println("test_ks exists: " + manager.keyspaceExists("test_ks"));

        } finally {
            manager.close();
        }
    }
}