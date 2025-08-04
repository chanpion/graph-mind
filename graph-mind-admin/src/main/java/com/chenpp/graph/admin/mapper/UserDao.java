package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenpp.graph.admin.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author April.Chen
 * @date 2025/8/1 14:30
 */
@Mapper
public interface UserDao extends BaseMapper<User> {
    /**
     * 根据条件分页查询用户列表
     *
     * @param page        分页对象
     * @param username    用户名
     * @param phoneNumber 手机号码
     * @param status      状态
     * @return 用户分页列表
     */
    IPage<User> selectUserPage(Page<User> page, @Param("username") String username, 
                               @Param("phoneNumber") String phoneNumber, @Param("status") Integer status);
}