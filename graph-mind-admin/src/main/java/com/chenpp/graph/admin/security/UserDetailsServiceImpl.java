package com.chenpp.graph.admin.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chenpp.graph.admin.mapper.UserDao;
import com.chenpp.graph.admin.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户认证服务 — 精简版，仅验证用户名密码
 *
 * @author April.Chen
 * @date 2025/8/1 17:50
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserDao userDao;

    public UserDetailsServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("username", username);
        User user = userDao.selectOne(userQueryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getStatus() != 0)
                .build();
    }
}