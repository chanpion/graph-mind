package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.Role;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 11:15
 */
public interface RoleService extends IService<Role> {
    /**
     * 获取角色列表
     *
     * @param pageNum  页码
     * @param pageSize 每页记录数
     * @param roleName 角色名称
     * @param roleKey  权限字符
     * @param status   状态
     * @return 角色列表
     */
    PageResult<Role> getRoles(Integer pageNum, Integer pageSize, String roleName, String roleKey, Integer status);

    /**
     * 根据角色ID获取角色信息
     *
     * @param roleId 角色ID
     * @return 角色信息
     */
    Role getRoleById(Long roleId);

    /**
     * 新增角色
     *
     * @param role 角色信息
     */
    void addRole(Role role);

    /**
     * 更新角色
     *
     * @param role 角色信息
     */
    void updateRole(Role role);

    /**
     * 删除角色
     *
     * @param roleIds 角色ID列表
     */
    void deleteRoles(List<Long> roleIds);

    /**
     * 更新角色状态
     *
     * @param roleId 角色ID
     * @param status 状态
     */
    void updateRoleStatus(Long roleId, Integer status);

    /**
     * 获取角色数据权限
     *
     * @param roleId 角色ID
     * @return 数据权限
     */
    Integer getRoleDataScope(Long roleId);

    /**
     * 更新角色数据权限
     *
     * @param roleId    角色ID
     * @param dataScope 数据权限
     */
    void updateRoleDataScope(Long roleId, Integer dataScope);
}