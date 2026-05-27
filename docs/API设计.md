# GraphMind API 设计文档

## 1. API 概述

GraphMind 提供 RESTful API 接口，用于图数据库的管理和操作。API 设计遵循 REST 原则，使用 JSON 格式进行数据交换。

## 2. API 基础信息

- Base URL: `/api`
- 协议: HTTP/HTTPS
- 数据格式: JSON
- 认证方式: JWT Token（通过 `Authorization: Bearer {token}` 请求头传递）

## 3. 通用响应格式

### 统一响应模型（`Result<T>`）

所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

字段说明：
| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 响应码，200 表示成功 |
| `message` | string | 响应消息 |
| `data` | T | 响应数据，类型由具体接口决定 |

### 分页模型（`PageResult<T>`）

分页接口返回 `Result<PageResult<T>>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "records": [],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### MyBatis-Plus 分页（`IPage<T>`）

部分接口使用 MyBatis-Plus 分页，返回格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 4. 认证 API

Base: `/api/auth`

### 4.1 用户登录

- **接口路径**: `POST /api/auth/login`
- **描述**: 用户登录获取 JWT 令牌
- **请求参数**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **响应参数**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "jwt_token_string",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "username": "string"
  }
}
```

### 4.2 用户登出

- **接口路径**: `POST /api/auth/logout`
- **描述**: 用户登出，清除安全上下文
- **请求头**: `Authorization: Bearer {token}`
- **响应参数**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": ""
}
```

---

## 5. 用户管理 API

Base: `/api/users`

### 5.1 获取用户列表

- **接口路径**: `GET /api/users`
- **描述**: 分页查询用户列表
- **请求参数**:
  - `pageNum` (int, query, default=1) — 页码
  - `pageSize` (int, query, default=10) — 每页大小
  - `username` (string, query, optional) — 用户名模糊查询
  - `phoneNumber` (string, query, optional) — 手机号
  - `status` (int, query, optional) — 状态
- **响应**: `Result<PageResult<User>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "records": [
      {
        "userId": 1,
        "username": "admin",
        "phoneNumber": "13800138000",
        "email": "admin@example.com",
        "status": 1
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 5.2 获取用户详情

- **接口路径**: `GET /api/users/{userId}`
- **描述**: 根据 ID 获取用户详情
- **路径参数**: `userId` (Long) — 用户 ID
- **响应**: `Result<User>`

### 5.3 获取当前用户信息

- **接口路径**: `GET /api/users/profile`
- **描述**: 根据用户名获取用户信息
- **请求参数**: `username` (string, query) — 用户名
- **响应**: `Result<User>`

### 5.4 新增用户

- **接口路径**: `POST /api/users`
- **描述**: 新增用户
- **请求参数**:
```json
{
  "username": "string",
  "password": "string",
  "phoneNumber": "string",
  "email": "string",
  "status": 1
}
```
- **响应**: `Result<String>`（消息："新增用户成功"）

### 5.5 更新用户

- **接口路径**: `PUT /api/users/{userId}`
- **描述**: 更新用户信息
- **路径参数**: `userId` (Long)
- **请求参数**: User 对象
- **响应**: `Result<String>`（消息："更新用户成功"）

### 5.6 删除用户

- **接口路径**: `DELETE /api/users/{userId}`
- **描述**: 删除单个用户
- **路径参数**: `userId` (Long)
- **响应**: `Result<String>`

### 5.7 批量删除用户

- **接口路径**: `DELETE /api/users`
- **描述**: 批量删除用户
- **请求参数**:
```json
[1, 2, 3]
```
- **响应**: `Result<String>`

### 5.8 更新用户状态

- **接口路径**: `PUT /api/users/{userId}/status`
- **描述**: 更新用户状态
- **路径参数**: `userId` (Long)
- **请求参数**: `status` (int, query)
- **响应**: `Result<String>`

### 5.9 重置密码

- **接口路径**: `POST /api/users/{userId}/password/reset`
- **描述**: 重置用户密码
- **路径参数**: `userId` (Long)
- **请求参数**: User 对象（含密码）
- **响应**: `Result<String>`

---

## 6. 角色管理 API

Base: `/api/roles`

### 6.1 获取角色列表

- **接口路径**: `GET /api/roles`
- **描述**: 分页查询角色列表
- **请求参数**:
  - `pageNum` (int, query, default=1)
  - `pageSize` (int, query, default=10)
  - `roleName` (string, query, optional)
  - `roleKey` (string, query, optional)
  - `status` (int, query, optional)
- **响应**: `Result<PageResult<Role>>`

### 6.2 获取角色详情

- **接口路径**: `GET /api/roles/{roleId}`
- **描述**: 根据 ID 获取角色详情
- **响应**: `Result<Role>`

### 6.3 新增角色

- **接口路径**: `POST /api/roles`
- **描述**: 新增角色
- **响应**: `Result<String>`

### 6.4 更新角色

- **接口路径**: `PUT /api/roles/{roleId}`
- **描述**: 更新角色信息
- **响应**: `Result<String>`

### 6.5 删除角色

- **接口路径**: `DELETE /api/roles`
- **描述**: 批量删除角色
- **请求参数**: `[roleId1, roleId2, ...]`
- **响应**: `Result<String>`

### 6.6 更新角色状态

- **接口路径**: `PUT /api/roles/{roleId}/status`
- **描述**: 更新角色状态
- **请求参数**: `status` (int, query)
- **响应**: `Result<String>`

### 6.7 获取角色数据权限

- **接口路径**: `GET /api/roles/{roleId}/dataScope`
- **描述**: 获取角色数据权限范围
- **响应**: `Result<Integer>`

### 6.8 更新角色数据权限

- **接口路径**: `PUT /api/roles/{roleId}/dataScope`
- **描述**: 更新角色数据权限范围
- **请求参数**: `dataScope` (int, query)
- **响应**: `Result<String>`

---

## 7. 权限管理 API

Base: `/api/permissions`

### 7.1 获取权限树

- **接口路径**: `GET /api/permissions`
- **描述**: 获取权限菜单树形结构
- **响应**: `Result<List<Permission>>`

### 7.2 获取权限详情

- **接口路径**: `GET /api/permissions/{permissionId}`
- **描述**: 根据 ID 获取权限详情
- **响应**: `Result<Permission>`

### 7.3 新增权限

- **接口路径**: `POST /api/permissions`
- **描述**: 新增权限
- **响应**: `Result<String>`

### 7.4 更新权限

- **接口路径**: `PUT /api/permissions/{permissionId}`
- **描述**: 更新权限信息
- **响应**: `Result<String>`

### 7.5 删除权限

- **接口路径**: `DELETE /api/permissions/{permissionId}`
- **描述**: 删除权限
- **响应**: `Result<String>`

### 7.6 更新权限状态

- **接口路径**: `PUT /api/permissions/{permissionId}/status`
- **描述**: 更新权限状态
- **权限**: `system:permission:edit`
- **请求参数**: `status` (int, query)
- **响应**: `Result<String>`

---

## 8. 图数据库连接管理 API

Base: `/api/connections`

### 8.1 获取连接列表

- **接口路径**: `GET /api/connections`
- **描述**: 分页查询连接列表
- **请求参数**:
  - `page` (int, query, default=1)
  - `pageSize` (int, query, default=10)
  - `keyword` (string, query, optional)
  - `type` (string, query, optional) — 数据库类型
- **响应**: `Result<IPage<GraphDatabaseConnection>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "连接名称",
        "type": "neo4j",
        "host": "127.0.0.1",
        "port": 7687,
        "status": 1,
        "description": "描述",
        "createTime": "2025-08-01T16:05:00",
        "updateTime": "2025-08-01T16:05:00",
        "params": "{}"
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 8.2 新增连接

- **接口路径**: `POST /api/connections`
- **描述**: 创建新的图数据库连接
- **请求参数**:
```json
{
  "name": "连接名称",
  "type": "neo4j",
  "host": "127.0.0.1",
  "port": 7687,
  "description": "描述",
  "params": "{}"
}
```
- **响应**: `Result<Long>`（连接 ID）

### 8.3 更新连接

- **接口路径**: `PUT /api/connections/{id}`
- **描述**: 更新连接信息
- **路径参数**: `id` (Long)
- **响应**: `Result<String>`

### 8.4 删除连接

- **接口路径**: `DELETE /api/connections/{id}`
- **描述**: 删除连接
- **路径参数**: `id` (Long)
- **响应**: `Result<String>`

### 8.5 测试连接

- **接口路径**: `POST /api/connections/{id}/test`
- **描述**: 测试图数据库连接是否可用
- **路径参数**: `id` (Long)
- **响应参数**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "responseTime": 123,
    "version": "4.4.0",
    "nodes": 5000,
    "edges": 15000
  }
}
```

---

## 9. 图管理 API

Base: `/api/graphs`

### 9.1 获取图列表

- **接口路径**: `GET /api/graphs`
- **描述**: 分页查询图列表
- **请求参数**:
  - `page` (int, query, default=1)
  - `pageSize` (int, query, default=10)
  - `keyword` (string, query, optional)
- **响应**: `Result<IPage<Graph>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "图名称",
        "code": "graph_code",
        "description": "描述",
        "status": 0,
        "connectionId": 1,
        "graphType": "PROPERTY_GRAPH",
        "creator": "admin",
        "createTime": "2025-08-01T17:00:00",
        "updateTime": "2025-08-01T17:00:00"
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 9.2 根据连接 ID 获取图列表

- **接口路径**: `GET /api/graphs/connection/{connectionId}`
- **描述**: 根据连接 ID 分页查询图列表
- **路径参数**: `connectionId` (Long)
- **请求参数**:
  - `page` (int, query, default=1)
  - `pageSize` (int, query, default=10)
- **响应**: `Result<IPage<Graph>>`

### 9.3 新增图

- **接口路径**: `POST /api/graphs`
- **描述**: 创建新图
- **请求参数**:
```json
{
  "name": "图名称",
  "code": "graph_code",
  "description": "描述",
  "connectionId": 1,
  "graphType": "PROPERTY_GRAPH"
}
```
- **响应**: `Result<Long>`（图 ID）

### 9.4 获取图详情

- **接口路径**: `GET /api/graphs/{id}`
- **描述**: 获取图详情
- **路径参数**: `id` (Long)
- **响应**: `Result<Graph>`

### 9.5 更新图

- **接口路径**: `PUT /api/graphs/{id}`
- **描述**: 更新图信息
- **路径参数**: `id` (Long)
- **响应**: `Result<String>`

### 9.6 删除图

- **接口路径**: `DELETE /api/graphs/{id}`
- **描述**: 删除图
- **路径参数**: `id` (Long)
- **响应**: `Result<String>`

---

## 10. 图 Schema 管理 API

Base: `/api/graphs/{graphId}`

### 10.1 获取节点定义列表

- **接口路径**: `GET /api/graphs/{graphId}/nodes`
- **描述**: 获取图的所有节点定义
- **路径参数**: `graphId` (Long)
- **请求参数**: `status` (int, query, optional)
- **响应**: `Result<List<GraphNodeDef>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "graphId": 1,
      "label": "Person",
      "name": "人员",
      "description": "人员节点",
      "status": 1,
      "createTime": "2025-08-04T16:00:00",
      "updateTime": "2025-08-04T16:00:00",
      "properties": [
        {
          "id": 1,
          "name": "name",
          "dataType": "STRING",
          "required": true
        }
      ]
    }
  ]
}
```

### 10.2 新增节点定义

- **接口路径**: `POST /api/graphs/{graphId}/nodes`
- **描述**: 新增节点定义（含属性列表）
- **路径参数**: `graphId` (Long)
- **响应**: `Result<String>`

### 10.3 更新节点定义

- **接口路径**: `PUT /api/graphs/{graphId}/nodes/{nodeId}`
- **描述**: 更新节点定义
- **路径参数**: `graphId` (Long), `nodeId` (Long)
- **响应**: `Result<String>`

### 10.4 删除节点定义

- **接口路径**: `DELETE /api/graphs/{graphId}/nodes/{nodeId}`
- **描述**: 删除节点定义
- **路径参数**: `graphId` (Long), `nodeId` (Long)
- **响应**: `Result<String>`

### 10.5 获取边定义列表

- **接口路径**: `GET /api/graphs/{graphId}/edges`
- **描述**: 获取图的所有边定义
- **路径参数**: `graphId` (Long)
- **请求参数**: `status` (int, query, optional)
- **响应**: `Result<List<GraphEdgeDef>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "graphId": 1,
      "label": "KNOWS",
      "name": "认识",
      "from": "Person",
      "to": "Person",
      "description": "认识关系",
      "status": 1,
      "createTime": "2025-08-04T16:10:00",
      "updateTime": "2025-08-04T16:10:00",
      "properties": [],
      "multiple": false
    }
  ]
}
```

### 10.6 新增边定义

- **接口路径**: `POST /api/graphs/{graphId}/edges`
- **描述**: 新增边定义（含属性列表）
- **路径参数**: `graphId` (Long)
- **响应**: `Result<String>`

### 10.7 更新边定义

- **接口路径**: `PUT /api/graphs/{graphId}/edges/{edgeId}`
- **描述**: 更新边定义
- **路径参数**: `graphId` (Long), `edgeId` (Long)
- **响应**: `Result<String>`

### 10.8 删除边定义

- **接口路径**: `DELETE /api/graphs/{graphId}/edges/{edgeId}`
- **描述**: 删除边定义
- **路径参数**: `graphId` (Long), `edgeId` (Long)
- **响应**: `Result<String>`

### 10.9 发布 Schema

- **接口路径**: `POST /api/graphs/{graphId}/publish`
- **描述**: 将图 Schema 发布到图数据库
- **路径参数**: `graphId` (Long)
- **响应**: `Result<String>`

### 10.10 获取 Schema

- **接口路径**: `GET /api/graphs/{graphId}/schema`
- **描述**: 获取已发布的图 Schema 结构
- **路径参数**: `graphId` (Long)
- **响应**: `Result<GraphSchema>`

---

## 11. 图查询 API

Base: `/api/graphs/{graphId}`

### 11.1 执行查询

- **接口路径**: `POST /api/graphs/{graphId}/query`
- **描述**: 执行 Cypher 查询语句
- **路径参数**: `graphId` (Long)
- **请求参数**:
```json
{
  "cypher": "MATCH (n) RETURN n LIMIT 10"
}
```
- **响应**: `Result<GraphData>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "vertices": [],
    "edges": []
  }
}
```

### 11.2 节点展开

- **接口路径**: `POST /api/graphs/{graphId}/expand`
- **描述**: 按节点 ID 展开邻接节点（K 层展开）
- **路径参数**: `graphId` (Long)
- **请求参数**:
```json
{
  "nodeId": "string",
  "depth": 1
}
```
- **响应**: `Result<GraphData>`

### 11.3 路径查询

- **接口路径**: `POST /api/graphs/{graphId}/path`
- **描述**: 查询两点之间的路径
- **路径参数**: `graphId` (Long)
- **请求参数**:
```json
{
  "startNodeId": "string",
  "endNodeId": "string",
  "maxDepth": 5
}
```
- **响应**: `Result<GraphData>`

---

## 12. 图数据管理 API

Base: `/api/graphs/{graphId}`

### 12.1 导入节点数据

- **接口路径**: `POST /api/graphs/{graphId}/nodes/{nodeTypeId}/import`
- **描述**: 通过 CSV 文件导入节点数据
- **路径参数**: `graphId` (Long), `nodeTypeId` (Long)
- **请求类型**: `multipart/form-data`
- **请求参数**:
  - `file` (file) — CSV 文件
  - `headers` (string) — CSV 表头信息（JSON）
  - `mapping` (string) — 字段映射关系（JSON）
  - `data` (string) — 数据内容（JSON）
- **响应**: `Result<ImportResult>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "successCount": 100,
    "failureCount": 2,
    "totalCount": 102,
    "errorMessages": ["第5行数据格式错误"]
  }
}
```

### 12.2 导入边数据

- **接口路径**: `POST /api/graphs/{graphId}/edges/{edgeTypeId}/import`
- **描述**: 通过 CSV 文件导入边数据
- **路径参数**: `graphId` (Long), `edgeTypeId` (Long)
- **请求类型**: `multipart/form-data`
- **请求参数**:
  - `file` (file) — CSV 文件
  - `headers` (string) — CSV 表头信息
  - `mapping` (string) — 字段映射关系
  - `data` (string) — 数据内容
- **响应**: `Result<ImportResult>`

### 12.3 查询节点数据列表

- **接口路径**: `GET /api/graphs/{graphId}/nodes/{nodeTypeId}`
- **描述**: 分页查询指定类型的节点数据
- **路径参数**: `graphId` (Long), `nodeTypeId` (Long)
- **请求参数**: `page` (int, default=1), `size` (int, default=10)
- **响应**: `Result<List<Map<String, Object>>>`

### 12.4 查询边数据列表

- **接口路径**: `GET /api/graphs/{graphId}/edges/{edgeTypeId}`
- **描述**: 分页查询指定类型的边数据
- **路径参数**: `graphId` (Long), `edgeTypeId` (Long)
- **请求参数**: `page` (int, default=1), `size` (int, default=10)
- **响应**: `Result<List<Map<String, Object>>>`

### 12.5 获取节点数据详情

- **接口路径**: `GET /api/graphs/{graphId}/data/nodes/{nodeId}`
- **描述**: 根据 ID 获取节点数据详情
- **路径参数**: `graphId` (Long), `nodeId` (String)
- **响应**: `Result<Map<String, Object>>`

### 12.6 获取边数据详情

- **接口路径**: `GET /api/graphs/{graphId}/data/edges/{edgeId}`
- **描述**: 根据 ID 获取边数据详情
- **路径参数**: `graphId` (Long), `edgeId` (String)
- **响应**: `Result<Map<String, Object>>`

### 12.7 新增节点数据

- **接口路径**: `POST /api/graphs/{graphId}/data/nodes/{nodeTypeId}`
- **描述**: 新增节点数据
- **路径参数**: `graphId` (Long), `nodeTypeId` (Long)
- **请求参数**: `Map<String, Object>` — 节点属性
- **响应**: `Result<Boolean>`

### 12.8 新增边数据

- **接口路径**: `POST /api/graphs/{graphId}/data/edges/{edgeTypeId}`
- **描述**: 新增边数据
- **路径参数**: `graphId` (Long), `edgeTypeId` (Long)
- **响应**: `Result<Boolean>`

### 12.9 更新节点数据

- **接口路径**: `PUT /api/graphs/{graphId}/data/nodes/{nodeId}`
- **描述**: 更新节点数据
- **路径参数**: `graphId` (Long), `nodeId` (String)
- **响应**: `Result<Boolean>`

### 12.10 更新边数据

- **接口路径**: `PUT /api/graphs/{graphId}/data/edges/{edgeId}`
- **描述**: 更新边数据
- **路径参数**: `graphId` (Long), `edgeId` (String)
- **响应**: `Result<Boolean>`

### 12.11 删除节点

- **接口路径**: `DELETE /api/graphs/{graphId}/data/nodes/{nodeId}`
- **描述**: 删除单个节点
- **路径参数**: `graphId` (Long), `nodeId` (String)
- **请求参数**: `label` (string, query, optional)
- **响应**: `Result<Boolean>`

### 12.12 批量删除节点

- **接口路径**: `DELETE /api/graphs/{graphId}/data/nodes`
- **描述**: 批量删除节点
- **路径参数**: `graphId` (Long)
- **请求参数**:
```json
{
  "nodeIds": ["id1", "id2"],
  "label": "Person"
}
```
- **响应**: `Result<Boolean>`

### 12.13 获取图统计信息

- **接口路径**: `GET /api/graphs/{graphId}/summary`
- **描述**: 获取图的统计信息（节点数、边数、各类别数量等）
- **路径参数**: `graphId` (Long)
- **响应**: `Result<GraphSummary>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "graphCode": "graph_code",
    "graphName": "图名称",
    "vertexCount": 1000,
    "edgeCount": 5000,
    "vertexLabelCount": {
      "Person": 500,
      "Company": 500
    },
    "edgeLabelCount": {
      "KNOWS": 3000,
      "OWNS": 2000
    }
  }
}
```

---

## 13. 系统配置 API

Base: `/api/configs`

### 13.1 获取配置列表

- **接口路径**: `GET /api/configs`
- **描述**: 分页查询系统配置列表
- **请求参数**:
  - `page` (int, query, default=1)
  - `pageSize` (int, query, default=10)
  - `configKey` (string, query, optional)
- **响应**: `Result<IPage<AppConfig>>`

### 13.2 获取配置详情

- **接口路径**: `GET /api/configs/{id}`
- **描述**: 根据 ID 获取配置详情
- **路径参数**: `id` (Long)
- **响应**: `Result<AppConfig>`

### 13.3 新增或更新配置

- **接口路径**: `POST /api/configs`
- **描述**: 新增配置（若 ID 为空则检查 configKey 唯一性）
- **响应**: `Result<String>`

### 13.4 更新配置

- **接口路径**: `PUT /api/configs/{id}`
- **描述**: 更新配置
- **路径参数**: `id` (Long)
- **响应**: `Result<String>`

### 13.5 删除配置

- **接口路径**: `DELETE /api/configs/{id}`
- **描述**: 删除配置
- **路径参数**: `id` (Long)
- **响应**: `Result<String>`

---

## 14. 错误码定义

### HTTP 标准错误码

| 错误码 | 描述 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 405 | 请求方法不被允许 |
| 408 | 请求超时 |
| 500 | 服务器内部错误 |
| 501 | 功能未实现 |
| 502 | 网关错误 |
| 503 | 服务不可用 |
| 504 | 网关超时 |

### 核心错误码（1001-1008）

| 错误码 | 描述 |
|--------|------|
| 1001 | 连接失败 |
| 1002 | 查询执行失败 |
| 1003 | Schema 校验失败 |
| 1004 | 事务执行失败 |
| 1005 | 不支持的操作 |
| 1006 | 无效的配置 |
| 1007 | 批量操作失败 |
| 1008 | 版本冲突 |

### 业务错误码（2001-2012）

| 错误码 | 描述 |
|--------|------|
| 2001 | 用户不存在 |
| 2002 | 用户已存在 |
| 2003 | 角色不存在 |
| 2004 | 权限不足 |
| 2005 | 用户名或密码错误 |
| 2006 | 账户已被禁用 |
| 2007 | 图不存在 |
| 2008 | 图已存在 |
| 2009 | 无效的 Schema 定义 |
| 2010 | 图数据库连接失败 |
| 2011 | 图查询失败 |
| 2012 | 图数据导入失败 |

---

## 15. 附录

### 15.1 数据库类型枚举

| 类型值 | 说明 |
|--------|------|
| `neo4j` | Neo4j 图数据库 |
| `nebula` | Nebula Graph |
| `janus` | JanusGraph |

### 15.2 通用状态枚举

| 状态值 | 说明 |
|--------|------|
| 0 | 未发布 / 未检测 |
| 1 | 已发布 / 通过 |
| 2 | 失败 |

### 15.3 公共请求头

| 请求头 | 说明 |
|--------|------|
| `Authorization: Bearer {token}` | JWT 认证令牌 |
| `Content-Type: application/json` | 请求内容类型（默认） |

### 15.4 模型对照

| 模型 | 说明 | 所在包 |
|------|------|--------|
| `Result<T>` | 统一响应包装 | `com.chenpp.graph.admin.model` |
| `PageResult<T>` | 分页结果 | `com.chenpp.graph.admin.model` |
| `LoginRequest` | 登录请求 | `com.chenpp.graph.admin.model` |
| `LoginResponse` | 登录响应 | `com.chenpp.graph.admin.model` |
| `User` | 用户 | `com.chenpp.graph.admin.model` |
| `Role` | 角色 | `com.chenpp.graph.admin.model` |
| `Permission` | 权限 | `com.chenpp.graph.admin.model` |
| `Graph` | 图 | `com.chenpp.graph.admin.model` |
| `GraphDatabaseConnection` | 图数据库连接 | `com.chenpp.graph.admin.model` |
| `GraphNodeDef` | 节点定义 | `com.chenpp.graph.admin.model` |
| `GraphEdgeDef` | 边定义 | `com.chenpp.graph.admin.model` |
| `GraphPropertyDef` | 属性定义 | `com.chenpp.graph.admin.model` |
| `GraphSchema` | 图 Schema | `com.chenpp.graph.core.schema` |
| `GraphData` | 图查询数据 | `com.chenpp.graph.core.model` |
| `GraphSummary` | 图统计 | `com.chenpp.graph.core.model` |
| `ImportResult` | 导入结果 | `com.chenpp.graph.admin.model` |
| `AppConfig` | 系统配置 | `com.chenpp.graph.admin.model` |
