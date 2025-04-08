package com.chenpp.graph.nebula;

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
        List<HostAddress> addresses = Arrays.stream(nebulaConf.getHosts().split(","))
                .map(ip -> new HostAddress(ip, nebulaConf.getPort())).collect(Collectors.toList());
        String spaceName = nebulaConf.getGraphCode();
        String user = nebulaConf.getUsername();
        String password = nebulaConf.getPassword();
        SessionPoolConfig sessionPoolConfig = new SessionPoolConfig(addresses, spaceName, user, password);
        SessionPool sessionPool = new SessionPool(sessionPoolConfig);
        if (!sessionPool.isActive()) {
            log.error("session pool init failed.");
            System.exit(1);
        }
        return sessionPool;
    }

    /**
     * NebulaPool 用户图库操作，管理面
     *
     * @param nebulaConf
     * @return
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
                log.error("pool init failed.");
            }
        } catch (Exception e) {
            log.error("init nebula session error", e);
        }
        return pool;
    }

}
