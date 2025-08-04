package com.chenpp.graph.admin.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chenpp.graph.admin.mapper.UserDao;
import com.chenpp.graph.admin.mapper.UserRoleDao;
import com.chenpp.graph.admin.mapper.RolePermissionDao;
import com.chenpp.graph.admin.mapper.PermissionDao;
import com.chenpp.graph.admin.model.User;
import com.chenpp.graph.admin.model.Role;
import com.chenpp.graph.admin.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/8/1 17:50
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserDao userDao;
    private final UserRoleDao userRoleDao;
    private final RolePermissionDao rolePermissionDao;
    private final PermissionDao permissionDao;

    public UserDetailsServiceImpl(UserDao userDao, UserRoleDao userRoleDao, RolePermissionDao rolePermissionDao, PermissionDao permissionDao) {
        this.userDao = userDao;
        this.userRoleDao = userRoleDao;
        this.rolePermissionDao = rolePermissionDao;
        this.permissionDao = permissionDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("username", username);
        User user = userDao.selectOne(userQueryWrapper);

        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 获取用户的角色和权限
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 根据用户ID查询角色列表
        List<Role> roles = userRoleDao.selectRolesByUserId(user.getUserId());
        
        // 检查是否为管理员角色
        boolean isAdmin = roles.stream().anyMatch(role -> "admin".equals(role.getRoleKey()));
        
        if (isAdmin) {
            // 管理员拥有所有权限
            authorities.add(new SimpleGrantedAuthority("ROLE_admin"));
            
            // 查询所有权限
            List<Permission> allPermissions = permissionDao.selectList(null);
            allPermissions.forEach(permission -> {
                if (permission.getPermissionKey() != null && !permission.getPermissionKey().isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getPermissionKey()));
                }
            });
        } else {
            // 普通用户根据角色查询权限
            List<Permission> permissions = new ArrayList<>();
            for (Role role : roles) {
                // 添加角色权限
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleKey()));
                
                // 查询该角色拥有的权限
                List<Permission> rolePermissions = rolePermissionDao.selectPermissionsByRoleId(role.getRoleId());
                permissions.addAll(rolePermissions);
            }
            
            // 添加权限到authorities列表
            permissions.forEach(permission -> {
                if (permission.getPermissionKey() != null && !permission.getPermissionKey().isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getPermissionKey()));
                }
            });
        }

        // 返回UserDetails实现类
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