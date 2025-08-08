# 更新日志

## [1.5.0] - 2024-01-16

### 新增功能
- ✨ **图数据加工模块**
  - 支持CSV文件导入点边数据
  - 提供数据映射配置功能
  - 支持数据预览和导入进度展示
  - 完整的五步导入向导

### 技术改进
- 📦 **依赖升级**
  - 添加 papaparse 库用于CSV解析
  - 更新相关API接口和Mock数据

## [1.4.0] - 2024-01-15

### 图连接管理功能增强
- 🔗 **数据库支持扩展**
  - 支持 Neo4j、Nebula Graph、JanusGraph 三种图数据库
  - 优化数据库类型选择界面，添加标签说明
  - 根据数据库类型自动设置默认端口和数据库名
- 🧪 **连接测试功能**
  - 新增连接测试功能，显示响应时间、版本、节点数、边数
  - 添加测试结果对话框，展示详细的连接信息
- 🔧 **技术架构升级**
  - 引入 Axios 进行 HTTP 请求管理
  - 引入 Mock.js 提供模拟后端数据接口
  - 创建完整的 API 服务层和请求拦截器
  - 实现真实的 CRUD 操作和状态管理

### 菜单结构调整
- 🔄 **菜单重组**
  - 删除"人员管理"菜单
  - 将用户管理、岗位管理、部门管理移动到"系统管理"下
  - 优化菜单层级结构，提高用户体验
  - 更新默认菜单选中状态

## [1.3.0] - 2024-01-15

## [1.2.0] - 2024-01-15

### 新增功能
- ✨ **图库管理模块**
  - 新增图连接管理功能
  - 支持多种图数据库类型（Neo4j、ArangoDB、OrientDB、JanusGraph）
  - 完整的连接配置管理
  - 连接状态监控和操作

### 技术改进
- 🔧 **TypeScript 转 JavaScript**
  - 将整个项目从 TypeScript 转换为 JavaScript
  - 移除所有 TypeScript 相关依赖和配置
  - 更新所有 Vue 组件语法
  - 保持功能完整性

- 🔧 **登录系统完善**
  - 添加完整的登录页面
  - 实现用户状态管理
  - 添加路由守卫
  - 支持记住我功能

### 新增文件
- `src/views/graph/Connection.vue` - 图连接管理页面
- `src/api/connection.js` - 图连接管理API服务
- `src/utils/request.js` - Axios请求配置
- `src/mock/index.js` - Mock数据配置
- `src/stores/user.js` - 用户状态管理
- `LOGIN_README.md` - 登录功能说明
- `GRAPH_MANAGEMENT_README.md` - 图库管理功能说明

### 修改文件
- `src/utils/commonData.js` - 添加图库管理菜单配置，调整菜单结构
- `src/utils/icons.js` - 添加新图标支持
- `src/router/index.js` - 添加图库管理路由，调整路由结构
- `src/views/Login.vue` - 完善登录页面
- `src/views/Home.vue` - 添加用户信息显示和退出功能，更新默认菜单
- `package.json` - 移除 TypeScript 依赖
- `vite.config.js` - 更新构建配置

### 删除文件
- `vite.config.ts` - 替换为 JavaScript 版本
- `src/main.ts` - 替换为 JavaScript 版本
- `src/router/index.ts` - 替换为 JavaScript 版本
- `src/stores/counter.ts` - 替换为 JavaScript 版本
- `src/stores/tabs.ts` - 替换为 JavaScript 版本
- `src/utils/commonData.ts` - 替换为 JavaScript 版本
- `src/utils/icons.ts` - 替换为 JavaScript 版本
- `tsconfig.json` - TypeScript 配置文件
- `tsconfig.app.json` - TypeScript 配置文件
- `tsconfig.node.json` - TypeScript 配置文件
- `tsconfig.vitest.json` - TypeScript 配置文件
- `env.d.ts` - TypeScript 类型声明
- `vitest.config.ts` - 替换为 JavaScript 版本

## [1.1.0] - 2024-01-14

### 新增功能
- ✨ **基础管理系统**
  - 人员管理模块（用户管理、岗位管理、部门管理）
  - 系统管理模块（角色管理、权限管理）
  - 多页签导航系统
  - 面包屑导航

### 技术特性
- 🎨 **现代化UI设计**
  - 使用 Element Plus 组件库
  - 响应式布局设计
  - 美观的界面风格

- 🔧 **技术栈**
  - Vue 3 + Composition API
  - Vue Router 4
  - Pinia 状态管理
  - Vite 构建工具

## [1.0.0] - 2024-01-13

### 初始版本
- 🎉 **项目初始化**
  - 创建 Vue 3 项目基础结构
  - 配置开发环境
  - 设置基础路由和组件

---

## 开发计划

### 即将推出
- 🔮 **图数据可视化**
  - 图数据展示组件
  - 交互式图编辑器
  - 数据导入导出功能

- 🔮 **查询编辑器**
  - 支持多种图查询语言
  - 语法高亮和自动补全
  - 查询结果展示

- 🔮 **数据管理**
  - 节点和边的管理
  - 批量操作功能
  - 数据备份和恢复

### 长期规划
- 🌟 **高级功能**
  - 图算法分析
  - 性能监控
  - 多用户协作
  - 插件系统

---

## 技术栈

### 前端技术
- **框架**: Vue 3.5.13
- **构建工具**: Vite 6.1.0
- **UI组件库**: Element Plus 2.9.5
- **状态管理**: Pinia 3.0.1
- **路由**: Vue Router 4.5.0
- **图标**: Element Plus Icons

### 开发工具
- **语言**: JavaScript (ES6+)
- **包管理**: npm
- **代码规范**: ESLint
- **测试框架**: Vitest

---

## 贡献指南

### 开发环境设置
1. 克隆项目
2. 安装依赖: `npm install`
3. 启动开发服务器: `npm run dev`
4. 构建生产版本: `npm run build`

### 代码规范
- 使用 Vue 3 Composition API
- 遵循 Element Plus 设计规范
- 保持代码简洁和可读性
- 添加必要的注释和文档

### 提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建过程或辅助工具的变动

---

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。 