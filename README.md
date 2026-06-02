# Graph Mind Platform (GMP)

![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Vue](https://img.shields.io/badge/Vue-3%2B-green)

## 🌟 平台概述

Graph Mind Platform (GMP) 是一款面向企业级的图数据库管理平台，提供统一的、支持多种图数据库（Neo4j、Nebula Graph、JanusGraph）的可视化管理界面，覆盖图数据库连接管理、数据建模、数据管理、图查询、图分析与可视化等全流程能力。本仓库包含后端多模块实现与活跃前端项目。

## 🚀 快速概览（仓库结构）

主要模块说明：

- `graph-mind-core`：核心抽象与域模型
- `graph-mind-common`：公共工具与共享库
- `graph-mind-admin`：Spring Boot 管理后端（admin 服务）
- `graph-mind-neo4j` / `graph-mind-nebula` / `graph-mind-janus`：各图数据库适配实现
- `frontend/`：当前活跃的统一前端（Vue 3 + Vite）
- `graph-mind-studio`：遗留前端（Vue 3，保留供参考）

更详细的模块说明见仓库各子模块目录及 [CLAUDE.md](CLAUDE.md)。

## 🛠️ 环境与快速启动

### 前置要求
- JDK 17+
- Maven 3.6+
- Node.js 16+（前端开发）
- 关系型数据库（MySQL/H2 等）及至少一种图数据库（Neo4j / Nebula / JanusGraph）

### 后端：构建与运行

构建所有模块（跳过测试）：

```bash
mvn clean install -DskipTests
```

运行 admin 服务（开发用 H2 profile）：

```bash
cd graph-mind-admin
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

或使用默认配置（MySQL 等）：

```bash
cd graph-mind-admin
mvn spring-boot:run
```

运行单个测试类或方法：

```bash
mvn test -pl <module> -Dtest=<TestClass>
mvn test -pl <module> -Dtest=<TestClass>#testMethod
```

### 前端：开发与构建

当前活跃统一前端 `frontend/`（Vue 3 + Vite + Element Plus）：

```bash
cd frontend
npm install
npm run dev      # 开发服务器（端口 3000）
npm run build    # 生产构建
npm run preview  # 预览生产构建
```

遗留前端 `graph-mind-studio`（Vue 3，保留供参考）：

```bash
cd graph-mind-studio
npm install
npm run dev      # 默认端口 3000
```

更多前端构建与测试命令见各子目录下的 `package.json` 及 [CLAUDE.md](CLAUDE.md)。

## 📚 模块简要说明

| 模块 | 说明 |
|---|---|
| graph-mind-core | 核心抽象、域模型 |
| graph-mind-common | 公共工具、共享依赖 |
| graph-mind-admin | 管理后台、REST API |
| graph-mind-neo4j / nebula / janus | 各图数据库实现 |
| frontend/ | 当前活跃前端（Vue 3 + Vite） |
| graph-mind-studio | 遗留前端（Vue 3，保留供参考） |

## 📖 开发者指南（摘要）

- 扩展数据库适配：在对应模块下实现 `GraphClient` / SPI 并测试。
- 打包发布：使用 Maven 多模块标准流程，遵循仓库的 BOM 管理。
- 文档与变更：新增 API/功能请同步更新子模块 `README` 与顶层文档。

## 📜 许可证
Apache License 2.0

## 🤝 参与贡献

欢迎通过 Issue 与 PR 参与贡献。提交前请确保：

1. 遵循项目代码风格与已有约定；
2. 新功能包含必要的单元测试；
3. README / 子模块文档同步更新。

## 📮 联系方式

- 项目主页：https://github.com/chanpion/graph-mind


