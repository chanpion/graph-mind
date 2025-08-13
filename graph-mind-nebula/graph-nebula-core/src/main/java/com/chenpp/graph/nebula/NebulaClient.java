package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import com.vesoft.nebula.client.graph.net.NebulaPool;

/**
 * @author April.Chen
 * @date 2025/4/9 15:21
 */
public class NebulaClient implements GraphClient {

    private NebulaConf nebulaConf;

    public NebulaClient(NebulaConf nebulaConf) {
        this.nebulaConf = nebulaConf;
    }

    @Override
    public GraphOperations opsForGraph() {
        return new NebulaGraphOperations();
    }

    @Override
    public GraphDataOperations opsForGraphData() {
        return new NebulaGraphDataOperations(nebulaConf);
    }

    @Override
    public boolean checkConnection() {
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        return nebulaPool != null;
    }
}