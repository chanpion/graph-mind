/*
MySQL Database Data
*/

-- ----------------------------
-- 初始化用户数据
-- ----------------------------
BEGIN;
-- 密码: admin123 (已使用BCrypt加密)
INSERT INTO `sys_user` VALUES (1, 'admin', '管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800138000', 'admin@example.com', 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- 初始化角色数据
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` VALUES (1, '管理员', 'admin', 1, 0, 1, '管理员角色', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_role` VALUES (2, '普通用户', 'user', 2, 0, 5, '普通用户角色', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- 初始化权限数据
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
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_user_role` VALUES (1, 1);
COMMIT;

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

-- ----------------------------
-- 初始化图数据库连接数据
-- ----------------------------
BEGIN;
INSERT INTO `graph_database_connection` VALUES (1, 'Neo4j测试环境', 'neo4j', '192.168.1.100', 7687, 'neo4j', 'neo4j', 'password', 1, 10, 30, '用于测试的Neo4j数据库', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `graph_database_connection` VALUES (2, 'Nebula生产环境', 'nebula', '192.168.1.101', 9669, 'nebula', 'root', 'nebula', 0, 20, 60, '生产环境Nebula数据库', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `graph_database_connection` VALUES (3, 'Janus开发环境', 'janus', '192.168.1.102', 8182, 'janus', 'admin', 'admin', 2, 15, 45, '开发环境Janus数据库', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- 初始化图数据
-- ----------------------------
BEGIN;
INSERT INTO `graph` VALUES (1, '用户关系图', 'user_relation', '用户之间的关系图谱', 1, 1, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `graph` VALUES (2, '商品知识图谱', 'product_kg', '商品相关的知识图谱', 1, 2, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `graph` VALUES (3, '企业图谱', 'company_graph', '企业相关信息图谱', 1, 3, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;
