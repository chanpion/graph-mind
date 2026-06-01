/*
H2 Database Data
*/

-- ----------------------------
-- 初始化用户数据
-- ----------------------------
-- 密码: admin123 (已使用BCrypt加密)
INSERT INTO sys_user VALUES (1, 'admin', '管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800138000', 'admin@example.com', 0, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');

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
ALTER TABLE graph ALTER COLUMN id RESTART WITH 4;

-- ----------------------------
-- 初始化节点定义数据
-- ----------------------------
BEGIN;
INSERT INTO graph_vertex_def (id, graph_id, name, label, description, status, create_time, update_time) VALUES (1, 1, '用户', 'user', '用户节点', 1, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph_vertex_def (id, graph_id, name, label, description, status, create_time, update_time) VALUES (2, 1, '组织', 'org', '组织节点', 1, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph_vertex_def (id, graph_id, name, label, description, status, create_time, update_time) VALUES (3, 2, '商品', 'product', '商品节点', 1, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph_vertex_def (id, graph_id, name, label, description, status, create_time, update_time) VALUES (4, 2, '分类', 'category', '分类节点', 1, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;
ALTER TABLE graph_vertex_def ALTER COLUMN id RESTART WITH 5;

-- ----------------------------
-- 初始化边定义数据
-- ----------------------------
BEGIN;
INSERT INTO graph_edge_def (id, graph_id, name, label, `from`, `to`, description, status, multiple, create_time, update_time) VALUES (1, 1, '属于', 'belongs_to', '1', '2', '用户属于组织', 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
INSERT INTO graph_edge_def (id, graph_id, name, label, `from`, `to`, description, status, multiple, create_time, update_time) VALUES (2, 2, '属于分类', 'in_category', '3', '4', '商品属于分类', 1, 0, '2025-08-01 10:00:00', '2025-08-01 10:00:00');
COMMIT;
ALTER TABLE graph_edge_def ALTER COLUMN id RESTART WITH 3;
ALTER TABLE sys_user ALTER COLUMN user_id RESTART WITH 2;
ALTER TABLE graph_connection ALTER COLUMN id RESTART WITH 4;