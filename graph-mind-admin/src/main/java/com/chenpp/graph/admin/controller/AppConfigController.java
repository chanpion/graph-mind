package com.chenpp.graph.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chenpp.graph.admin.model.AppConfig;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.service.AppConfigService;
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

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/configs")
public class AppConfigController {

    @Autowired
    private AppConfigService appConfigService;

    /**
     * 分页查询配置列表
     *
     * @param page     页码，默认1
     * @param pageSize 每页大小，默认10
     * @param configKey 配置键（模糊查询）
     * @return 配置分页列表
     */
    @GetMapping
    public Result<IPage<AppConfig>> listConfigs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String configKey) {
        IPage<AppConfig> configs = appConfigService.queryConfigs(page, pageSize, configKey);
        return Result.success(configs);
    }

    /**
     * 根据ID获取配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    @GetMapping("/{id}")
    public Result<AppConfig> getConfig(@PathVariable Long id) {
        AppConfig config = appConfigService.getById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        return Result.success(config);
    }

    /**
     * 新增或更新配置
     *
     * @param config 配置信息
     * @return 操作结果
     */
    @PostMapping
    public Result<String> addConfig(@RequestBody AppConfig config) {
        // 检查是否已存在相同key的配置
        if (config.getId() == null) {
            AppConfig existingConfig = getConfigByKey(config.getConfigKey());
            if (existingConfig != null) {
                return Result.error("配置键已存在");
            }
        }
        boolean success = appConfigService.saveOrUpdateConfig(config);
        return success ? Result.success("保存成功") : Result.error("保存失败");
    }

    /**
     * 更新配置
     *
     * @param id     配置ID
     * @param config 配置信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<String> updateConfig(@PathVariable Long id, @RequestBody AppConfig config) {
        config.setId(id);
        boolean success = appConfigService.saveOrUpdateConfig(config);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteConfig(@PathVariable Long id) {
        boolean success = appConfigService.deleteConfig(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 根据配置键获取配置
     *
     * @param configKey 配置键
     * @return 配置信息
     */
    private AppConfig getConfigByKey(String configKey) {
        return appConfigService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AppConfig>()
                .eq("config_key", configKey));
    }
}