package com.chenpp.graph.core.schema;

import lombok.Data;

import java.util.List;

/**
 * @author April.Chen
 * @date 2023/10/11 4:29 下午
 **/
@Data
public class GraphLabel {
    private String name;
    private String label;
    private List<GraphProperty> properties;
}
