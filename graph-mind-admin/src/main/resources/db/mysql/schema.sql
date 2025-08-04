/*
MySQL Database Schema
*/

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(30) NOT NULL COMMENT '用户账号',
  `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `phone_number` varchar(11) DEFAULT '' COMMENT '手机号码',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `sex` tinyint DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `status` tinyint DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE KEY `uniq_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '角色状态（0正常 1停用）',
  `data_scope` tinyint DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` VALUES (1, '管理员', 'admin', 1, 0, 1, '管理员角色', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_role` VALUES (2, '普通用户', 'user', 2, 0, 5, '普通用户角色', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父权限ID',
  `permission_type` char(1) NOT NULL DEFAULT 'M' COMMENT '权限类型（M目录 C菜单 F按钮）',
  `permission_key` varchar(100) DEFAULT '' COMMENT '权限标识',
  `component` varchar(255) DEFAULT '' COMMENT '组件路径',
  `path` varchar(255) DEFAULT '' COMMENT '路由地址',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `visible` tinyint NOT NULL DEFAULT '0' COMMENT '是否显示（0显示 1隐藏）',
  `cache` tinyint NOT NULL DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
BEGIN;
-- 系统管理菜单
INSERT INTO `sys_permission` VALUES (1, '系统管理', 0, 'M', '', '', '/system', 'system', 1, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');

-- 用户管理菜单及按钮权限
INSERT INTO `sys_permission` VALUES (2, '用户管理', 1, 'C', 'system:user:list', 'system/user/index', '/system/user', 'user', 1, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (3, '用户新增', 2, 'F', 'system:user:add', '', '', '', 1, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (4, '用户修改', 2, 'F', 'system:user:edit', '', '', '', 2, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (5, '用户删除', 2, 'F', 'system:user:remove', '', '', '', 3, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');

-- 角色管理菜单及按钮权限
INSERT INTO `sys_permission` VALUES (6, '角色管理', 1, 'C', 'system:role:list', 'system/role/index', '/system/role', 'peoples', 2, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (7, '角色新增', 6, 'F', 'system:role:add', '', '', '', 1, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (8, '角色修改', 6, 'F', 'system:role:edit', '', '', '', 2, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (9, '角色删除', 6, 'F', 'system:role:remove', '', '', '', 3, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');

-- 权限管理菜单及按钮权限
INSERT INTO `sys_permission` VALUES (10, '权限管理', 1, 'C', 'system:permission:list', 'system/permission/index', '/system/permission', 'lock', 3, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (11, '权限新增', 10, 'F', 'system:permission:add', '', '', '', 1, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (12, '权限修改', 10, 'F', 'system:permission:edit', '', '', '', 2, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` VALUES (13, '权限删除', 10, 'F', 'system:permission:remove', '', '', '', 3, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_user_role` VALUES (1, 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和权限关联表';

-- ----------------------------
-- Records of sys_role_permission
-- ----------------------------
BEGIN;
-- 管理员角色拥有所有权限
INSERT INTO `sys_role_permission` VALUES (1, 1);
INSERT INTO `sys_role_permission` VALUES (1, 2);
INSERT INTO `sys_role_permission` VALUES (1, 3);
INSERT INTO `sys_role_permission` VALUES (1, 4);
INSERT INTO `sys_role_permission` VALUES (1, 5);
INSERT INTO `sys_role_permission` VALUES (1, 6);
INSERT INTO `sys_role_permission` VALUES (1, 7);
INSERT INTO `sys_role_permission` VALUES (1, 8);
INSERT INTO `sys_role_permission` VALUES (1, 9);
INSERT INTO `sys_role_permission` VALUES (1, 10);
INSERT INTO `sys_role_permission` VALUES (1, 11);
INSERT INTO `sys_role_permission` VALUES (1, 12);
INSERT INTO `sys_role_permission` VALUES (1, 13);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for graph_database_connection
-- ----------------------------
DROP TABLE IF EXISTS `graph_database_connection`;
CREATE TABLE `graph_database_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '连接ID',
  `name` varchar(255) NOT NULL COMMENT '连接名称',
  `type` varchar(50) NOT NULL COMMENT '数据库类型',
  `host` varchar(255) NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口号',
  `database` varchar(255) DEFAULT NULL COMMENT '数据库名',
  `username` varchar(255) DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `status` int DEFAULT '0' COMMENT '状态（0: disconnected, 1: connected, 2: connecting）',
  `pool_size` int DEFAULT NULL COMMENT '连接池大小',
  `timeout` int DEFAULT NULL COMMENT '超时时间',
  `description` text COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图数据库连接配置表';

-- ----------------------------
-- Table structure for graph
-- ----------------------------
DROP TABLE IF EXISTS `graph`;
CREATE TABLE `graph` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图ID',
  `name` varchar(255) NOT NULL COMMENT '图名称',
  `code` varchar(100) NOT NULL COMMENT '图编码',
  `description` text COMMENT '图描述',
  `status` int DEFAULT '1' COMMENT '状态（0: 禁用, 1: 启用）',
  `connection_id` bigint NOT NULL COMMENT '关联的图数据库连接ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_code` (`code`) USING BTREE,
  KEY `idx_connection_id` (`connection_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图信息表';
