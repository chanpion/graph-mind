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
    private String storageHostname;

    /**
     * 存储端口
     */
    private int storagePort;

    /**
     * 存储用户名
     */
    private String storageUsername;

    /**
     * 存储密码
     */
    private String storagePassword;

    /**
     * 是否启用索引后端
     */
    private boolean enableIndexBackend;

    /**
     * 索引后端类型，如elasticsearch、solr等
     */
    private String indexBackendType;

    /**
     * 索引后端主机地址
     */
    private String indexBackendHostname;

    /**
     * 索引后端端口
     */
    private int indexBackendPort;
}