# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在此仓库中工作时提供指导。

## 构建与开发命令

### 后端（Java 17，Maven 多模块）
- **构建所有模块（跳过测试）：** `mvn clean install -DskipTests -Dreversion=1.0.0-SNAPSHOT`
- **启动管理后台（H2 模式，推荐开发使用）：** `cd graph-mind-admin && mvn spring-boot:run -Dspring-boot.run.profiles=h2`
- **启动管理后台（MySQL 模式）：** `cd graph-mind-admin && mvn spring-boot:run`（默认 profile 为 `mysql`）
- **运行单个测试类：** `mvn test -pl <module> -Dtest=<TestClass>`
- **运行单个测试方法：** `mvn test -pl <module> -Dtest=<TestClass>#testMethod`

### 前端 — frontend/（Vue 3 + Vite，SPA）
- **安装依赖：** `cd frontend && npm install`
- **启动开发服务器（端口 3000）：** `cd frontend && npm run dev`
- **构建：** `cd frontend && npm run build`
- **运行单元测试：** `cd frontend && npm run test:unit`
- **预览生产构建：** `cd frontend && npm run preview`

### 开发小贴士
- **开发时禁用认证：** 在 `application.yml` 中设置 `auth.enable: false`（默认即为 false）
- **H2 数据库控制台：** 使用 `h2` profile 启动后访问 `http://localhost:18080/h2-console`
- **前端 Mock 模式：** 通过 localStorage 的 `useMock` 开关控制 — 配置在 `frontend/src/config/apiConfig.js`
- **前端 API 基础地址** 默认为 `http://localhost:18080`（配置在 `frontend/.env`）

## 项目架构概览

**Graph Mind Platform (GMP)** 是一个多数据库知识图谱管理平台，通过统一抽象层支持 Neo4j、Nebula Graph 和 JanusGraph。

### 后端模块结构（Maven 多模块，Java 17）

```
graph-mind-dependency    — BOM POM，管理所有依赖版本（Spring Boot 3.4.8、MyBatis-Plus 3.5.12 等）
graph-mind-common        — 共享工具类（Apache Commons、FastJSON2、Lombok）
graph-mind-core          — 核心抽象与领域模型（无 Spring 依赖）
  ├── GraphClient         — 顶层客户端接口：opsForGraph()、opsForGraphData()
  ├── GraphOperations     — 图生命周期：创建/删除/列出图、应用/获取 Schema
  ├── GraphDataOperations — 顶点/边的 CRUD、查询、扩展、路径查找、摘要
  └── schema/ & model/    — GraphSchema、GraphVertex、GraphEdge、GraphData、GraphPath 等
graph-mind-neo4j          — Neo4j 驱动实现
  └── graph-neo4j-core    — Neo4jClient、CypherBuilder、Neo4jConf
graph-mind-nebula         — Nebula Graph 实现
  └── graph-nebula-core   — NebulaClient、NGQLBuilder、Nebula schema 类型（Tag、Edge、Space）
graph-mind-janus          — JanusGraph 实现（支持 Cassandra/CQL 和 HBase 存储后端）
  └── graph-janus-core    — JanusClient、CassandraConf、HBaseConf
graph-mind-admin          — Spring Boot 管理后端（端口 18080）
  ├── GraphClientFactory   — 工厂类：解析连接配置，实例化具体 GraphClient
  ├── config/              — SecurityConfig（JWT）、WebConfig（CORS）、MyBatisPlusConfig、MyMetaObjectHandler
  ├── controller/          — 7 个 REST 控制器（详见下方 API 表格）
  ├── service/impl/        — 业务逻辑层：GraphService、GraphSchemaService、GraphDataService 等
  ├── mapper/              — 4 个 MyBatis-Plus DAO 接口
  ├── security/            — JwtAuthenticationFilter、UserDetailsServiceImpl
  ├── model/               — 实体与 DTO 类（User、Graph、GraphNodeDef、GraphEdgeDef 等）
  ├── util/                — GraphClientFactory、JwtUtil
  └── resources/
      ├── db/h2/           — H2 建表脚本 + 种子数据（开发 profile）
      ├── db/mysql/        — MySQL 建表脚本 + 种子数据
      └── mapper/          — MyBatis XML 映射文件
```

