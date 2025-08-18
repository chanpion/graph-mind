package com.chenpp.graph.janus;

import com.chenpp.graph.core.model.GraphConf;
import lombok.Data;

/**
 * JanusGraph配置类
 *
 * @author April.Chen
 * @date 2025/8/13 15:30
 */
@Data
public class JanusConf extends GraphConf {
    private static final long serialVersionUID = -2304451347285714889L;

    /**
     * 存储后端类型，如cql、hbase等
     */
    private String storageBackend;

    /**
     * 存储主机地址
     */
    private String storageHost;

    /**
     * 存储端口
     */
    private int storagePort;

    /**
     * 索引后端端口
     */
    private int indexBackendPort;

    private Integer idsBlockSize;

    private HBaseConf hBaseConf;
    private CassandraConf cassandraConf;
}