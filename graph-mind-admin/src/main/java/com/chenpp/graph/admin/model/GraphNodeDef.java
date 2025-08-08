package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图节点定义
 *
 * @author April.Chen
 * @date 2025/8/4 15:00
 */
@TableName(value = "graph_node_def", excludeProperty = {"properties"})
@Data
public class GraphNodeDef {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图ID
     */
    private Long graphId;

    /**
     * 实体标识
     */
    private String code;
    /**
     * 节点类型名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：active-启用，inactive-停用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 节点属性列表
     */
    private List<GraphNodeProperty> properties;
}