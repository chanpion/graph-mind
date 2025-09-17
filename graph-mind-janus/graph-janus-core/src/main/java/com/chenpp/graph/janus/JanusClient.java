package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.GraphException;
import lombok.extern.slf4j.Slf4j;
import org.janusgraph.core.JanusGraph;

/**
 * JanusGraph客户端实现
 *
 * @author April.Chen
 * @date 2025/8/13 15:40
 */
@Slf4j
public class JanusClient implements GraphClient {
    private final JanusConf janusConf;
    private JanusGraph graph;

    public JanusClient(JanusConf janusConf) {
        if (janusConf == null) {
            throw new IllegalArgumentException("JanusConf cannot be null");
        }
        
        this.janusConf = janusConf;
        if (janusConf.getGraphCode() == null) {
            janusConf.setGraphCode(JanusConstants.DEFAULT_GRAPH_CODE);
        }
        
        try {
            this.graph = JanusClientFactory.getOrCreateJanusGrapht(janusConf);
            log.debug("Successfully created JanusClient for graph: {}", janusConf.getGraphCode());
        } catch (Exception e) {
            log.error("Failed to create JanusGraph client for graph: {}", janusConf.getGraphCode(), e);
            throw new GraphException("Failed to create JanusGraph client", e);
        }
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
        boolean isOpen = graph != null && graph.isOpen();
        log.debug("Checking JanusGraph connection status: {}", isOpen);
        return isOpen;
    }


    public JanusGraph getGraph() {
        return graph;
    }

    /**
     * 关闭图数据库连接
     */
    @Override
    public void close() {
        log.info("Closing JanusGraph client for graph: {}", janusConf.getGraphCode());
        if (graph != null && !graph.isClosed()) {
            try {
                graph.close();
                log.info("Successfully closed JanusGraph client for graph: {}", janusConf.getGraphCode());
            } catch (Exception e) {
                log.error("Error closing JanusGraph client for graph: {}", janusConf.getGraphCode(), e);
            }
        }
    }

}