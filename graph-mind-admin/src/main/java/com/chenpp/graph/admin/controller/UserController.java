package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.model.User;
import com.chenpp.graph.admin.service.UserService;
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
     *
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @param username    用户名
     * @param phoneNumber 手机号
     * @param status      状态
     * @return 用户列表
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
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    public Result<User> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 新增结果
     */
    @PostMapping
    public Result<String> addUser(@RequestBody User user) {
        userService.addUser(user);
        return Result.success("新增用户成功");
    }

    /**
     * 更新用户
     *
     * @param userId 用户ID
     * @param user   用户信息
     * @return 更新结果
     */
    @PutMapping("/{userId}")
    public Result<String> updateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setUserId(userId);
        userService.updateUser(user);
        return Result.success("更新用户成功");
    }

    /**
     * 删除用户
     *
     * @param userIds 用户ID列表
     * @return 删除结果
     */
    @DeleteMapping
    public Result<String> deleteUsers(@RequestBody List<Long> userIds) {
        userService.deleteUsers(userIds);
        return Result.success("删除用户成功");
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 更新结果
     */
    @PutMapping("/{userId}/status")
    public Result<String> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userService.updateUserStatus(userId, status);
        return Result.success("更新用户状态成功");
    }
}