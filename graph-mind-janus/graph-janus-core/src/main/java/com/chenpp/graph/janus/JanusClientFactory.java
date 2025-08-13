package com.chenpp.graph.janus;

import lombok.extern.slf4j.Slf4j;

/**
 * JanusGraph客户端工厂类
 *
 * @author April.Chen
 * @date 2025/8/13 17:00
 */
@Slf4j
public class JanusClientFactory {

    /**
     * 根据配置创建JanusGraph客户端实例
     *
     * @param janusConf JanusGraph配置
     * @return JanusClient实例
     */
    public static JanusClient createJanusClient(JanusConf janusConf) {
        try {
            return new JanusClient(janusConf);
        } catch (Exception e) {
            log.error("Failed to create JanusGraph client", e);
            throw new RuntimeException("Failed to create JanusGraph client", e);
        }
    }
}