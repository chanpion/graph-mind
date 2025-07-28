# API 集成说明

## 概述

本项目已集成 Axios 和 Mock.js，提供了完整的 API 请求管理和模拟数据功能。

## 技术栈

### 1. Axios
- **版本**: 最新版本
- **用途**: HTTP 客户端，处理 API 请求
- **特性**: 请求/响应拦截器、错误处理、自动转换 JSON

### 2. Mock.js
- **版本**: 最新版本
- **用途**: 模拟后端数据接口
- **特性**: 随机数据生成、接口拦截、开发环境模拟

## 文件结构

```
src/
├── api/
│   └── connection.js          # 图连接管理 API
├── utils/
│   └── request.js             # Axios 配置和拦截器
├── mock/
│   └── index.js               # Mock 数据配置
└── main.js                    # 应用入口（引入 mock）
```

## 配置说明

### 1. Axios 配置 (`src/utils/request.js`)

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})
```

**主要特性**:
- 基础 URL 配置
- 请求超时设置
- 自动添加 Authorization 头
- 统一的错误处理
- 响应数据自动解析

### 2. Mock 配置 (`src/mock/index.js`)

```javascript
import Mock from 'mockjs'

// 设置延迟时间
Mock.setup({
  timeout: '200-600'
})

// 注册接口
Mock.mock('/api/connections', 'get', {
  code: 200,
  message: '获取成功',
  data: { /* 模拟数据 */ }
})
```

**主要特性**:
- 随机数据生成
- 接口拦截
- 延迟模拟
- 开发环境自动加载

## API 接口

### 图连接管理接口

#### 1. 获取连接列表
```javascript
GET /api/connections
```

**参数**:
- `page`: 页码
- `pageSize`: 每页数量
- `keyword`: 搜索关键词

**响应**:
```javascript
{
  code: 200,
  message: '获取成功',
  data: {
    list: [...],
    total: 100,
    page: 1,
    pageSize: 10
  }
}
```

#### 2. 新增连接
```javascript
POST /api/connections
```

**请求体**:
```javascript
{
  name: '连接名称',
  type: 'neo4j',
  host: 'localhost',
  port: 7687,
  database: 'neo4j',
  username: 'neo4j',
  password: 'password',
  poolSize: 10,
  timeout: 30,
  description: '连接描述'
}
```

#### 3. 更新连接
```javascript
PUT /api/connections/:id
```

#### 4. 删除连接
```javascript
DELETE /api/connections/:id
```

#### 5. 连接数据库
```javascript
POST /api/connections/:id/connect
```

#### 6. 断开连接
```javascript
POST /api/connections/:id/disconnect
```

#### 7. 测试连接
```javascript
POST /api/connections/:id/test
```

**响应**:
```javascript
{
  code: 200,
  message: '连接测试成功',
  data: {
    responseTime: 150,
    version: '4.4.0',
    nodes: 10000,
    edges: 50000
  }
}
```

## 使用方法

### 1. 在组件中使用 API

```javascript
import connectionApi from '@/api/connection'

// 获取连接列表
const fetchConnections = async () => {
  try {
    const response = await connectionApi.getConnections({
      page: 1,
      pageSize: 10
    })
    console.log(response.data)
  } catch (error) {
    console.error('获取失败:', error)
  }
}

// 新增连接
const createConnection = async (data) => {
  try {
    const response = await connectionApi.createConnection(data)
    console.log('新增成功:', response)
  } catch (error) {
    console.error('新增失败:', error)
  }
}
```

### 2. 错误处理

```javascript
// 请求拦截器自动处理
request.interceptors.response.use(
  response => {
    if (response.data.code === 200) {
      return response.data
    }
    ElMessage.error(response.data.message)
    return Promise.reject(new Error(response.data.message))
  },
  error => {
    // 自动处理网络错误
    console.error('请求失败:', error)
  }
)
```

## 开发环境配置

### 1. 启用 Mock 数据

在 `src/main.js` 中：

```javascript
// 仅在开发环境加载 mock
if (import.meta.env.DEV) {
  import('./mock')
}
```

### 2. 生产环境配置

生产环境中，Mock.js 不会加载，需要配置真实的后端 API 地址：

```javascript
// 在 vite.config.js 中配置代理
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://your-backend-api.com',
        changeOrigin: true
      }
    }
  }
})
```

## 数据库类型配置

### 支持的数据库类型

```javascript
export const databaseTypes = {
  neo4j: {
    name: 'Neo4j',
    defaultPort: 7687,
    description: '最流行的图数据库，支持Cypher查询语言',
    features: ['Cypher查询语言', 'ACID事务', '图算法库', '可视化工具']
  },
  nebula: {
    name: 'Nebula Graph',
    defaultPort: 9669,
    description: '分布式图数据库，支持大规模图数据处理',
    features: ['分布式架构', '高可用性', '图计算引擎', '多租户支持']
  },
  janus: {
    name: 'JanusGraph',
    defaultPort: 8182,
    description: '分布式图数据库，支持多种存储后端',
    features: ['多种存储后端', '图遍历语言', '事务支持', '索引优化']
  }
}
```

## 注意事项

### 1. 安全性
- 生产环境中需要加密敏感信息
- 使用 HTTPS 传输
- 实现适当的认证和授权

### 2. 性能优化
- 使用请求缓存
- 实现请求防抖
- 优化大数据量处理

### 3. 错误处理
- 网络错误处理
- 业务错误处理
- 用户友好的错误提示

### 4. 开发调试
- 使用浏览器开发者工具
- 查看网络请求
- 检查控制台错误

## 扩展功能

### 1. 添加新的 API 接口

```javascript
// 在 src/api/ 目录下创建新的 API 文件
export const newApi = {
  getData() {
    return request.get('/new-endpoint')
  },
  createData(data) {
    return request.post('/new-endpoint', data)
  }
}
```

### 2. 添加新的 Mock 数据

```javascript
// 在 src/mock/index.js 中添加
Mock.mock('/api/new-endpoint', 'get', {
  code: 200,
  message: '获取成功',
  data: {
    'list|10': [{
      'id|+1': 1,
      'name': '@ctitle(5, 10)',
      'status|1': ['active', 'inactive']
    }]
  }
})
```

## 故障排除

### 常见问题

1. **Mock 数据不生效**
   - 检查是否在开发环境
   - 确认 Mock.js 已正确引入
   - 检查接口路径是否正确

2. **请求失败**
   - 检查网络连接
   - 确认 API 地址正确
   - 查看浏览器控制台错误

3. **数据格式错误**
   - 检查请求/响应数据格式
   - 确认 Content-Type 设置正确
   - 验证 JSON 格式

### 调试技巧

1. **查看网络请求**
   - 打开浏览器开发者工具
   - 查看 Network 标签页
   - 检查请求和响应

2. **查看控制台日志**
   - 检查 JavaScript 错误
   - 查看 API 调用日志
   - 确认数据流转

3. **测试 API 接口**
   - 使用 Postman 测试
   - 验证接口响应
   - 检查数据格式 