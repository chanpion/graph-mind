/*
H2 Database Schema
*/

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(30) NOT NULL COMMENT '用户账号',
  nickname VARCHAR(30) NOT NULL COMMENT '用户昵称',
  password VARCHAR(100) DEFAULT '' COMMENT '密码',
  phone_number VARCHAR(11) DEFAULT '' COMMENT '手机号码',
  email VARCHAR(50) DEFAULT '' COMMENT '用户邮箱',
  sex TINYINT DEFAULT 0 COMMENT '用户性别（0男 1女 2未知）',
  status TINYINT DEFAULT 0 COMMENT '帐号状态（0正常 1停用）',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT uniq_username UNIQUE (username)
);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  role_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_name VARCHAR(30) NOT NULL COMMENT '角色名称',
  role_key VARCHAR(100) NOT NULL COMMENT '角色权限字符串',
  role_sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '角色状态（0正常 1停用）',
  data_scope TINYINT DEFAULT 1 COMMENT '数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (role_id)
);


-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
  permission_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID',
  permission_type CHAR(1) NOT NULL DEFAULT 'M' COMMENT '权限类型（M目录 C菜单 F按钮）',
  permission_key VARCHAR(100) DEFAULT '' COMMENT '权限标识',
  component VARCHAR(255) DEFAULT '' COMMENT '组件路径',
  path VARCHAR(255) DEFAULT '' COMMENT '路由地址',
  icon VARCHAR(100) DEFAULT '#' COMMENT '菜单图标',
  order_num INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '菜单状态（0正常 1停用）',
  visible TINYINT NOT NULL DEFAULT 0 COMMENT '是否显示（0显示 1隐藏）',
  cache TINYINT NOT NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (permission_id)
);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
);

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO sys_user_role VALUES (1, 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (role_id, permission_id)
);

-- ----------------------------
-- Table structure for graph_database_connection
-- ----------------------------
DROP TABLE IF EXISTS graph_database_connection;
CREATE TABLE graph_database_connection (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '连接ID',
  name VARCHAR(255) NOT NULL COMMENT '连接名称',
  type VARCHAR(50) NOT NULL COMMENT '数据库类型',
  host VARCHAR(255) NOT NULL COMMENT '主机地址',
  port INT NOT NULL COMMENT '端口号',
  database VARCHAR(255) DEFAULT NULL COMMENT '数据库名',
  username VARCHAR(255) DEFAULT NULL COMMENT '用户名',
  password VARCHAR(255) DEFAULT NULL COMMENT '密码',
  status INT DEFAULT 0 COMMENT '状态（0: disconnected, 1: connected, 2: connecting）',
  pool_size INT DEFAULT NULL COMMENT '连接池大小',
  timeout INT DEFAULT NULL COMMENT '超时时间',
  description TEXT COMMENT '描述',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for graph
-- ----------------------------
DROP TABLE IF EXISTS graph;
CREATE TABLE graph (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图ID',
  name VARCHAR(255) NOT NULL COMMENT '图名称',
  code VARCHAR(100) NOT NULL COMMENT '图编码',
  description TEXT COMMENT '图描述',
  status INT DEFAULT 1 COMMENT '状态（0: 禁用, 1: 启用）',
  connection_id BIGINT NOT NULL COMMENT '关联的图数据库连接ID',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  CONSTRAINT uniq_code UNIQUE (code)
);
