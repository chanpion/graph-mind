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
    private final JanusConf janusConf;
    private JanusGraph graph;

    public JanusClient(JanusConf janusConf) {
        this.janusConf = janusConf;
        if (janusConf.getGraphCode() == null) {
            janusConf.setGraphCode("\"default\"");
        }
        this.graph = JanusClientFactory.getOrCreateJanusGrapht(janusConf);
    }

    @Override
    public GraphOperations opsForGraph() {
        return new JanusGraphOperations(graph, janusConf);
    }

    @Override
    public GraphDataOperations opsForGraphData() {
        return new JanusGraphDataOperations(graph);
    }

    @Override
    public boolean checkConnection() {
        return graph.isOpen();
    }

    /**
     * 关闭图数据库连接
     */
    public void close() {
        if (graph != null && !graph.isClosed()) {
            graph.close();
        }
    }

    public JanusGraph getGraph() {
        return graph;
    }
}