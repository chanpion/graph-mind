package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 图属性定义（合并点属性和边属性）
 *
 * @author April.Chen
 * @date 2025/8/12 15:30
 */
@TableName("graph_property_def")
@Data
public class GraphPropertyDef {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图ID
     */
    private Long graphId;

    /**
     * 节点定义ID或边定义ID
     */
    private Long entityId;

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
     * 状态：0-未发布，1-已发布
     */
    private Integer status = 1;

    /**
     * 是否索引：true-是，false-否
     */
    private Boolean indexed = false;
    
    /**
     * 属性类型标记：node-节点属性，edge-边属性
     */
    private String propertyType;
}