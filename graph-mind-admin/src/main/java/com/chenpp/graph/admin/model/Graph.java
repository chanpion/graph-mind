package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图实体类
 *
 * @author April.Chen
 * @date 2025/8/1 16:31
 */
@TableName("graph")
@Data
public class Graph {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String description;

    /**
     * 状态
     * 0: 正常
     * 1: 异常
     * 2: 未知
     */
    private Integer status;

    /**
     * 关联的图数据库连接ID
     */
    private Long connectionId;

    /**
     * 图类型,
     * @see com.chenpp.graph.admin.enums.GraphTypeEnum
     */
    private String graphType;
    /**
     * 创建人
     */
    private String creator;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ====== 非持久化字段（前端展示使用） ======

    /** 数据库类型（展示用，来自 graphType） */
    @TableField(exist = false)
    private String databaseType;

    /** 节点数量（展示用，来自图数据库统计） */
    @TableField(exist = false)
    private Integer vertexCount;

    /** 边数量（展示用，来自图数据库统计） */
    @TableField(exist = false)
    private Integer edgeCount;

    /** 节点类型数量（展示用，来自 graph_node_def 统计） */
    @TableField(exist = false)
    private Integer nodeTypeCount;

    /** 边类型数量（展示用，来自 graph_edge_def 统计） */
    @TableField(exist = false)
    private Integer edgeTypeCount;
}