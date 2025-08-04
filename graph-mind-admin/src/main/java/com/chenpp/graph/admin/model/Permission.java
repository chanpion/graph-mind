package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 10:40
 */
@Data
@TableName("sys_permission")
public class Permission {
    /**
     * 权限ID
     */
    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;

    /**
     * 权限名称
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 父权限ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 权限类型（M:目录 C:菜单 F:按钮）
     */
    @TableField("permission_type")
    private String permissionType;

    /**
     * 权限标识
     */
    @TableField("permission_key")
    private String permissionKey;

    /**
     * 组件路径
     */
    @TableField("component")
    private String component;

    /**
     * 路由地址
     */
    @TableField("path")
    private String path;

    /**
     * 菜单图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 状态（0:正常 1:停用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 是否显示（0:显示 1:隐藏）
     */
    @TableField("visible")
    private Integer visible;

    /**
     * 是否缓存（0:缓存 1:不缓存）
     */
    @TableField("cache")
    private Integer cache;

    /**
     * 子权限列表
     */
    @TableField(exist = false)
    private List<Permission> children;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}