package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.BaseConfiguration;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

/**
 * JanusGraph客户端实现
 *
 * @author April.Chen
 * @date 2025/8/13 15:40
 */
public class JanusClient implements GraphClient {
    private JanusConf janusConf;
    private JanusGraph graph;

    public JanusClient(JanusConf janusConf) {
        this.janusConf = janusConf;
        this.graph = createGraphInstance();
    }

    /**
     * 根据配置创建JanusGraph实例
     *
     * @return JanusGraph实例
     */
    private JanusGraph createGraphInstance() {
        // 构建配置
        Configuration config = new BaseConfiguration();
        
        // 存储后端配置
        config.setProperty("storage.backend", janusConf.getStorageBackend());
        config.setProperty("storage.hostname", janusConf.getStorageHostname());
        config.setProperty("storage.port", janusConf.getStoragePort());
        
        if (janusConf.getStorageUsername() != null && !janusConf.getStorageUsername().isEmpty()) {
            config.setProperty("storage.username", janusConf.getStorageUsername());
        }
        
        if (janusConf.getStoragePassword() != null && !janusConf.getStoragePassword().isEmpty()) {
            config.setProperty("storage.password", janusConf.getStoragePassword());
        }
        
        // 索引后端配置
        if (janusConf.isEnableIndexBackend()) {
            config.setProperty("index.search.backend", janusConf.getIndexBackendType());
            config.setProperty("index.search.hostname", janusConf.getIndexBackendHostname());
            config.setProperty("index.search.port", janusConf.getIndexBackendPort());
        }
        
        return JanusGraphFactory.open(config);
    }

    @Override
    public GraphOperations opsForGraph() {
        return new JanusGraphOperations(graph);
    }

    @Override
    public GraphDataOperations opsForGraphData() {
        return new JanusGraphDataOperations(graph);
    }

    @Override
    public boolean checkConnection() {
        return false;
    }

    /**
     * 关闭图数据库连接
     */
    public void close() {
        if (graph != null && !graph.isClosed()) {
            graph.close();
        }
    }
}