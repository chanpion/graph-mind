package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.UserDao;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.User;
import com.chenpp.graph.admin.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/8/1 11:25
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<User> getUsers(Integer pageNum, Integer pageSize, String username, String phoneNumber, Integer status) {
        // 创建分页对象
        Page<User> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        IPage<User> userIPage = this.baseMapper.selectUserPage(page, username, phoneNumber, status);
        
        // 封装分页结果
        return new PageResult<>(userIPage.getRecords(), userIPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public User getUserById(Long userId) {
        return this.getById(userId);
    }

    @Override
    public void addUser(User user) {
        // 对密码进行加密
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        this.save(user);
    }

    @Override
    public void updateUser(User user) {
        // 如果密码不为空，则进行加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        this.updateById(user);
    }

    @Override
    public void deleteUsers(List<Long> userIds) {
        this.removeBatchByIds(userIds);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = new User();
        user.setUserId(userId);
        user.setStatus(status);
        this.updateById(user);
    }
}