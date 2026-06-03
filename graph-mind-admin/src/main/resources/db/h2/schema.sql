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
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT uniq_username UNIQUE (username)
);

-- ----------------------------
-- Table structure for graph_connection
-- ----------------------------
DROP TABLE IF EXISTS graph_connection;
CREATE TABLE graph_connection (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '连接ID',
  name VARCHAR(255) NOT NULL COMMENT '连接名称',
  graph_type VARCHAR(50) NOT NULL COMMENT '数据库类型',
  hosts VARCHAR(255) NOT NULL COMMENT '主机地址',
  port INT NOT NULL COMMENT '端口号',
  username VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  password VARCHAR(255) DEFAULT NULL COMMENT '密码',
  status INT DEFAULT 0 COMMENT '状态（0: disconnected, 1: connected, 2: connecting）',
  description TEXT COMMENT '描述',
  params VARCHAR(2000) DEFAULT NULL COMMENT '其他参数',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
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
  status INT DEFAULT 0 COMMENT '状态（0: 正常, 1: 异常, 2: 未知）',
  connection_id BIGINT NOT NULL COMMENT '关联的图数据库连接ID',
  graph_type VARCHAR(20) DEFAULT NULL COMMENT '图数据库类型：NEO4J、NEBULA、JANUS',
  creator VARCHAR(255) DEFAULT NULL COMMENT '创建人',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  CONSTRAINT uniq_code UNIQUE (code)
);
CREATE INDEX idx_connection_id ON graph(connection_id);

-- ----------------------------
-- Table structure for graph_vertex_def
-- ----------------------------
DROP TABLE IF EXISTS graph_vertex_def;
CREATE TABLE graph_vertex_def (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '节点定义ID',
  graph_id BIGINT NOT NULL COMMENT '图ID',
  name VARCHAR(50) NOT NULL COMMENT '节点类型名称',
  label VARCHAR(20) DEFAULT NULL COMMENT '标识',
  description TEXT COMMENT '描述',
  status TINYINT DEFAULT 0 COMMENT '状态：0-未发布，1-已发布',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_vertex_def_graph_id ON graph_vertex_def(graph_id);

-- ----------------------------
-- Table structure for graph_edge_def
-- ----------------------------
DROP TABLE IF EXISTS graph_edge_def;
CREATE TABLE graph_edge_def (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '边定义ID',
  graph_id BIGINT NOT NULL COMMENT '图ID',
  name VARCHAR(50) NOT NULL COMMENT '边类型名称',
  label VARCHAR(20) DEFAULT NULL COMMENT '边标识',
  start_label VARCHAR(255) NOT NULL COMMENT '起点类型',
  end_label VARCHAR(255) NOT NULL COMMENT '终点类型',
  description TEXT COMMENT '描述',
  status TINYINT DEFAULT 0 COMMENT '状态：0-未发布，1-已发布',
  multiple TINYINT DEFAULT 0 COMMENT '是否是多边',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_edge_def_graph_id ON graph_edge_def(graph_id);

-- ----------------------------
-- Table structure for graph_property_def
-- ----------------------------
DROP TABLE IF EXISTS graph_property_def;
CREATE TABLE graph_property_def (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  graph_id BIGINT DEFAULT NULL COMMENT '图ID',
  entity_id BIGINT NOT NULL COMMENT '节点定义ID或边定义ID',
  code VARCHAR(255) NOT NULL COMMENT '属性标识',
  name VARCHAR(255) NOT NULL COMMENT '属性名',
  graph_type VARCHAR(50) NOT NULL COMMENT '属性类型',
  `desc` TEXT COMMENT '属性描述',
  status TINYINT DEFAULT 0 COMMENT '状态：0-未发布，1-已发布',
  indexed TINYINT NOT NULL DEFAULT 0 COMMENT '是否索引：1-是，0-否',
  property_type VARCHAR(10) DEFAULT NULL COMMENT '属性类型标记：node-节点属性，edge-边属性',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_property_def_entity_id ON graph_property_def(entity_id);
CREATE INDEX idx_property_def_type ON graph_property_def(property_type);

-- ----------------------------
