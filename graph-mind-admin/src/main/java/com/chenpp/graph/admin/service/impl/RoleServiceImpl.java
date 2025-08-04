package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.RoleDao;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.Role;
import com.chenpp.graph.admin.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 11:30
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleDao, Role> implements RoleService {

    @Override
    public PageResult<Role> getRoles(Integer pageNum, Integer pageSize, String roleName, String roleKey, Integer status) {
        // 创建分页对象
        Page<Role> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        IPage<Role> roleIPage = this.baseMapper.selectRolePage(page, roleName, roleKey, status);
        
        // 封装分页结果
        return new PageResult<>(roleIPage.getRecords(), roleIPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public Role getRoleById(Long roleId) {
        return this.getById(roleId);
    }

    @Override
    public void addRole(Role role) {
        this.save(role);
    }

    @Override
    public void updateRole(Role role) {
        this.updateById(role);
    }

    @Override
    public void deleteRoles(List<Long> roleIds) {
        this.removeBatchByIds(roleIds);
    }

    @Override
    public void updateRoleStatus(Long roleId, Integer status) {
        Role role = new Role();
        role.setRoleId(roleId);
        role.setStatus(status);
        this.updateById(role);
    }

    @Override
    public Integer getRoleDataScope(Long roleId) {
        Role role = this.getById(roleId);
        return role != null ? role.getDataScope() : null;
    }

    @Override
    public void updateRoleDataScope(Long roleId, Integer dataScope) {
        Role role = new Role();
        role.setRoleId(roleId);
        role.setDataScope(dataScope);
        this.updateById(role);
    }
}