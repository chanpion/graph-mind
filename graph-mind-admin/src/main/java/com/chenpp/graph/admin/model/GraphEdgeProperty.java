package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 图边属性定义
 *
 * @author April.Chen
 * @date 2025/8/4 15:30
 */
@TableName("graph_edge_property")
@Data
public class GraphEdgeProperty {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 边定义ID
     */
    private Long edgeDefId;
    /**
     * 属性标识
     */
    private String code;
    /**
     * 属性名
     */
    private String name;

    /**
     * 属性类型
     */
    private String type;

    /**
     * 属性描述
     */
    @TableField("`desc`")
    private String desc;
    
    /**
     * 状态：active-启用，inactive-停用
     */
    private String status = "active";
}