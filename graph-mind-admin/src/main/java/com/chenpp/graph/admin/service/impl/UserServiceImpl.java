package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.UserDao;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.User;
import com.chenpp.graph.admin.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> userIPage = this.baseMapper.selectUserPage(page, username, phoneNumber, status);
        return new PageResult<>(userIPage.getRecords(), userIPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public User getUserById(Long userId) {
        return this.getById(userId);
    }

    @Override
    public User getUserByName(String username) {
        return this.getOne(new QueryWrapper<>(User.class).eq("username", username));
    }


    @Override
    public void addUser(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        this.save(user);
    }

    @Override
    public void updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        this.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteUsers(List<Long> userIds) {
        this.removeBatchByIds(userIds);
    }
}