package com.chenpp.graph.core.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2023/10/11 3:51 下午
 **/
@Data
public class GraphEdge implements Serializable {
    private static final long serialVersionUID = -7475762228551385446L;

    private String id;
    /**
     * 类型
     */
    private String label;
    /**
     * 属性
     */
    private Map<String, Object> properties;
    private String source;
    private String target;
}
