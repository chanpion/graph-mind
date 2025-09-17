package com.chenpp.graph.janus;

import com.chenpp.graph.core.exception.GraphException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.chenpp.graph.janus.JanusConstants.BACKEND_CASSANDRA;
import static com.chenpp.graph.janus.JanusConstants.BACKEND_HBASE;
import static org.janusgraph.diskstorage.hbase.HBaseStoreManager.HBASE_TABLE;
import static org.janusgraph.diskstorage.hbase.HBaseStoreManager.REGION_COUNT;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.AUTH_PASSWORD;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.AUTH_USERNAME;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.AUTO_TYPE;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.CLUSTER_MAX_PARTITIONS;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.IDAUTHORITY_WAIT;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.IDS_BLOCK_SIZE;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.STORAGE_BACKEND;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.STORAGE_BATCH;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.STORAGE_HOSTS;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.STORAGE_PORT;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.USE_MULTIQUERY;
import static org.janusgraph.graphdb.database.idassigner.placement.SimpleBulkPlacementStrategy.CONCURRENT_PARTITIONS;

/**
 * JanusGraph客户端工厂类
 *
 * @author April.Chen
 * @date 2025/8/13 17:00
 */
@Slf4j
public class JanusClientFactory {

    /**
     * 根据配置创建JanusGraph客户端实例
     *
     * @param janusConf JanusGraph配置
     * @return JanusClient实例
     */
    public static JanusClient createJanusClient(JanusConf janusConf) {
        try {
            return new JanusClient(janusConf);
        } catch (Exception e) {
            log.error("Failed to create JanusGraph client", e);
            throw new RuntimeException("Failed to create JanusGraph client", e);
        }
    }


    private static final Map<String, JanusGraph> JANUS_GRAPH_MAP = new ConcurrentHashMap<>();

    public static JanusGraph getOrCreateJanusGrapht(JanusConf janusConf) {
        String key = String.format("%s_%s_%s", janusConf.getStorageHost(), janusConf.getStoragePort(), janusConf.getGraphCode());

        JanusGraph graph = JANUS_GRAPH_MAP.get(key);
        if (graph != null && graph.isOpen()) {
            return graph;
        }
        
        Configuration configuration;
        try {
            switch (janusConf.getStorageBackend()) {
                case BACKEND_HBASE:
                    configuration = buildHBaseConfiguration(janusConf);
                    break;
                case BACKEND_CASSANDRA:
                    configuration = buildCassandraConfiguration(janusConf);
                    break;
                default:
                    log.error("Not supported backend: {}", janusConf.getStorageBackend());
                    throw new GraphException("Not supported backend: " + janusConf.getStorageBackend());
            }
            
            //open a graph database
            graph = JanusGraphFactory.open(configuration);
            JANUS_GRAPH_MAP.put(key, graph);
            log.info("Created new JanusGraph instance for key: {}", key);
            return graph;
        } catch (Exception e) {
            log.error("Failed to create or get JanusGraph instance for key: {}", key, e);
            throw new GraphException("Failed to create or get JanusGraph instance", e);
        }
    }


    private static Configuration buildHBaseConfiguration(JanusConf janusConf) {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(STORAGE_BACKEND.toStringWithoutRoot(), janusConf.getStorageBackend());

        String finalTableName = resolveTableName(janusConf);
        config.setProperty(HBASE_TABLE.toStringWithoutRoot(), finalTableName);

        HBaseConf hbaseConf = janusConf.getHBaseConf();
        config.setProperty(STORAGE_HOSTS.toStringWithoutRoot(), hbaseConf.getHbaseHost());
        config.setProperty(STORAGE_PORT.toStringWithoutRoot(), hbaseConf.getHbasePort());
        if (StringUtils.isNotEmpty(hbaseConf.getHbaseZnode())) {
            config.setProperty("storage.hbase.ext.zookeeper.znode.parent", hbaseConf.getHbaseZnode());
        }
        config.setProperty(CLUSTER_MAX_PARTITIONS.toStringWithoutRoot(), hbaseConf.getHbaseRegionNum());
        config.setProperty(REGION_COUNT.toStringWithoutRoot(), hbaseConf.getHbaseRegionNum());
        config.setProperty(AUTO_TYPE.toStringWithoutRoot(), "none");
        config.setProperty(STORAGE_BATCH.toStringWithoutRoot(), false);
        config.setProperty("graph.replace-instance-if-exists", true);
        config.setProperty("cache.db-cache", true);

        config.setProperty("storage.hbase.ext.hbase.client.retries.number", "1");
        config.setProperty("storage.hbase.ext.zookeeper.recovery.retry", "1");

        config.setProperty(CONCURRENT_PARTITIONS.toStringWithoutRoot(), hbaseConf.getHbaseRegionNum());
        config.setProperty(USE_MULTIQUERY.toStringWithoutRoot(), true);
        config.setProperty("query.batch-property-prefetch", true);
        config.setProperty(IDAUTHORITY_WAIT.toStringWithoutRoot(), Duration.ofMillis(1000L));
        config.setProperty(IDS_BLOCK_SIZE.toStringWithoutRoot(), janusConf.getIdsBlockSize());


        return config;
    }

    private static String resolveTableName(JanusConf janusConf) {
        String tableName = janusConf.getGraphCode();

        String namespace = janusConf.getHBaseConf().getHbaseNs();
        if (StringUtils.isNotEmpty(namespace)) {
            tableName = namespace + ":" + tableName;
        }
        return tableName;
    }


    public static Configuration buildCassandraConfiguration(JanusConf janusConf) {
        CassandraConf cassandraConf = janusConf.getCassandraConf();
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(STORAGE_BACKEND.toStringWithoutRoot(), "cql");
        configuration.setProperty(STORAGE_HOSTS.toStringWithoutRoot(), cassandraConf.getHostname());
        configuration.setProperty(STORAGE_PORT.toStringWithoutRoot(), cassandraConf.getPort());
        String keyspace = cassandraConf.getKeyspace();
        if (Objects.equals(keyspace, JanusConstants.DEFAULT_GRAPH_CODE)) {
            keyspace = "\"default\"";
        }
        configuration.setProperty("storage.cql.keyspace", keyspace);
        if (StringUtils.isNoneBlank(cassandraConf.getPassword(), cassandraConf.getPassword())) {
            configuration.setProperty(AUTH_USERNAME.toStringWithoutRoot(), janusConf.getCassandraConf().getUsername());
            configuration.setProperty(AUTH_PASSWORD.toStringWithoutRoot(), janusConf.getCassandraConf().getPassword());
        }
        configuration.setProperty(IDS_BLOCK_SIZE.toStringWithoutRoot(), janusConf.getIdsBlockSize());
        return configuration;
    }
}