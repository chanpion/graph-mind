package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenpp.graph.admin.model.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author April.Chen
 * @date 2025/8/1 14:35
 */
@Mapper
public interface RoleDao extends BaseMapper<Role> {
    /**
     * 根据条件分页查询角色列表
     *
     * @param page     分页对象
     * @param roleName 角色名称
     * @param roleKey  权限字符
     * @param status   状态
     * @return 角色分页列表
     */
    IPage<Role> selectRolePage(Page<Role> page, @Param("roleName") String roleName, 
                               @Param("roleKey") String roleKey, @Param("status") Integer status);
}