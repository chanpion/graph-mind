package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.PermissionDao;
import com.chenpp.graph.admin.model.Permission;
import com.chenpp.graph.admin.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/8/1 11:35
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionDao, Permission> implements PermissionService {

    @Override
    public List<Permission> getPermissions() {
        // 查询所有权限
        List<Permission> permissions = this.list();
        // 构建权限树
        return buildPermissionTree(permissions, 0L);
    }

    @Override
    public Permission getPermissionById(Long permissionId) {
        return this.getById(permissionId);
    }

    @Override
    public void addPermission(Permission permission) {
        this.save(permission);
    }

    @Override
    public void updatePermission(Permission permission) {
        this.updateById(permission);
    }

    @Override
    public void deletePermission(Long permissionId) {
        // 删除权限及其子权限
        this.removeById(permissionId);
        removeChildPermissions(permissionId);
    }

    @Override
    public void updatePermissionStatus(Long permissionId, Integer status) {
        Permission permission = new Permission();
        permission.setPermissionId(permissionId);
        permission.setStatus(status);
        this.updateById(permission);
    }

    /**
     * 递归删除子权限
     *
     * @param parentId 父权限ID
     */
    private void removeChildPermissions(Long parentId) {
        // 查询直接子权限
        List<Permission> childPermissions = this.baseMapper.selectByParentId(parentId);
        for (Permission child : childPermissions) {
            // 删除子权限
            this.removeById(child.getPermissionId());
            // 递归删除孙子权限
            removeChildPermissions(child.getPermissionId());
        }
    }

    /**
     * 构建权限树
     *
     * @param permissions 权限列表
     * @param parentId    父级ID
     * @return 权限树
     */
    private List<Permission> buildPermissionTree(List<Permission> permissions, Long parentId) {
        return permissions.stream()
                .filter(permission -> permission.getParentId().equals(parentId))
                .peek(permission -> {
                    List<Permission> children = buildPermissionTree(permissions, permission.getPermissionId());
                    permission.setChildren(children);
                })
                .collect(Collectors.toList());
    }
}