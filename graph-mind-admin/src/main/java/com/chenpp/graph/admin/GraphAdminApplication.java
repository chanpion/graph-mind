package com.chenpp.graph.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author April.Chen
 * @date 2025/8/1 14:00
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.chenpp.graph")
@MapperScan("com.chenpp.graph.admin.mapper")
@EnableAsync
@EnableAspectJAutoProxy
public class GraphAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphAdminApplication.class, args);
    }
}