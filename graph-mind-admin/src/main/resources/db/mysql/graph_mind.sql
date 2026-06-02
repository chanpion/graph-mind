/*
 Navicat Premium Data Transfer

 Source Server         : 10.58.11.13 3338
 Source Server Type    : MySQL
 Source Server Version : 50736 (5.7.36)
 Source Host           : 10.58.11.13:3338
 Source Schema         : graph_mind

 Target Server Type    : MySQL
 Target Server Version : 50736 (5.7.36)
 File Encoding         : 65001

 Date: 20/09/2025 00:13:55
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
-- Records of graph
-- ----------------------------
BEGIN;
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (2, 'neo4j', 'neo4j', '', 1, 1, 'neo4j', 'admin', '2025-08-04 16:12:42', '2025-08-18 11:06:04');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (3, 'cpp_test_nebula', 'cpp_test_nebula', '', 1, 3, 'nebula', 'admin', '2025-08-13 14:02:15', '2025-08-18 11:06:08');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (5, 'cpp_test_janus', 'cpp_test_janus', '', 1, 4, 'janus', 'anonymousUser', '2025-08-18 19:05:18', '2025-08-19 10:43:38');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (7, 'test_janus_hbase', 'test_janus_hbase', '', 1, 8, 'janus', 'anonymousUser', '2025-08-25 15:50:56', '2025-08-25 16:35:49');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (8, 'test_nebula_1', 'test_nebula_1', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:39:30', '2025-09-17 11:39:30');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (9, 'test_nebula_2', 'test_nebula_2', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:39:42', '2025-09-17 11:39:42');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (10, 'test_nebula_3', 'test_nebula_3', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:39:53', '2025-09-17 11:39:53');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (11, 'test_nebula_4', 'test_nebula_4', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:40:04', '2025-09-17 11:40:04');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (12, 'test_nebula_5', 'test_nebula_5', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:40:14', '2025-09-17 11:40:14');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (13, 'test_nebula_6', 'test_nebula_6', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:40:22', '2025-09-17 11:40:22');
INSERT INTO `graph` (`id`, `name`, `code`, `description`, `status`, `connection_id`, `graph_type`, `creator`, `create_time`, `update_time`) VALUES (14, 'test_nebula_7', 'test_nebula_7', '', 0, 6, 'nebula', 'anonymousUser', '2025-09-17 11:40:31', '2025-09-17 11:40:31');
COMMIT;

-- ----------------------------
-- Table structure for graph_connection
-- ----------------------------
DROP TABLE IF EXISTS `graph_connection`;
CREATE TABLE `graph_connection` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '连接ID',
  `name` varchar(255) NOT NULL COMMENT '连接名称',
  `graph_type` varchar(50) NOT NULL COMMENT '数据库类型',
  `hosts` varchar(255) NOT NULL COMMENT '主机地址',
  `port` int(11) NOT NULL COMMENT '端口号',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `status` int(11) DEFAULT '0' COMMENT '状态（0: disconnected, 1: connected, 2: connecting）',
  `description` text COMMENT '描述',
  `params` varchar(2000) DEFAULT NULL COMMENT '其他参数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='图数据库连接配置表';

-- ----------------------------
-- Records of graph_connection
-- ----------------------------
BEGIN;
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (1, 'neo4j', 'neo4j', '10.57.240.115', 7687, 1, 'test', '{\"database\":\"neo4j\",\"encrypted\":false,\"maxConnectionPoolSize\":10,\"username\":\"neo4j\",\"password\":\"neo4j123\"}', '2025-08-04 16:11:38', '2025-08-25 15:36:19');
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (3, 'nebula', 'nebula', '10.57.36.17,10.57.36.18,10.57.36.19', 9660, 1, '', '{\"metaHosts\":\"10.57.36.17,10.57.36.18,10.57.36.19\",\"username\":\"root\",\"password\":\"nebula\"}', '2025-08-13 13:57:44', '2025-08-25 15:40:39');
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (4, 'janus', 'janus', '10.58.12.60', 9042, 1, '', '{\"storageBackend\":\"cassandra\",\"keyspace\":\"cpp_test\",\"username\":\"cassandra\",\"password\":\"cassandra\"}', '2025-08-13 19:52:10', '2025-08-25 15:35:12');
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (5, 'neo4j-localhost', 'neo4j', 'localhost', 7687, 1, '', '{\"database\":\"neo4j\",\"encrypted\":false,\"maxConnectionPoolSize\":10,\"username\":\"neo4j\",\"password\":\"neo4j123\"}', '2025-08-25 14:23:26', '2025-08-25 14:23:26');
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (6, 'nebula-17', 'nebula', '10.57.36.17,10.57.36.18,10.57.36.19', 9660, 1, '', '{\"timeout\":30,\"poolSize\":10,\"metaHosts\":\"10.57.36.17,10.57.36.18,10.57.36.19\",\"username\":\"root\",\"password\":\"nebula\"}', '2025-08-25 14:35:25', '2025-08-25 14:35:25');
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (7, 'janus-cassandra', 'janus', '10.58.12.60', 9042, 1, '', '{\"storageBackend\":\"cassandra\",\"storageHost\":\"10.58.12.60\",\"storagePort\":9042,\"keyspace\":\"cpp_test\",\"username\":\"cassandra\",\"password\":\"cassandra\"}', '2025-08-25 14:43:50', '2025-08-25 15:07:51');
INSERT INTO `graph_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (8, 'janus-hbase', 'janus', '10.57.36.17,10.57.36.18,10.57.36.19', 2182, 1, '', '{\"storageBackend\":\"hbase\",\"storageHost\":\"10.57.36.17,10.57.36.18,10.57.36.19\",\"storagePort\":2182,\"hbaseNs\":\"cpp\",\"hbaseZnode\":\"/hbase\"}', '2025-08-25 14:47:22', '2025-08-25 15:08:30');
COMMIT;

-- ----------------------------
-- Table structure for graph_edge_def
-- ----------------------------
DROP TABLE IF EXISTS `graph_edge_def`;
CREATE TABLE `graph_edge_def` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '边定义ID',
  `graph_id` bigint(20) NOT NULL COMMENT '图ID',
  `name` varchar(50) NOT NULL COMMENT '边类型名称',
  `label` varchar(20) DEFAULT NULL COMMENT '边标识',
  `start_label` varchar(255) NOT NULL COMMENT '起点类型',
  `end_label` varchar(255) NOT NULL COMMENT '终点类型',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态：0-未发布，1-已发布',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `multiple` tinyint(4) DEFAULT '0' COMMENT '是否是多边',
  PRIMARY KEY (`id`),
  KEY `idx_graph_id` (`graph_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='图边定义表';

-- ----------------------------
-- Records of graph_edge_def
-- ----------------------------
BEGIN;
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `start_label`, `end_label`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (1, 2, '工作', 'work', 'person', 'company', '工作', 0, '2025-08-08 14:29:22', '2025-08-15 17:49:28', 0);
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `start_label`, `end_label`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (2, 3, '工作', 'work', 'person', 'company', '', 1, '2025-08-13 14:03:44', '2025-08-13 15:18:46', 0);
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `start_label`, `end_label`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (3, 5, '工作', 'work', 'person', 'company', '', 1, '2025-08-18 19:06:40', '2025-08-18 20:04:28', 0);
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `start_label`, `end_label`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (4, 7, '朋友', 'friends', 'person', 'person', '', 1, '2025-08-25 15:51:40', '2025-08-25 15:51:40', 0);
COMMIT;

-- ----------------------------
-- Table structure for graph_vertex_def
-- ----------------------------
DROP TABLE IF EXISTS `graph_vertex_def`;
CREATE TABLE `graph_vertex_def` (
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
-- Records of graph_vertex_def
-- ----------------------------
BEGIN;
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (1, 2, '自然人', 'person', '自然人实体', 0, '2025-08-08 13:57:44', '2025-08-15 17:49:32');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (2, 2, '公司', 'company', '公司', 0, '2025-08-08 14:21:41', '2025-08-15 17:49:34');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (4, 3, '自然人', 'person', '', 1, '2025-08-13 14:03:02', '2025-08-13 15:18:11');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (5, 3, '公司', 'company', '', 1, '2025-08-13 14:03:26', '2025-08-13 14:03:26');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (6, 5, '自然人', 'person', '', 1, '2025-08-18 19:05:44', '2025-08-18 20:04:13');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (7, 5, '公司', 'company', '', 1, '2025-08-18 19:06:09', '2025-08-18 20:04:20');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (8, 6, '自然人', 'person', '', 1, '2025-08-19 10:13:57', '2025-08-19 10:14:02');
INSERT INTO `graph_vertex_def` (`id`, `graph_id`, `name`, `start_label`, `description`, `status`, `create_time`, `update_time`) VALUES (9, 7, '自然人', 'person', '', 1, '2025-08-25 15:51:14', '2025-08-25 15:51:14');
COMMIT;

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
  `graph_type` varchar(50) NOT NULL COMMENT '属性类型',
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
-- Records of graph_property_def
-- ----------------------------
BEGIN;
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (1, 2, 1, 'uid', 'uid', 'String', NULL, 0, 1, 'node', '2025-08-08 14:21:19', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (2, 2, 1, 'name', '姓名', 'String', NULL, 0, 0, 'node', '2025-08-08 14:21:19', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (3, 2, 2, 'uid', 'uid', 'String', NULL, 0, 1, 'node', '2025-08-08 14:21:41', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (4, 2, 2, 'name', '名称', 'String', NULL, 0, 0, 'node', '2025-08-08 14:21:41', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (6, 2, 1, 'uid', 'uid', 'String', NULL, 0, 1, 'edge', '2025-08-08 15:07:24', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (7, 2, 1, 'start_time', '开始时间', 'Date', NULL, 0, 0, 'edge', '2025-08-08 15:07:24', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (8, 3, 4, 'name', '姓名', 'String', NULL, 0, 1, 'node', '2025-08-13 15:18:11', '2025-08-19 15:55:20');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (9, 3, 5, 'name', '公司名称', 'String', NULL, 0, 1, 'node', '2025-08-13 14:03:25', '2025-08-19 15:55:20');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (14, 3, 4, 'uid', 'uid', 'String', NULL, 0, 1, 'node', '2025-08-13 15:18:11', '2025-08-19 15:55:20');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (15, 3, 5, 'uid', '唯一标识', 'string', '节点唯一标识符', 0, 0, 'node', '2025-08-13 15:18:11', '2025-08-19 15:55:20');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (16, 2, 2, 'uid', 'uid', 'String', NULL, 0, 0, 'edge', '2025-08-13 15:18:46', '2025-08-19 15:54:40');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (17, 5, 6, 'uid', '唯一标识', 'String', '节点唯一标识符', 0, 0, 'node', '2025-08-18 19:06:50', '2025-08-19 15:56:01');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (18, 5, 6, 'name', '姓名', 'String', NULL, 0, 0, 'node', '2025-08-18 19:06:50', '2025-08-19 15:56:01');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (19, 5, 6, 'age', '年龄', 'Int', NULL, 0, 0, 'node', '2025-08-18 19:06:50', '2025-08-19 15:56:01');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (20, 5, 7, 'uid', '唯一标识', 'String', '节点唯一标识符', 0, 0, 'node', '2025-08-18 19:06:54', '2025-08-19 15:56:01');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (21, 5, 7, 'name', '名称', 'String', NULL, 0, 0, 'node', '2025-08-18 19:06:55', '2025-08-19 15:56:01');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (22, 5, 3, 'uid', '唯一标识', 'String', '边唯一标识符', 0, 0, 'edge', '2025-08-18 19:06:39', '2025-08-19 15:57:13');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (23, 5, 3, 'since', '起始时间', 'Date', NULL, 0, 0, 'edge', '2025-08-18 19:06:39', '2025-08-19 15:57:14');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (24, 6, 8, 'uid', '唯一标识', 'String', '节点唯一标识符', 0, 1, 'node', '2025-08-19 10:14:02', '2025-08-19 15:57:22');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (25, 6, 8, 'name', '姓名', 'String', NULL, 0, 1, 'node', '2025-08-19 10:14:02', '2025-08-19 15:57:23');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (26, NULL, 9, 'uid', '唯一标识', 'String', '节点唯一标识符', 0, 1, 'node', '2025-08-25 15:51:13', '2025-08-25 15:51:13');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (27, NULL, 9, 'name', '姓名', 'String', NULL, 0, 1, 'node', '2025-08-25 15:51:13', '2025-08-25 15:51:13');
INSERT INTO `graph_property_def` (`id`, `graph_id`, `entity_id`, `code`, `name`, `type`, `desc`, `status`, `indexed`, `property_type`, `create_time`, `update_time`) VALUES (28, NULL, 4, 'uid', '唯一标识', 'String', '边唯一标识符', 0, 1, 'edge', '2025-08-25 15:51:40', '2025-08-25 15:51:40');
COMMIT;

-- ----------------------------
SET FOREIGN_KEY_CHECKS = 1;
