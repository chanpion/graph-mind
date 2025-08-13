package com.chenpp.graph.janus;

import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.model.GraphConf;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

/**
 * JanusGraph测试类
 *
 * @author April.Chen
 * @date 2025/8/13 09:53
 */
public class JanusTest {
    private JanusConf janusConf;
    private JanusClient janusClient;

    @Before
    public void init() {
        janusConf = new JanusConf();
        janusConf.setGraphCode("test_graph");
        janusConf.setStorageBackend("inmemory"); // 使用内存存储进行测试
        janusConf.setStorageHostname("localhost");
        janusConf.setStoragePort(9160);
    }

    @Test
    public void testCreateClient() {
        // 测试创建JanusGraph客户端
        janusClient = new JanusClient(janusConf);
        assert janusClient != null;
        
        // 获取图操作实例
        GraphOperations graphOps = janusClient.opsForGraph();
        assert graphOps != null;
        
        // 关闭客户端
        janusClient.close();
    }
}