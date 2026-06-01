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
-- 初始化用户数据
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` VALUES (1, 'admin', '管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800138000', 'admin@example.com', 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- 初始化角色数据
-- ----------------------------
BEGIN;

-- ----------------------------
-- 初始化权限数据
-- ----------------------------
BEGIN;
-- 系统管理菜单

-- ----------------------------
-- 初始化图数据库连接数据
-- ----------------------------
BEGIN;
INSERT INTO graph_connection (id, name, graph_type, hosts, port, username, password, status, description, params, create_time, update_time) VALUES (1, 'Neo4j测试环境', 'NEO4J', '192.168.1.100', 7687, 'neo4j', 'password', 1, '用于测试的Neo4j数据库', '{"username":"neo4j","password":"password"}', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph_connection (id, name, graph_type, hosts, port, username, password, status, description, params, create_time, update_time) VALUES (2, 'Nebula生产环境', 'NEBULA', '192.168.1.101', 9669, 'root', 'nebula', 0, '生产环境Nebula数据库', '{"username":"root","password":"nebula"}', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph_connection (id, name, graph_type, hosts, port, username, password, status, description, params, create_time, update_time) VALUES (3, 'Janus开发环境', 'JANUS', '192.168.1.102', 8182, 'admin', 'admin', 2, '开发环境Janus数据库', '{"username":"admin","password":"admin","storageBackend":"cql"}', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- 初始化图数据
-- ----------------------------
BEGIN;
INSERT INTO graph (id, name, code, description, status, connection_id, graph_type, creator, create_time, update_time) VALUES (1, '用户关系图', 'user_relation', '用户之间的关系图谱', 1, 1, 'NEO4J', 'admin', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph (id, name, code, description, status, connection_id, graph_type, creator, create_time, update_time) VALUES (2, '商品知识图谱', 'product_kg', '商品相关的知识图谱', 1, 2, 'NEBULA', 'admin', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph (id, name, code, description, status, connection_id, graph_type, creator, create_time, update_time) VALUES (3, '企业图谱', 'company_graph', '企业相关信息图谱', 1, 3, 'JANUS', 'admin', '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;

-- ----------------------------
-- 初始化用户和角色关联数据
-- ----------------------------
BEGIN;

-- ----------------------------
-- 初始化角色和权限关联数据
-- ----------------------------
BEGIN;
-- 管理员角色拥有所有权限

-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;