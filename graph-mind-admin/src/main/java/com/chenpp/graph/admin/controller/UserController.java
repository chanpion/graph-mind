package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.User;
import com.chenpp.graph.admin.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口
 *
 * @author April.Chen
 * @date 2025/8/1 10:45
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<PageResult<User>> getUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Integer status) {
        PageResult<User> pageResult = userService.getUsers(pageNum, pageSize, username, phoneNumber, status);
        return Result.success(pageResult);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public Result<User> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 获取当前登录用户信息（基于JWT令牌）
     */
    @GetMapping("/profile")
    public Result<User> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        return Result.success(user);
    }

    /**
     * 更新当前登录用户信息
     */
    @PutMapping("/profile")
    public Result<String> updateCurrentUser(Authentication authentication, @RequestBody User user) {
        user.setUserId(null);
        User existing = userService.getUserByName(authentication.getName());
        if (existing != null) {
            user.setUserId(existing.getUserId());
            userService.updateUser(user);
        }
        return Result.success("更新成功");
    }

    /**
     * 修改当前登录用户密码
     */
    @PutMapping("/profile/password")
    public Result<String> changePassword(Authentication authentication, @RequestBody User user) {
        User existing = userService.getUserByName(authentication.getName());
        if (existing != null) {
            existing.setPassword(user.getPassword());
            userService.updateUser(existing);
        }
        return Result.success("密码修改成功");
    }

    @DeleteMapping("/{userId}")
    public Result<String> deleteUser(@PathVariable Long userId) {
        userService.deleteUsers(List.of(userId));
        return Result.success("删除用户成功");
    }

    /**
     * 新增用户
     */
    @PostMapping
    public Result<String> addUser(@RequestBody User user) {
        userService.addUser(user);
        return Result.success("新增用户成功");
    }

    /**
     * 更新用户
     */
    @PutMapping("/{userId}")
    public Result<String> updateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setUserId(userId);
        userService.updateUser(user);
        return Result.success("更新用户成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping
    public Result<String> deleteUsers(@RequestBody List<Long> userIds) {
        userService.deleteUsers(userIds);
        return Result.success("删除用户成功");
    }

    @PostMapping("/{userId}/password/reset")
    public Result<String> resetPassword(@PathVariable Long userId, @RequestBody User user) {
        user.setUserId(userId);
        userService.updateUser(user);
        return Result.success("更新用户密码成功");
    }
}