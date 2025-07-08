package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.model.GraphConf;
import lombok.Data;

/**
 * @author April.Chen
 * @date 2025/4/30 11:11
 */
@Data
public class Neo4jConf extends GraphConf {

    private String uri;
    private String username;
    private String password;
}
