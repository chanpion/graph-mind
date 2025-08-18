package com.chenpp.graph.janus;

import lombok.Data;

/**
 * @author April.Chen
 * @date 2024/5/13 17:41
 */
@Data
public class CassandraConf {
    private String hostname;
    private int port;
    private String keyspace;
    private String username;
    private String password;
}
