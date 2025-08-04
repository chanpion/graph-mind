package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.User;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 10:30
 */
public interface UserService extends IService<User> {
    /**
     * 获取用户列表
     *
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @param username    用户名
     * @param phoneNumber 手机号
     * @param status      状态
     * @return 用户列表
     */
    PageResult<User> getUsers(Integer pageNum, Integer pageSize, String username, String phoneNumber, Integer status);

    /**
     * 根据用户ID获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    User getUserById(Long userId);

    /**
     * 新增用户
     *
     * @param user 用户信息
     */
    void addUser(User user);

    /**
     * 更新用户
     *
     * @param user 用户信息
     */
    void updateUser(User user);

    /**
     * 删除用户
     *
     * @param userIds 用户ID列表
     */
    void deleteUsers(List<Long> userIds);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     */
    void updateUserStatus(Long userId, Integer status);
}