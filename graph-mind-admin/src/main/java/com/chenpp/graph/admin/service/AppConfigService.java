package com.chenpp.graph.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chenpp.graph.admin.model.AppConfig;

/**
 * 系统配置服务接口
 */
public interface AppConfigService extends IService<AppConfig> {

    /**
     * 分页查询配置列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param configKey 配置键（模糊查询）
     * @return 配置分页列表
     */
    IPage<AppConfig> queryConfigs(Integer page, Integer pageSize, String configKey);

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 更新或保存配置
     *
     * @param config 配置信息
     * @return 是否成功
     */
    boolean saveOrUpdateConfig(AppConfig config);

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteConfig(Long id);
}