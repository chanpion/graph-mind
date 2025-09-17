package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.GraphOperations;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import lombok.extern.slf4j.Slf4j;

/**
 * @author April.Chen
 * @date 2025/4/9 15:21
 */
@Slf4j
public class NebulaClient implements GraphClient {

    private NebulaConf nebulaConf;

    public NebulaClient(NebulaConf nebulaConf) {
        if (nebulaConf.getPartitionNum() <= 0) {
            nebulaConf.setPartitionNum(3);
        }
        if (nebulaConf.getReplicaFactor() <= 0) {
            nebulaConf.setReplicaFactor(1);
        }
        if (nebulaConf.getVidFixedStrLength() <= 0) {
            nebulaConf.setVidFixedStrLength(32);
        }

        this.nebulaConf = nebulaConf;
    }

    @Override
    public GraphOperations opsForGraph() {
        return new NebulaGraphOperations(nebulaConf);
    }

    @Override
    public GraphDataOperations opsForGraphData() {
        return new NebulaGraphDataOperations(nebulaConf);
    }

    @Override
    public boolean checkConnection() {
        log.info("Checking Nebula connection");
        NebulaPool nebulaPool = NebulaClientFactory.getNebulaPool(nebulaConf);
        if (nebulaPool == null) {
            log.warn("Failed to get NebulaPool");
            return false;
        }
        
        try {
            // 使用正确的API检查连接状态
            // 通过尝试获取会话来检查连接是否有效
            var session = nebulaPool.getSession(nebulaConf.getUsername(), nebulaConf.getPassword(), true);
            if (session != null) {
                session.release();
                log.info("Nebula connection check result: true");
                return true;
            }
            log.info("Nebula connection check result: false");
            return false;
        } catch (Exception e) {
            log.error("Error checking Nebula connection", e);
            return false;
        }
    }

    @Override
    public void close() {
        log.info("Closing Nebula client");
        //todo close
        log.warn("Nebula client close operation not yet implemented");
    }
}