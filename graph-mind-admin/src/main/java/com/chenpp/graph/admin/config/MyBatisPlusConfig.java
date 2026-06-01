package com.chenpp.graph.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * @author April.Chen
 * @date 2025/8/1 14:45
 */
@Configuration
@MapperScan("com.chenpp.graph.admin.mapper")
public class MyBatisPlusConfig {
    /**
     * 配置MyBatis-Plus拦截器
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(Environment environment) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isH2 = Arrays.asList(activeProfiles).contains("h2");
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(isH2 ? DbType.H2 : DbType.MYSQL));
        return interceptor;
    }
}