**关键设计模式**：`GraphClientFactory` 根据数据库类型字符串（"neo4j"、"nebula"、"janus"）创建具体的 `GraphClient` 实现。每个图数据库模块（`graph-mind-neo4j`、`graph-mind-nebula`、`graph-mind-janus`）包含一个 `graph-*-core` 子模块存放实现代码。

### 数据库表结构（MySQL / H2）
**认证相关表**：`sys_user`
**业务表**：`graph_database_connection`（数据库连接配置）、`graph`（图元数据）、`graph_node_def`、`graph_edge_def`、`graph_property_def`（Schema 定义）
**其他**：`app_config`（系统配置）、`operation_log`（操作日志）
开发时使用 `h2` Spring profile 启动 H2 内存数据库（自动从 `schema.sql` 建表）。

### REST API 控制器（graph-mind-admin，端口 18080）
| 控制器 | 路径 | 用途 |
|---|---|---|
| AuthController | /api/auth | 登录、JWT 令牌 |
| UserController | /api/users | 用户 CRUD |
| GraphController | /api/graphs | 图 CRUD |
| GraphConnectionController | /api/connections | 图数据库连接配置 |
| GraphSchemaController | /api/graphs/schema | Schema 发现/应用/获取 |
| GraphDataController | /api/graph/data | 顶点/边 CRUD、导入、摘要 |
| GraphQueryController | /api/graphs/{graphId} | Cypher/原生查询执行 |
| AppConfigController | /api/config | 系统配置 |

### 认证与授权
- **基于 JWT**：`JwtAuthenticationFilter` 从 `Authorization: Bearer <token>` 请求头中提取令牌
- **开发时可关闭认证**：在 `application.yml` 中设置 `auth.enable: false` 即可绕过
- **默认管理员账号**（来自种子数据）：admin / admin123

### 扩展接入新图数据库
1. 在 `graph-mind-dependency/pom.xml` 中添加依赖版本
2. 创建 `graph-mind-<db>/graph-<db>-core` 模块，实现 `GraphClient` 接口
3. 在 `graph-mind-admin/pom.xml` 中添加该模块依赖
4. 在 `GraphClientFactory.createGraphClient()` 中添加新的 case

### 当前活跃前端 — frontend/（Vue 3 + Vite，SPA）
- **技术栈**：Vue 3.5、Element Plus、Pinia（含 persistedstate）、Vue Router 4、Axios、D3.js v7、ECharts v6、Monaco Editor、MockJS、PapaParse
- **Feature 模块结构**：`views/<domain>/` 目录下包含视图组件、`api/`、`stores/`，以及可选的 `components/`
- **路由**（22 个懒加载路由）：公开路由 `/login`，受保护路由 `/home/*`，涵盖仪表盘、图管理、建模、数据、可视化、分析、摘要、连接管理以及系统管理（用户管理、配置、个人资料）
- **布局**：`AppLayout.vue` — 主题切换（黑暗/明亮）、侧边栏导航、标签页系统、用户下拉菜单
- **认证**：基于 JWT 的 `useAuthStore`（Pinia + persist），路由守卫将未登录用户重定向到 `/login`
- **Mock 系统**：通过 `config/apiConfig.js` 控制（localStorage 中的 `useMock` 开关）。Mock 处理器按领域组织在 `src/mock/` 中，通过 request.js 拦截器延迟加载，与 API 层透明协作
- **API 层**：`src/api/request.js` 中的 Axios 实例，包含认证令牌注入和错误处理的拦截器
