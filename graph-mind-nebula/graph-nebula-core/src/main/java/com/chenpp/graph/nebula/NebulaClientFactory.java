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
import java.util.stream.Collectors;

/**
 * nebula client 工厂类
 *
 * @author April.Chen
 * @date 2025/4/8 19:56
 */
@Slf4j
public class NebulaClientFactory {

    /**
     * SessionPool 用于单个图库操作，业务面
     */
    public static SessionPool getSessionPool(NebulaConf nebulaConf) {
        try {
            List<HostAddress> addresses = Arrays.stream(nebulaConf.getHosts().split(","))
                    .map(ip -> new HostAddress(ip, nebulaConf.getPort())).collect(Collectors.toList());
            String spaceName = nebulaConf.getGraphCode();
            String user = nebulaConf.getUsername();
            String password = nebulaConf.getPassword();
            SessionPoolConfig sessionPoolConfig = new SessionPoolConfig(addresses, spaceName, user, password);
            SessionPool sessionPool = new SessionPool(sessionPoolConfig);
            if (!sessionPool.isActive()) {
                log.error("Session pool init failed for space: {}", spaceName);
                // 不应该直接退出JVM，而应该抛出异常让调用方处理
                throw new GraphException("Session pool init failed for space: " + spaceName);
            }
            log.debug("Successfully created session pool for space: {}", spaceName);
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
        NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
        nebulaPoolConfig.setMaxConnSize(10);
        List<HostAddress> addresses = Arrays.stream(nebulaConf.getHosts().split(","))
                .map(ip -> new HostAddress(ip, nebulaConf.getPort())).collect(Collectors.toList());
        NebulaPool pool = new NebulaPool();
        try {
            boolean initResult = pool.init(addresses, nebulaPoolConfig);
            if (!initResult) {
                log.error("Pool init failed for hosts: {}", nebulaConf.getHosts());
                pool = null;
            } else {
                log.info("Successfully initialized NebulaPool for hosts: {}", nebulaConf.getHosts());
            }
        } catch (Exception e) {
            log.error("Init nebula session error for hosts: {}", nebulaConf.getHosts(), e);
            pool = null;
        }
        return pool;
    }

}