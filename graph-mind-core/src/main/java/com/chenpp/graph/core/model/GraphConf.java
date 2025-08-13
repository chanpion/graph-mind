package com.chenpp.graph.core.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2024/5/14 16:24
 */
@Data
public class GraphConf implements Serializable {

    private static final long serialVersionUID = 1280486515932787575L;

    private String graphCode;
    private String type;
    private String host;
    private int port;
    private String username;
    private String password;

    /**
     * 配置参数
     */
    private Map<String, Object> params;

}