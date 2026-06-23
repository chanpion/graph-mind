package com.chenpp.graph.admin.util;

import com.alibaba.fastjson2.JSON;
import com.chenpp.graph.admin.enums.GraphTypeEnum;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.janus.CassandraConf;
import com.chenpp.graph.janus.HBaseConf;
import com.chenpp.graph.janus.JanusClient;
import com.chenpp.graph.janus.JanusConf;
import com.chenpp.graph.janus.JanusConstants;
import com.chenpp.graph.nebula.NebulaClient;
import com.chenpp.graph.nebula.NebulaConf;
import com.chenpp.graph.neo4j.Neo4jClient;
import com.chenpp.graph.neo4j.Neo4jConf;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.service.GraphConnectionService;
import org.apache.commons.lang3.StringUtils;

/**
 * 图客户端工厂类，用于根据数据库类型动态创建对应的图客户端实例
 *
 * @author April.Chen
 * @date 2025/8/13 10:00
 */
public class GraphClientFactory {

    /**
     * 根据数据库类型创建对应的图客户端实例
     *
     * @param graphConf 图配置信息
     * @return GraphClient 图客户端实例
     */
    public static GraphClient createGraphClient(GraphConf graphConf) {
        String type = graphConf.getType();
        switch (GraphTypeEnum.valueOf(type.toLowerCase())) {
            case neo4j:
                Neo4jConf neo4jConf = JSON.parseObject(JSON.toJSONString(graphConf.getParams()), Neo4jConf.class);
                neo4jConf.setGraphCode(graphConf.getGraphCode());
                neo4jConf.setUsername(graphConf.getUsername());
                neo4jConf.setPassword(graphConf.getPassword());
                if (graphConf.getHost() != null) {
                    neo4jConf.setUri(String.format("neo4j://%s:%s", graphConf.getHost(), graphConf.getPort()));
                } else {
                    neo4jConf.setUri(String.format("neo4j://%s:%s", graphConf.getParams().get("host"), graphConf.getParams().get("port")));
                }
                return new Neo4jClient(neo4jConf);

            case nebula:
                NebulaConf nebulaConf = JSON.parseObject(JSON.toJSONString(graphConf.getParams()), NebulaConf.class);
                nebulaConf.setGraphCode(graphConf.getGraphCode());
                nebulaConf.setHosts(graphConf.getHost());
                nebulaConf.setPort(graphConf.getPort());
                nebulaConf.setUsername(graphConf.getUsername());
                nebulaConf.setPassword(graphConf.getPassword());
                nebulaConf.setGraphCode(graphConf.getGraphCode());
                return new NebulaClient(nebulaConf);
            case janus:
                JanusConf janusConf = JSON.parseObject(JSON.toJSONString(graphConf.getParams()), JanusConf.class);
                janusConf.setGraphCode(graphConf.getGraphCode());
                janusConf.setUsername(graphConf.getUsername());
                janusConf.setPassword(graphConf.getPassword());
                String storageBackend = janusConf.getStorageBackend();
                if (StringUtils.equalsAny(storageBackend, JanusConstants.BACKEND_CASSANDRA, JanusConstants.CQL)) {
                    CassandraConf cassandraConf = JSON.parseObject(JSON.toJSONString(graphConf.getParams()), CassandraConf.class);
                    cassandraConf.setHostname(graphConf.getHost());
                    cassandraConf.setPort(graphConf.getPort());
                    janusConf.setCassandraConf(cassandraConf);
                } else {
                    HBaseConf hBaseConf = JSON.parseObject(JSON.toJSONString(graphConf.getParams()), HBaseConf.class);
                    hBaseConf.setHbaseHost(graphConf.getHost());
                    hBaseConf.setHbasePort(graphConf.getPort());
                    janusConf.setHBaseConf(hBaseConf);
                }
                return new JanusClient(janusConf);

            default:
                throw new IllegalArgumentException("Unsupported graph database type: " + type);
        }
    }

    public static GraphConf resolveGraphConf(Long graphId, Long connectionId, String graphCode,
                                             GraphService graphService, GraphConnectionService connectionService) {
        if (graphId != null && graphId > 0) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo != null && graphInfo.getConnectionId() != null) {
                GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
                if (connection != null) {
                    return createGraphConf(connection, graphInfo.getCode());
                }
            }
        }
        if (connectionId != null && graphCode != null) {
            GraphConnection connection = connectionService.getById(connectionId);
            if (connection != null) {
                return createGraphConf(connection, graphCode);
            }
        }
        throw new IllegalArgumentException("无法解析图连接配置，请检查 graphId/connectionId/graphCode");
    }

    public static GraphConf createGraphConf(GraphConnection connection, String graphCode) {
        GraphConf graphConf = new GraphConf();
        graphConf.setGraphCode(graphCode);
        graphConf.setType(connection.getGraphType());
        graphConf.setHost(connection.getHosts());
        graphConf.setPort(connection.getPort());
        graphConf.setUsername(connection.getUsername());
        graphConf.setPassword(connection.getPassword());
        if (connection.getParams() != null) {
            graphConf.setParams(JSON.parseObject(connection.getParams()));
        } else {
            graphConf.setParams(JSON.parseObject(JSON.toJSONString(connection)));
        }

        if (graphConf.getUsername() == null && graphConf.getParams() != null) {
            Object username = graphConf.getParams().get("username");
            if (username != null) {
                graphConf.setUsername(username.toString());
            }
        }
        if (graphConf.getPassword() == null && graphConf.getParams() != null) {
            Object password = graphConf.getParams().get("password");
            if (password != null) {
                graphConf.setPassword(password.toString());
            }
        }
        return graphConf;
    }
}