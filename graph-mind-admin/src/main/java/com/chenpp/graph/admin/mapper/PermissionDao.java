package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenpp.graph.admin.model.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 14:40
 */
@Mapper
public interface PermissionDao extends BaseMapper<Permission> {
    /**
     * 根据父权限ID查询子权限列表
     *
     * @param parentId 父权限ID
     * @return 子权限列表
     */
    List<Permission> selectByParentId(@Param("parentId") Long parentId);
}