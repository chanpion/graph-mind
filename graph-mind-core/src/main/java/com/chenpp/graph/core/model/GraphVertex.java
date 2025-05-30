package com.chenpp.graph.core.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 点数据
 *
 * @author April.Chen
 * @date 2023/10/11 3:51 下午
 **/
@Data
public class GraphVertex implements Serializable {


    private String id;
    /**
     * 类型
     */
    private String label;
    /**
     * 属性
     */
    private Map<String, Object> properties;
}
