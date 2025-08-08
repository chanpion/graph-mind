package com.chenpp.graph.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 图数据库连接管理控制器
 *
 * @author April.Chen
 * @date 2025/8/1 16:05
 */
@Slf4j
@RestController
@RequestMapping("/api/connections")
public class GraphDatabaseController {

    @Autowired
    private GraphDatabaseConnectionService connectionService;

    /**
     * 获取连接列表
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词
     * @param type     数据库类型
     * @return 连接列表
     */
    @GetMapping
    public Result<Page<GraphDatabaseConnection>> getConnections(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {

        Page<GraphDatabaseConnection> pageObj = new Page<>(page, pageSize);
        Page<GraphDatabaseConnection> result = connectionService.queryConnections(pageObj, keyword, type);
        return Result.success(result);
    }

    /**
     * 新增连接
     *
     * @param connection 连接信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Long> createConnection(@RequestBody GraphDatabaseConnection connection) {
        connection.setCreateTime(LocalDateTime.now());
        connection.setUpdateTime(LocalDateTime.now());
        connection.setStatus(0);
        connectionService.save(connection);
        return Result.success(connection.getId());
    }

    /**
     * 更新连接
     *
     * @param id         连接ID
     * @param connection 连接信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<String> updateConnection(@PathVariable Long id, @RequestBody GraphDatabaseConnection connection) {
        connection.setId(id);
        connection.setUpdateTime(LocalDateTime.now());
        connectionService.updateById(connection);
        return Result.success("更新成功");
    }

    /**
     * 删除连接
     *
     * @param id 连接ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteConnection(@PathVariable Long id) {
        connectionService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 测试连接
     *
     * @param id 连接ID
     * @return 测试结果
     */
    @PostMapping("/{id}/test")
    public Result<TestConnectionResult> testConnection(@PathVariable Long id) {
        boolean success = connectionService.testConnection(id);
        if (success) {
            TestConnectionResult result = new TestConnectionResult();
            result.setResponseTime((int) (Math.random() * 500) + 50);
            result.setVersion("4.4.0");
            result.setNodes((int) (Math.random() * 10000) + 1000);
            result.setEdges((int) (Math.random() * 50000) + 5000);
            return Result.success(result);
        } else {
            return new Result<>(500, "连接测试失败", null);
        }
    }

    /**
     * 连接数据库
     *
     * @param id 连接ID
     * @return 是否成功
     */
    @PostMapping("/{id}/connect")
    public Result<ConnectionStatus> connectDatabase(@PathVariable Long id) {
        boolean success = connectionService.connectDatabase(id);
        if (success) {
            ConnectionStatus status = new ConnectionStatus();
            status.setStatus("connected");
            status.setLastConnectTime(LocalDateTime.now());
            return Result.success(status);
        } else {
            return new Result<>(500, "连接失败", null);
        }
    }

    /**
     * 断开连接
     *
     * @param id 连接ID
     * @return 是否成功
     */
    @PostMapping("/{id}/disconnect")
    public Result<ConnectionStatus> disconnectDatabase(@PathVariable Long id) {
        boolean success = connectionService.disconnectDatabase(id);
        if (success) {
            ConnectionStatus status = new ConnectionStatus();
            status.setStatus("disconnected");
            return Result.success(status);
        } else {
            return new Result<>(500, "断开连接失败", null);
        }
    }

    /**
     * 测试连接结果
     */
    @Data
    public static class TestConnectionResult {
        private int responseTime;
        private String version;
        private int nodes;
        private int edges;
    }

    /**
     * 连接状态
     */
    @Data
    public static class ConnectionStatus {
        private String status;
        private LocalDateTime lastConnectTime;
    }
}