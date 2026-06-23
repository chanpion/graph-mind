package com.chenpp.graph.nebula;

import com.chenpp.graph.core.exception.GraphException;
import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.SessionPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * nebula client 工厂类
 *
 * @author April.Chen
 * @date 2025/4/8 19:56
 */
@Slf4j
public class NebulaClientFactory {
    private static final Map<String, SessionPool> CACHE_SESSION_POOL = new ConcurrentHashMap<>();
    private static final Map<String, NebulaPool> CACHE_NEBULA_POOL = new ConcurrentHashMap<>();

    /**
     * SessionPool 用于单个图库操作，业务面
     */
    public static SessionPool getSessionPool(NebulaConf nebulaConf) {
        // 使用 graphCode 作为 space 名称，确保与其他操作一致
        String key = nebulaConf.getHosts() + ":" + nebulaConf.getPort() + ":" + nebulaConf.getGraphCode();
        SessionPool sessionPool = CACHE_SESSION_POOL.get(key);
        if (sessionPool != null && sessionPool.isActive()) {
            return sessionPool;
        }
        try {
            List<HostAddress> addresses = Arrays.stream(nebulaConf.getHosts().split(","))
                    .map(ip -> new HostAddress(ip, nebulaConf.getPort())).collect(Collectors.toList());
            String spaceName = nebulaConf.getGraphCode();
            String user = nebulaConf.getUsername();
            String password = nebulaConf.getPassword();
            SessionPoolConfig sessionPoolConfig = new SessionPoolConfig(addresses, spaceName, user, password);
            sessionPool = new SessionPool(sessionPoolConfig);
            if (!sessionPool.isActive()) {
                log.error("Session pool init failed for space: {}", spaceName);
                throw new GraphException("Session pool init failed for space: " + spaceName);
            }
            log.debug("Successfully created session pool for space: {}", spaceName);
            CACHE_SESSION_POOL.put(key, sessionPool);
            return sessionPool;
        } catch (Exception e) {
            log.error("Failed to create session pool for space: {}", nebulaConf.getGraphCode(), e);
            throw new GraphException("Failed to create session pool", e);
        }
    }

    /**
     * NebulaPool 用户图库操作，管理面
     *
     * @param nebulaConf 配置信息
     * @return NebulaPool实例
     */
    public static NebulaPool getNebulaPool(NebulaConf nebulaConf) {
        String key = nebulaConf.getHosts() + ":" + nebulaConf.getPort();
        NebulaPool pool = CACHE_NEBULA_POOL.get(key);
        if (pool != null) {
            return pool;
        }
        NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
        nebulaPoolConfig.setMaxConnSize(10);
        List<HostAddress> addresses = Arrays.stream(nebulaConf.getHosts().split(","))
                .map(ip -> new HostAddress(ip, nebulaConf.getPort())).collect(Collectors.toList());
        pool = new NebulaPool();
        try {
            boolean initResult = pool.init(addresses, nebulaPoolConfig);
            if (!initResult) {
                log.error("Pool init failed for hosts: {}", nebulaConf.getHosts());
                throw new GraphException("Nebula pool init failed for " + nebulaConf.getHosts());
            } else {
                log.info("Successfully initialized NebulaPool for hosts: {}", nebulaConf.getHosts());
                CACHE_NEBULA_POOL.put(key, pool);
            }
        } catch (Exception e) {
            log.error("Init nebula session error for hosts: {}", nebulaConf.getHosts(), e);
            throw new GraphException("Failed to init Nebula pool for " + nebulaConf.getHosts(), e);
        }
        return pool;
    }

}