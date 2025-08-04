package com.chenpp.graph.admin.mapper;

import com.chenpp.graph.admin.model.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 18:30
 */
@Mapper
public interface RolePermissionDao {
    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}