package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.model.GraphConf;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author April.Chen
 * @date 2025/4/30 11:11
 */
@Data
@Slf4j
public class Neo4jConf extends GraphConf {

    private String uri;
    private String username;
    private String password;
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 30000;
    
    /**
     * 最大连接池大小
     */
    private int maxConnectionPoolSize = 100;
    
    /**
     * 验证配置是否完整
     * @return 配置是否有效
     */
    public boolean isValid() {
        if (uri == null || uri.isEmpty()) {
            log.warn("Neo4j URI is not configured");
            return false;
        }
        
        if (username == null || username.isEmpty()) {
            log.warn("Neo4j username is not configured");
            return false;
        }
        
        if (password == null || password.isEmpty()) {
            log.warn("Neo4j password is not configured");
            return false;
        }
        
        return true;
    }
}