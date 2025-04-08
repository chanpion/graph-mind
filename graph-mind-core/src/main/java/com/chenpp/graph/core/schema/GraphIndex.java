package com.chenpp.graph.core.schema;

import lombok.Data;

import java.util.List;

/**
 * @author April.Chen
 * @date 2023/10/11 4:25 下午
 **/
@Data
public class GraphIndex {

    private String name;
    private String type;
    private String label;
    private String property;
    /**
     * 属性标识列表
     */
    private List<String> propertyNames;
}
