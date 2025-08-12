package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图边定义
 *
 * @author April.Chen
 * @date 2025/8/4 15:10
 */
@TableName(value = "graph_edge_def", excludeProperty = {"properties"})
@Data
public class GraphEdgeDef {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图ID
     */
    private Long graphId;

    /**
     * 标签
     */
    private String label;
    /**
     * 边类型名称
     */
    private String name;

    /**
     * 起点类型
     */
    @TableField("`from`")
    private String from;

    /**
     * 终点类型
     */
    @TableField("`to`")
    private String to;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：0-未发布，1-已发布
     */
    private Integer status = 1;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 边属性列表
     */
    private List<GraphPropertyDef> properties;
}