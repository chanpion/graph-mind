package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author April.Chen
 * @date 2025/8/1 10:35
 */
@Data
@TableName("sys_role")
public class Role {
    /**
     * 角色ID
     */
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色权限字符串
     */
    @TableField("role_key")
    private String roleKey;

    /**
     * 显示顺序
     */
    @TableField("role_sort")
    private Integer roleSort;

    /**
     * 状态（0:正常 1:停用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 数据范围（1:全部数据权限 2:自定义数据权限 3:本部门数据权限 4:本部门及以下数据权限 5:仅本人数据权限）
     */
    @TableField("data_scope")
    private Integer dataScope;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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