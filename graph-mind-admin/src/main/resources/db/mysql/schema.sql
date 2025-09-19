/*
 Navicat Premium Data Transfer

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80025
 Source Host           : localhost:3306
 Source Schema         : graph_mind

 Target Server Type    : MySQL
 Target Server Version : 80025
 File Encoding         : 65001

 Date: 01/08/2025 16:30:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for graph
-- ----------------------------
DROP TABLE IF EXISTS `graph`;
CREATE TABLE `graph` (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '图ID',
                         `name` varchar(255) NOT NULL COMMENT '图名称',
                         `code` varchar(100) NOT NULL COMMENT '图编码',
                         `description` text COMMENT '图描述',
                         `status` int(11) DEFAULT '1' COMMENT '状态（0: 禁用, 1: 启用）',
                         `connection_id` bigint(20) NOT NULL COMMENT '关联的图数据库连接ID',
                         `graph_type` varchar(20) DEFAULT NULL COMMENT '图数据库类型：neo4j、nebula、janus',
                         `creator` varchar(50) DEFAULT NULL,
                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uniq_code` (`code`) USING BTREE,
                         KEY `idx_connection_id` (`connection_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='图信息表';

-- ----------------------------
-- Table structure for graph_database_connection
-- ----------------------------
DROP TABLE IF EXISTS `graph_database_connection`;
CREATE TABLE `graph_database_connection` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '连接ID',
                                             `name` varchar(255) NOT NULL COMMENT '连接名称',
                                             `type` varchar(50) NOT NULL COMMENT '数据库类型',
                                             `host` varchar(255) NOT NULL COMMENT '主机地址',
                                             `port` int(11) NOT NULL COMMENT '端口号',
                                             `status` int(11) DEFAULT '0' COMMENT '状态（0: disconnected, 1: connected, 2: connecting）',
                                             `description` text COMMENT '描述',
                                             `params` varchar(2000) DEFAULT NULL COMMENT '其他参数',
                                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='图数据库连接配置表';

-- ----------------------------
-- Table structure for graph_edge_def
-- ----------------------------
DROP TABLE IF EXISTS `graph_edge_def`;
CREATE TABLE `graph_edge_def` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '边定义ID',
                                  `graph_id` bigint(20) NOT NULL COMMENT '图ID',
                                  `name` varchar(50) NOT NULL COMMENT '边类型名称',
                                  `label` varchar(20) DEFAULT NULL COMMENT '边标识',
                                  `from` varchar(255) NOT NULL COMMENT '起点类型',
                                  `to` varchar(255) NOT NULL COMMENT '终点类型',
                                  `description` text COMMENT '描述',
                                  `status` tinyint(4) DEFAULT '0' COMMENT '状态：0-未发布，1-已发布',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `multiple` tinyint(4) DEFAULT '0' COMMENT '是否是多边',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_graph_id` (`graph_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='图边定义表';

-- ----------------------------
-- Table structure for graph_node_def
-- ----------------------------
DROP TABLE IF EXISTS `graph_node_def`;
CREATE TABLE `graph_node_def` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '节点定义ID',
                                  `graph_id` bigint(20) NOT NULL COMMENT '图ID',
                                  `name` varchar(50) NOT NULL COMMENT '节点类型名称',
                                  `label` varchar(20) DEFAULT NULL COMMENT '标识',
                                  `description` text COMMENT '描述',
                                  `status` tinyint(4) DEFAULT '0' COMMENT '状态：0-未发布，1-已发布',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_graph_id` (`graph_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='图节点定义表';

-- ----------------------------
-- Table structure for graph_property_def
-- ----------------------------
DROP TABLE IF EXISTS `graph_property_def`;
CREATE TABLE `graph_property_def` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '属性ID',
                                      `graph_id` bigint(20) DEFAULT NULL COMMENT '图ID',
                                      `entity_id` bigint(20) NOT NULL COMMENT '节点定义ID或边定义ID',
                                      `code` varchar(255) NOT NULL COMMENT '属性标识',
                                      `name` varchar(255) NOT NULL COMMENT '属性名',
                                      `type` varchar(50) NOT NULL COMMENT '属性类型',
                                      `desc` text COMMENT '属性描述',
                                      `status` tinyint(4) DEFAULT '0' COMMENT '状态：0-未发布，1-已发布',
                                      `indexed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否索引：1-是，0-否',
                                      `property_type` varchar(10) DEFAULT NULL COMMENT '属性类型标记：node-节点属性，edge-边属性',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_entity_id` (`entity_id`) USING BTREE,
                                      KEY `idx_property_type` (`property_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COMMENT='图属性定义表';

-- ----------------------------
-- Table structure for sys_app_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_app_config`;
CREATE TABLE `sys_app_config` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `config_key` varchar(255) NOT NULL COMMENT '配置键',
                                  `config_value` text COMMENT '配置值',
                                  `description` varchar(500) DEFAULT NULL COMMENT '配置描述',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
                                  `permission_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                                  `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
                                  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父权限ID',
                                  `permission_type` char(1) NOT NULL DEFAULT 'M' COMMENT '权限类型（M目录 C菜单 F按钮）',
                                  `permission_key` varchar(100) DEFAULT '' COMMENT '权限标识',
                                  `component` varchar(255) DEFAULT '' COMMENT '组件路径',
                                  `path` varchar(255) DEFAULT '' COMMENT '路由地址',
                                  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
                                  `order_num` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
                                  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
                                  `visible` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否显示（0显示 1隐藏）',
                                  `cache` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`permission_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` varchar(30) NOT NULL COMMENT '角色名称',
                            `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
                            `role_sort` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
                            `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '角色状态（0正常 1停用）',
                            `data_scope` tinyint(4) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
                                       `role_id` bigint(20) NOT NULL COMMENT '角色ID',
                                       `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
                                       PRIMARY KEY (`role_id`,`permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和权限关联表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `username` varchar(30) NOT NULL COMMENT '用户账号',
                            `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
                            `password` varchar(100) DEFAULT '' COMMENT '密码',
                            `phone_number` varchar(11) DEFAULT '' COMMENT '手机号码',
                            `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
                            `sex` tinyint(4) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
                            `status` tinyint(4) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`user_id`) USING BTREE,
                            UNIQUE KEY `uniq_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
                                 `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                 `role_id` bigint(20) NOT NULL COMMENT '角色ID',
                                 PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

SET FOREIGN_KEY_CHECKS = 1;