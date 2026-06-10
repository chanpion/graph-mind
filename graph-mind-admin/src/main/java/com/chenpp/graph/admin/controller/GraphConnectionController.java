package com.chenpp.graph.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.GraphConnectionService;
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
public class GraphConnectionController {

    @Autowired
    private GraphConnectionService connectionService;

    @GetMapping
    public Result<Page<GraphConnection>> getConnections(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String graphType) {

        Page<GraphConnection> pageObj = new Page<>(page, pageSize);
        Page<GraphConnection> result = connectionService.queryConnections(pageObj, keyword, graphType);
        return Result.success(result);
    }

    @PostMapping
    public Result<Boolean> createConnection(@RequestBody GraphConnection connection) {
        boolean success = connectionService.save(connection);
        return Result.success(success);
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateConnection(@PathVariable Long id, @RequestBody GraphConnection connection) {
        connection.setId(id);
        connection.setUpdateTime(LocalDateTime.now());
        boolean success = connectionService.updateById(connection);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteConnection(@PathVariable Long id) {
        boolean success = connectionService.removeById(id);
        return Result.success(success);
    }

    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        boolean success = connectionService.testConnection(id);
        return Result.success(success);
    }
}