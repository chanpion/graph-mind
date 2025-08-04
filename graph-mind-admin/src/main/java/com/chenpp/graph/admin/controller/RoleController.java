package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.Role;
import com.chenpp.graph.admin.service.RoleService;
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
 * 角色管理接口
 *
 * @author April.Chen
 * @date 2025/8/1 10:50
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 获取角色列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param roleName 角色名
     * @param roleKey  权限字符
     * @param status   状态
     * @return 角色列表
     */
    @GetMapping
    public Result<PageResult<Role>> getRoles(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Integer status) {
        PageResult<Role> pageResult = roleService.getRoles(pageNum, pageSize, roleName, roleKey, status);
        return Result.success(pageResult);
    }

    /**
     * 获取角色详情
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    @GetMapping("/{roleId}")
    public Result<Role> getRole(@PathVariable Long roleId) {
        Role role = roleService.getRoleById(roleId);
        return Result.success(role);
    }

    /**
     * 新增角色
     *
     * @param role 角色信息
     * @return 新增结果
     */
    @PostMapping
    public Result<String> addRole(@RequestBody Role role) {
        roleService.addRole(role);
        return Result.success("新增角色成功");
    }

    /**
     * 更新角色
     *
     * @param roleId 角色ID
     * @param role   角色信息
     * @return 更新结果
     */
    @PutMapping("/{roleId}")
    public Result<String> updateRole(@PathVariable Long roleId, @RequestBody Role role) {
        role.setRoleId(roleId);
        roleService.updateRole(role);
        return Result.success("更新角色成功");
    }

    /**
     * 删除角色
     *
     * @param roleIds 角色ID列表
     * @return 删除结果
     */
    @DeleteMapping
    public Result<String> deleteRoles(@RequestBody List<Long> roleIds) {
        roleService.deleteRoles(roleIds);
        return Result.success("删除角色成功");
    }

    /**
     * 更新角色状态
     *
     * @param roleId 角色ID
     * @param status 状态
     * @return 更新结果
     */
    @PutMapping("/{roleId}/status")
    public Result<String> updateRoleStatus(@PathVariable Long roleId, @RequestParam Integer status) {
        roleService.updateRoleStatus(roleId, status);
        return Result.success("更新角色状态成功");
    }

    /**
     * 获取角色数据权限
     *
     * @param roleId 角色ID
     * @return 数据权限
     */
    @GetMapping("/{roleId}/dataScope")
    public Result<Integer> getRoleDataScope(@PathVariable Long roleId) {
        Integer dataScope = roleService.getRoleDataScope(roleId);
        return Result.success(dataScope);
    }

    /**
     * 更新角色数据权限
     *
     * @param roleId    角色ID
     * @param dataScope 数据权限
     * @return 更新结果
     */
    @PutMapping("/{roleId}/dataScope")
    public Result<String> updateRoleDataScope(@PathVariable Long roleId, @RequestParam Integer dataScope) {
        roleService.updateRoleDataScope(roleId, dataScope);
        return Result.success("更新角色数据权限成功");
    }
}