package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.Permission;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 11:20
 */
public interface PermissionService extends IService<Permission> {
    /**
     * 获取权限菜单树
     *
     * @return 权限菜单树
     */
    List<Permission> getPermissions();

    /**
     * 根据权限ID获取权限信息
     *
     * @param permissionId 权限ID
     * @return 权限信息
     */
    Permission getPermissionById(Long permissionId);

    /**
     * 新增权限
     *
     * @param permission 权限信息
     */
    void addPermission(Permission permission);

    /**
     * 更新权限
     *
     * @param permission 权限信息
     */
    void updatePermission(Permission permission);

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     */
    void deletePermission(Long permissionId);

    /**
     * 更新权限状态
     *
     * @param permissionId 权限ID
     * @param status       状态
     */
    void updatePermissionStatus(Long permissionId, Integer status);
}