package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.Permission;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理接口
 *
 * @author April.Chen
 * @date 2025/8/1 10:55
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 获取权限菜单树
     *
     * @return 权限菜单树
     */
    @GetMapping
    public Result<List<Permission>> getPermissions() {
        List<Permission> permissions = permissionService.getPermissions();
        return Result.success(permissions);
    }

    /**
     * 获取权限详情
     *
     * @param permissionId 权限ID
     * @return 权限详情
     */
    @GetMapping("/{permissionId}")
    public Result<Permission> getPermission(@PathVariable Long permissionId) {
        Permission permission = permissionService.getPermissionById(permissionId);
        return Result.success(permission);
    }

    /**
     * 新增权限
     *
     * @param permission 权限信息
     * @return 新增结果
     */
    @PostMapping
    public Result<String> addPermission(@RequestBody Permission permission) {
        permissionService.addPermission(permission);
        return Result.success("新增权限成功");
    }

    /**
     * 更新权限
     *
     * @param permissionId 权限ID
     * @param permission   权限信息
     * @return 更新结果
     */
    @PutMapping("/{permissionId}")
    public Result<String> updatePermission(@PathVariable Long permissionId, @RequestBody Permission permission) {
        permission.setPermissionId(permissionId);
        permissionService.updatePermission(permission);
        return Result.success("更新权限成功");
    }

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     * @return 删除结果
     */
    @DeleteMapping("/{permissionId}")
    public Result<String> deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return Result.success("删除权限成功");
    }

    /**
     * 更新权限状态
     *
     * @param permissionId 权限ID
     * @param status       状态
     * @return 更新结果
     */
    @PutMapping("/{permissionId}/status")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    public Result<String> updatePermissionStatus(@PathVariable Long permissionId, @RequestParam Integer status) {
        permissionService.updatePermissionStatus(permissionId, status);
        return Result.success("更新权限状态成功");
    }
}