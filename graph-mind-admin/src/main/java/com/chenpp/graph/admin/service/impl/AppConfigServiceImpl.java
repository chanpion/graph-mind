package com.chenpp.graph.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenpp.graph.admin.mapper.AppConfigDao;
import com.chenpp.graph.admin.model.AppConfig;
import com.chenpp.graph.admin.service.AppConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现类
 */
@Service
public class AppConfigServiceImpl extends ServiceImpl<AppConfigDao, AppConfig> implements AppConfigService {

    @Override
    public IPage<AppConfig> queryConfigs(Integer page, Integer pageSize, String configKey) {
        QueryWrapper<AppConfig> queryWrapper = new QueryWrapper<>();
        if (configKey != null && !configKey.isEmpty()) {
            queryWrapper.like("config_key", configKey);
        }
        queryWrapper.orderByDesc("update_time");
        return this.page(new Page<>(page, pageSize), queryWrapper);
    }

    @Override
    public String getConfigValue(String configKey) {
        QueryWrapper<AppConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", configKey);
        AppConfig config = this.getOne(queryWrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public boolean saveOrUpdateConfig(AppConfig config) {
        return this.saveOrUpdate(config);
    }

    @Override
    public boolean deleteConfig(Long id) {
        return this.removeById(id);
    }
}