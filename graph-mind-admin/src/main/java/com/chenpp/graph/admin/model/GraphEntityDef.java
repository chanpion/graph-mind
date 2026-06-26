package com.chenpp.graph.admin.model;

import java.util.List;

/**
 * 图实体定义（顶点/边）公共接口
 *
 * @author April.Chen
 * @date 2026/6/26
 */
public interface GraphEntityDef {

    Long getId();

    void setId(Long id);

    Long getGraphId();

    void setGraphId(Long graphId);

    String getLabel();

    void setLabel(String label);

    String getName();

    void setName(String name);

    List<GraphPropertyDef> getProperties();

    void setProperties(List<GraphPropertyDef> properties);
}
