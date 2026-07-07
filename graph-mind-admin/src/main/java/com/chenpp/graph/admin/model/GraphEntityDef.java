package com.chenpp.graph.admin.model;

import lombok.Data;

import java.util.List;

/**
 * 图实体定义（顶点/边）公共接口
 *
 * @author April.Chen
 * @date 2026/6/26
 */
@Data
public class GraphEntityDef {

    private Long id;
    private  Long graphId;
    private String label;
    private List<GraphPropertyDef> properties;
}
