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
-- Records of graph_database_connection
-- ----------------------------
BEGIN;
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (1, 'neo4j', 'neo4j', '10.57.240.115', 7687, 1, 'test', '{\"database\":\"neo4j\",\"encrypted\":false,\"maxConnectionPoolSize\":10,\"username\":\"neo4j\",\"password\":\"neo4j123\"}', '2025-08-04 16:11:38', '2025-08-25 15:36:19');
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (3, 'nebula', 'nebula', '10.57.36.17,10.57.36.18,10.57.36.19', 9660, 1, '', '{\"metaHosts\":\"10.57.36.17,10.57.36.18,10.57.36.19\",\"username\":\"root\",\"password\":\"nebula\"}', '2025-08-13 13:57:44', '2025-08-25 15:40:39');
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (4, 'janus', 'janus', '10.58.12.60', 9042, 1, '', '{\"storageBackend\":\"cassandra\",\"keyspace\":\"cpp_test\",\"username\":\"cassandra\",\"password\":\"cassandra\"}', '2025-08-13 19:52:10', '2025-08-25 15:35:12');
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (5, 'neo4j-localhost', 'neo4j', 'localhost', 7687, 1, '', '{\"database\":\"neo4j\",\"encrypted\":false,\"maxConnectionPoolSize\":10,\"username\":\"neo4j\",\"password\":\"neo4j123\"}', '2025-08-25 14:23:26', '2025-08-25 14:23:26');
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (6, 'nebula-17', 'nebula', '10.57.36.17,10.57.36.18,10.57.36.19', 9660, 1, '', '{\"timeout\":30,\"poolSize\":10,\"metaHosts\":\"10.57.36.17,10.57.36.18,10.57.36.19\",\"username\":\"root\",\"password\":\"nebula\"}', '2025-08-25 14:35:25', '2025-08-25 14:35:25');
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (7, 'janus-cassandra', 'janus', '10.58.12.60', 9042, 1, '', '{\"storageBackend\":\"cassandra\",\"storageHost\":\"10.58.12.60\",\"storagePort\":9042,\"keyspace\":\"cpp_test\",\"username\":\"cassandra\",\"password\":\"cassandra\"}', '2025-08-25 14:43:50', '2025-08-25 15:07:51');
INSERT INTO `graph_database_connection` (`id`, `name`, `type`, `host`, `port`, `status`, `description`, `params`, `create_time`, `update_time`) VALUES (8, 'janus-hbase', 'janus', '10.57.36.17,10.57.36.18,10.57.36.19', 2182, 1, '', '{\"storageBackend\":\"hbase\",\"storageHost\":\"10.57.36.17,10.57.36.18,10.57.36.19\",\"storagePort\":2182,\"hbaseNs\":\"cpp\",\"hbaseZnode\":\"/hbase\"}', '2025-08-25 14:47:22', '2025-08-25 15:08:30');
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
-- Records of graph_edge_def
-- ----------------------------
BEGIN;
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `from`, `to`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (1, 2, '工作', 'work', '1', '2', '工作', 0, '2025-08-08 14:29:22', '2025-08-15 17:49:28', 0);
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `from`, `to`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (2, 3, '工作', 'work', '4', '5', '', 1, '2025-08-13 14:03:44', '2025-08-13 15:18:46', 0);
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `from`, `to`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (3, 5, '工作', 'work', '6', '7', '', 1, '2025-08-18 19:06:40', '2025-08-18 20:04:28', 0);
INSERT INTO `graph_edge_def` (`id`, `graph_id`, `name`, `label`, `from`, `to`, `description`, `status`, `create_time`, `update_time`, `multiple`) VALUES (4, 7, '朋友', 'friends', '9', '9', '', 1, '2025-08-25 15:51:40', '2025-08-25 15:51:40', 0);
COMMIT;

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
-- Records of graph_node_def
-- ----------------------------
BEGIN;
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (1, 2, '自然人', 'person', '自然人实体', 0, '2025-08-08 13:57:44', '2025-08-15 17:49:32');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (2, 2, '公司', 'company', '公司', 0, '2025-08-08 14:21:41', '2025-08-15 17:49:34');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (4, 3, '自然人', 'person', '', 1, '2025-08-13 14:03:02', '2025-08-13 15:18:11');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (5, 3, '公司', 'company', '', 1, '2025-08-13 14:03:26', '2025-08-13 14:03:26');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (6, 5, '自然人', 'person', '', 1, '2025-08-18 19:05:44', '2025-08-18 20:04:13');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (7, 5, '公司', 'company', '', 1, '2025-08-18 19:06:09', '2025-08-18 20:04:20');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (8, 6, '自然人', 'person', '', 1, '2025-08-19 10:13:57', '2025-08-19 10:14:02');
INSERT INTO `graph_node_def` (`id`, `graph_id`, `name`, `label`, `description`, `status`, `create_time`, `update_time`) VALUES (9, 7, '自然人', 'person', '', 1, '2025-08-25 15:51:14', '2025-08-25 15:51:14');
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
-- Records of sys_app_config
-- ----------------------------
BEGIN;
INSERT INTO `sys_app_config` (`id`, `config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES (1, 'test', 'test', '22', '2025-08-22 17:41:23', '2025-08-22 17:41:23');
INSERT INTO `sys_app_config` (`id`, `config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES (2, 'key', 'value', '111', '2025-08-25 17:00:03', '2025-08-25 17:00:03');
COMMIT;

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
-- Records of sys_permission
-- ----------------------------
BEGIN;
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (1, '系统管理', 0, 'M', '', '', '/system', 'system', 1, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (2, '用户管理', 1, 'C', 'system:user:list', 'system/user/index', '/system/user', 'user', 1, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (3, '用户新增', 2, 'F', 'system:user:add', '', '', '', 1, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (4, '用户修改', 2, 'F', 'system:user:edit', '', '', '', 2, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (5, '用户删除', 2, 'F', 'system:user:remove', '', '', '', 3, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (6, '角色管理', 1, 'C', 'system:role:list', 'system/role/index', '/system/role', 'peoples', 2, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (7, '角色新增', 6, 'F', 'system:role:add', '', '', '', 1, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (8, '角色修改', 6, 'F', 'system:role:edit', '', '', '', 2, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (9, '角色删除', 6, 'F', 'system:role:remove', '', '', '', 3, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (10, '权限管理', 1, 'C', 'system:permission:list', 'system/permission/index', '/system/permission', 'lock', 3, 0, 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (11, '权限新增', 10, 'F', 'system:permission:add', '', '', '', 1, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (12, '权限修改', 10, 'F', 'system:permission:edit', '', '', '', 2, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_permission` (`permission_id`, `permission_name`, `parent_id`, `permission_type`, `permission_key`, `component`, `path`, `icon`, `order_num`, `status`, `visible`, `cache`, `create_time`, `update_time`) VALUES (13, '权限删除', 10, 'F', 'system:permission:remove', '', '', '', 3, 0, 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

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
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `status`, `data_scope`, `remark`, `create_time`, `update_time`) VALUES (1, '管理员', 'admin', 1, 0, 1, '管理员角色', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `status`, `data_scope`, `remark`, `create_time`, `update_time`) VALUES (2, '普通用户', 'user', 2, 0, 5, '普通用户角色', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

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
-- Records of sys_role_permission
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 1);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 2);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 3);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 4);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 5);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 6);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 7);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 8);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 9);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 10);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 11);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 12);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1, 13);
COMMIT;

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
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`user_id`, `username`, `nickname`, `password`, `phone_number`, `email`, `sex`, `status`, `create_time`, `update_time`) VALUES (1, 'admin', '管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800138000', 'admin@example.com', 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO `sys_user` (`user_id`, `username`, `nickname`, `password`, `phone_number`, `email`, `sex`, `status`, `create_time`, `update_time`) VALUES (4, 'user', 'user', '$2a$10$LISkzl8mBE7iAymC0MK38OPjkrKK7ikSx5egbyYDmOVifHcRyFtea', '13101234567', 'user@example.com', 0, 1, '2025-08-14 19:17:02', '2025-08-14 19:17:02');
COMMIT;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
