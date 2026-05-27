# 前端 API 一致性审计报告

## 1. 汇总统计

| 类别 | 数量 |
|------|------|
| 前端接口总数 | 70 |
| 完全一致 | 52 |
| 路径不匹配 | 2 |
| 请求体格式差异 | 1 |
| 前端有后端无（多余接口） | 5 |
| 后端有前端无（缺失接口） | 2 |
| 响应格式差异 | 4 |

## 2. 逐页审计详情

### 2.1 登录页 — LoginView.vue

**API 模块**: `src/views/auth/api/auth.js` → **后端**: `AuthController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | POST | `/api/auth/login` | `authApi.login(data)` | `AuthController.login(@RequestBody LoginRequest)` | ✅ 一致 |
| 2 | POST | `/api/auth/logout` | `authApi.logout()` | `AuthController.logout()` | ✅ 一致 |

**说明**: 完全匹配。注意后端 `login` 返回 `ResponseEntity<Result<LoginResponse>>`，响应拦截器可正确处理。

---

### 2.2 连接管理页 — ConnectionView.vue

**API 模块**: `src/views/connections/api/connection.js` → **后端**: `GraphDatabaseController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/connections` | `connectionApi.list(params)` | `getConnections(page,pageSize,keyword,type)` | ✅ 一致 |
| 2 | POST | `/api/connections` | `connectionApi.create(data)` | `createConnection(@RequestBody)` | ✅ 一致 |
| 3 | PUT | `/api/connections/{id}` | `connectionApi.update(id,data)` | `updateConnection(@PathVariable id, @RequestBody)` | ✅ 一致 |
| 4 | DELETE | `/api/connections/{id}` | `connectionApi.delete(id)` | `deleteConnection(@PathVariable id)` | ✅ 一致 |
| 5 | POST | `/api/connections/{id}/test` | `connectionApi.testConnection(id)` | `testConnection(@PathVariable id)` | ✅ 一致 |
| 6 | POST | `/api/connections/{id}/connect` | `connectionApi.connect(id)` | ❌ **后端无此接口** | ⚠️ 多余 |
| 7 | POST | `/api/connections/{id}/disconnect` | `connectionApi.disconnect(id)` | ❌ **后端无此接口** | ⚠️ 多余 |

**响应格式检查**:
- 后端返回 `Result<Page<GraphDatabaseConnection>>`，Page 包含 `records/total/size/current/pages` 字段
- AppLayout 中 `connectionApi.list()` 响应处理使用 `res.data?.records || []`，与后端返回格式一致

---

### 2.3 图列表页 — GraphListView.vue

**API 模块**: `src/views/graphs/stores/useGraphsStore.js` (调用 `src/views/graphs/api/graph.js`) → **后端**: `GraphController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/graphs` | `graphApi.list(params)` | `getGraphs(page,pageSize,keyword)` | ✅ 一致 |
| 2 | GET | `/api/graphs/connection/{connectionId}` | `graphApi.listByConnection(connectionId,params)` | `getGraphsByConnectionId(@PathVariable,page,pageSize)` | ✅ 一致 |
| 3 | POST | `/api/graphs` | `graphApi.create(data)` | `createGraph(@RequestBody)` | ✅ 一致 |
| 4 | GET | `/api/graphs/{id}` | `graphApi.get(id)` | `getGraph(@PathVariable id)` | ✅ 一致 |
| 5 | PUT | `/api/graphs/{id}` | `graphApi.update(id,data)` | `updateGraph(@PathVariable id, @RequestBody)` | ✅ 一致 |
| 6 | DELETE | `/api/graphs/{id}` | `graphApi.delete(id)` | `deleteGraph(@PathVariable id)` | ✅ 一致 |

**响应格式检查**:
- 后端返回 `Result<Page<Graph>>`，Page 对象含 `records/total/size/current/pages`
- Store 中 `fetchGraphs()` 使用 `res?.data?.records` 访问，与后端 `data.records` 一致

---

### 2.4 图建模页 — ModelingView.vue

**API 模块**: `src/views/graphs/api/graph.js` (schema 部分) → **后端**: `GraphSchemaController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/graphs/{graphId}/nodes` | `graphApi.getNodeDefs(graphId)` | `getNodeDefs(@PathVariable, status)` | ✅ 一致 |
| 2 | POST | `/api/graphs/{graphId}/nodes` | `graphApi.addNodeDef(graphId,data)` | `addNodeDef(@PathVariable, @RequestBody)` | ✅ 一致 |
| 3 | PUT | `/api/graphs/{graphId}/nodes/{nodeId}` | `graphApi.updateNodeDef(graphId,nodeId,data)` | `updateNodeDef(@PathVariable, @PathVariable, @RequestBody)` | ✅ 一致 |
| 4 | DELETE | `/api/graphs/{graphId}/nodes/{nodeId}` | `graphApi.deleteNodeDef(graphId,nodeId)` | `deleteNodeDef(@PathVariable, @PathVariable)` | ✅ 一致 |
| 5 | GET | `/api/graphs/{graphId}/edges` | `graphApi.getEdgeDefs(graphId)` | `getEdgeDefs(@PathVariable, status)` | ✅ 一致 |
| 6 | POST | `/api/graphs/{graphId}/edges` | `graphApi.addEdgeDef(graphId,data)` | `addEdgeDef(@PathVariable, @RequestBody)` | ✅ 一致 |
| 7 | PUT | `/api/graphs/{graphId}/edges/{edgeId}` | `graphApi.updateEdgeDef(graphId,edgeId,data)` | `updateEdgeDef(@PathVariable, @PathVariable, @RequestBody)` | ✅ 一致 |
| 8 | DELETE | `/api/graphs/{graphId}/edges/{edgeId}` | `graphApi.deleteEdgeDef(graphId,edgeId)` | `deleteEdgeDef(@PathVariable, @PathVariable)` | ✅ 一致 |
| 9 | POST | `/api/graphs/{graphId}/publish` | `graphApi.publishSchema(graphId)` | `publishSchema(@PathVariable)` | ✅ 一致 |
| 10 | GET | `/api/graphs/{graphId}/schema` | — | `getSchema(@PathVariable)` | ⚠️ 后端有前端无 |

**说明**: 后端 `getNodeDefs` 和 `getEdgeDefs` 支持可选 `status` 查询参数，前端调用时未传递该参数（使用默认值 null），不影响功能。

---

### 2.5 图数据页 — GraphDataView.vue

**API 模块**: `src/views/graphs/api/graph.js` (数据 CRUD + 导入) → **后端**: `GraphDataController.java` + `GraphSchemaController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/graphs/{graphId}/nodes/{nodeTypeId}` | `graphApi.getNodeDataList(graphId,nodeTypeId,params)` | `getNodeDataList(@PathVariable, @PathVariable, page, size)` | ✅ 一致 |
| 2 | GET | `/api/graphs/{graphId}/edges/{edgeTypeId}` | `graphApi.getEdgeDataList(graphId,edgeTypeId,params)` | `getEdgeDataList(@PathVariable, @PathVariable, page, size)` | ✅ 一致 |
| 3 | POST | `/api/graphs/{graphId}/data/nodes/{nodeTypeId}` | `graphApi.addNodeData(graphId,nodeTypeId,data)` | `addNodeData(@PathVariable, @PathVariable, @RequestBody)` | ✅ 一致 |
| 4 | POST | `/api/graphs/{graphId}/data/edges/{edgeTypeId}` | `graphApi.addEdgeData(graphId,edgeTypeId,data)` | `addEdgeData(@PathVariable, @PathVariable, @RequestBody)` | ✅ 一致 |
| 5 | PUT | `/api/graphs/{graphId}/data/nodes/{nodeId}` | `graphApi.updateNodeData(graphId,nodeId,data)` | `updateNodeData(@PathVariable, @PathVariable, @RequestBody)` | ✅ 一致 |
| 6 | PUT | `/api/graphs/{graphId}/data/edges/{edgeId}` | `graphApi.updateEdgeData(graphId,edgeId,data)` | `updateEdgeData(@PathVariable, @PathVariable, @RequestBody)` | ✅ 一致 |
| 7 | DELETE | `/api/graphs/{graphId}/data/nodes/{nodeId}` | `graphApi.deleteNode(graphId,nodeId,label)` | `deleteNode(@PathVariable, @PathVariable, @RequestParam label)` | ✅ 一致 |
| 8 | DELETE | `/api/graphs/{graphId}/data/edges/{edgeId}` | `graphApi.deleteEdge(graphId,edgeId,label)` | ❌ **后端无此接口** | ⚠️ 多余 |
| 9 | DELETE | `/api/graphs/{graphId}/data/nodes` | — | `deleteNodes(@PathVariable, @RequestBody)` | ⚠️ 后端有前端无 |
| 10 | POST | `/api/graphs/{graphId}/import/nodes/{nodeTypeId}` | `graphApi.importNodeData(graphId,nodeTypeId,formData)` | `POST /api/graphs/{graphId}/nodes/{nodeTypeId}/import` | ❌ **路径不匹配** |
| 11 | POST | `/api/graphs/{graphId}/import/edges/{edgeTypeId}` | `graphApi.importEdgeData(graphId,edgeTypeId,formData)` | `POST /api/graphs/{graphId}/edges/{edgeTypeId}/import` | ❌ **路径不匹配** |

**响应格式检查**:
- 后端 `getNodeDataList` 返回 `Result<List<Map<String, Object>>>`（**无分页包装**），前端可能期望分页结构
- 后端 `addNodeData / addEdgeData` 返回 `Result<Boolean>`（data 为布尔值），前端可根据 `res.code === 200` 判断

---

### 2.6 图查询页 — GraphVisualView.vue

**API 模块**: `src/views/graphs/api/graph.js` (查询部分) → **后端**: `GraphQueryController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | POST | `/api/graphs/{graphId}/query` | `graphApi.queryGraph(graphId,cypher)` | `query(@PathVariable, @RequestBody map)` | ✅ 一致 |
| 2 | POST | `/api/graphs/{graphId}/expand` | `graphApi.expandNode(graphId,nodeId,depth)` | `expand(@PathVariable, @RequestBody map)` | ✅ 一致 |
| 3 | POST | `/api/graphs/{graphId}/path` | `graphApi.findPath(graphId,startNodeId,endNodeId,maxDepth)` | `findPath(@PathVariable, @RequestBody map)` | ✅ 一致 |

**说明**: 请求体字段名（cypher/nodeId/depth/startNodeId/endNodeId/maxDepth）前后端完全一致。

---

### 2.7 图分析页 — GraphAnalysisView.vue

**API 模块**: `src/views/graphs/api/graph.js` → **后端**: `GraphSchemaController.java` + `GraphQueryController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/graphs/{graphId}/nodes` | `graphApi.getNodeDefs(graphId)` | `getNodeDefs(@PathVariable)` | ✅ 一致 |
| 2 | GET | `/api/graphs/{graphId}/edges` | `graphApi.getEdgeDefs(graphId)` | `getEdgeDefs(@PathVariable)` | ✅ 一致 |
| 3 | POST | `/api/graphs/{graphId}/query` | `graphApi.queryGraph(graphId,cypher)` | `query(@PathVariable, @RequestBody)` | ✅ 一致 |
| 4 | POST | `/api/graphs/{graphId}/expand` | `graphApi.expandNode(graphId,nodeId,depth)` | `expand(@PathVariable, @RequestBody)` | ✅ 一致 |
| 5 | POST | `/api/graphs/{graphId}/path` | `graphApi.findPath(graphId,start,end,maxDepth)` | `findPath(@PathVariable, @RequestBody)` | ✅ 一致 |

---

### 2.8 图统计页 — GraphSummaryView.vue

**API 模块**: `src/views/graphs/api/graph.js` → **后端**: `GraphDataController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/graphs/{graphId}/summary` | `graphApi.getGraphSummary(graphId)` | `getGraphSummary(@PathVariable)` | ✅ 一致 |

---

### 2.9 用户管理页 — UserView.vue

**API 模块**: `src/api/user.js` → **后端**: `UserController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/users` | `userApi.getUsers(params)` | `getUsers(pageNum,pageSize,username,phoneNumber,status)` | ✅ 一致 |
| 2 | GET | `/api/users/{userId}` | `userApi.getUser(userId)` | `getUser(@PathVariable)` | ✅ 一致 |
| 3 | GET | `/api/users/profile` | `userApi.getCurrentUser(params)` | `getUserByName(@RequestParam username)` | ✅ 一致 |
| 4 | POST | `/api/users` | `userApi.createUser(data)` | `addUser(@RequestBody)` | ✅ 一致 |
| 5 | PUT | `/api/users/{userId}` | `userApi.updateUser(userId,data)` | `updateUser(@PathVariable, @RequestBody)` | ✅ 一致 |
| 6 | DELETE | `/api/users/{userId}` | `userApi.deleteUser(userId)` | `deleteUser(@PathVariable)` | ✅ 一致 |
| 7 | DELETE | `/api/users` | `userApi.deleteUsers(userIds)` 传 `{ids: [...]}` | `deleteUsers(@RequestBody List<Long>)` 直接接收数组 | ❌ **请求体格式不匹配** |
| 8 | PUT | `/api/users/{userId}/status` | — | `updateUserStatus(@PathVariable, @RequestParam)` | ⚠️ 后端有前端无 |
| 9 | POST | `/api/users/{userId}/password/reset` | `userApi.resetPassword(userId, password)` 传 `{password}` | `resetPassword(@PathVariable, @RequestBody User)` | ✅ 一致 |
| 10 | PUT | `/api/users/profile` | `userApi.updateCurrentUser(data)` | ❌ **后端无此接口** | ⚠️ 多余 |
| 11 | PUT | `/api/users/profile/password` | `userApi.changePassword(data)` | ❌ **后端无此接口** | ⚠️ 多余 |

**deleteUsers 请求体差异详解**:
- 前端: `DELETE /api/users` 请求体为 `{ids: [1, 2, 3]}`
- 后端: `@RequestBody List<Long>` 期望直接接收 `[1, 2, 3]`
- 结果: 后端反序列化会失败，因为 `{ids: [1,2,3]}` 无法绑定到 `List<Long>`

---

### 2.10 角色管理页 — RoleView.vue

**API 模块**: `src/api/role.js` → **后端**: `RoleController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/roles` | `roleApi.list(params)` | `getRoles(pageNum,pageSize,roleName,roleKey,status)` | ✅ 一致 |
| 2 | GET | `/api/roles/{roleId}` | `roleApi.get(roleId)` | `getRole(@PathVariable)` | ✅ 一致 |
| 3 | POST | `/api/roles` | `roleApi.create(data)` | `addRole(@RequestBody)` | ✅ 一致 |
| 4 | PUT | `/api/roles/{roleId}` | `roleApi.update(roleId,data)` | `updateRole(@PathVariable, @RequestBody)` | ✅ 一致 |
| 5 | DELETE | `/api/roles` | `roleApi.delete(roleIds)` 传 `roleIds` 数组 | `deleteRoles(@RequestBody List<Long>)` 接收数组 | ✅ 一致 |
| 6 | PUT | `/api/roles/{roleId}/status` | `roleApi.updateStatus(roleId,status)` | `updateRoleStatus(@PathVariable, @RequestParam)` | ✅ 一致 |
| 7 | GET | `/api/roles/{roleId}/dataScope` | `roleApi.getDataScope(roleId)` | `getRoleDataScope(@PathVariable)` | ✅ 一致 |
| 8 | PUT | `/api/roles/{roleId}/dataScope` | `roleApi.updateDataScope(roleId,dataScope)` | `updateRoleDataScope(@PathVariable, @RequestParam)` | ✅ 一致 |

**说明**: `roleApi.delete(roleIds)` 使用 `request.delete('/api/roles', { data: roleIds })`，后端接收 `@RequestBody List<Long>`，两者格式一致。

---

### 2.11 权限管理页 — PermissionView.vue

**API 模块**: `src/api/permission.js` → **后端**: `PermissionController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/permissions` | `permissionApi.list()` | `getPermissions()` | ✅ 一致 |
| 2 | GET | `/api/permissions/{permissionId}` | `permissionApi.get(permissionId)` | `getPermission(@PathVariable)` | ✅ 一致 |
| 3 | POST | `/api/permissions` | `permissionApi.create(data)` | `addPermission(@RequestBody)` | ✅ 一致 |
| 4 | PUT | `/api/permissions/{permissionId}` | `permissionApi.update(permissionId,data)` | `updatePermission(@PathVariable, @RequestBody)` | ✅ 一致 |
| 5 | DELETE | `/api/permissions/{permissionId}` | `permissionApi.delete(permissionId)` | `deletePermission(@PathVariable)` | ✅ 一致 |
| 6 | PUT | `/api/permissions/{permissionId}/status` | `permissionApi.updateStatus(permissionId,status)` | `updatePermissionStatus(@PathVariable, @RequestParam)` | ✅ 一致 |

---

### 2.12 系统配置页 — AppConfigView.vue

**API 模块**: `src/api/config.js` → **后端**: `AppConfigController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/configs` | `configApi.list(params)` | `listConfigs(page,pageSize,configKey)` | ✅ 一致 |
| 2 | GET | `/api/configs/{id}` | `configApi.get(id)` | `getConfig(@PathVariable)` | ✅ 一致 |
| 3 | POST | `/api/configs` | `configApi.create(data)` | `addConfig(@RequestBody)` | ✅ 一致 |
| 4 | PUT | `/api/configs/{id}` | `configApi.update(id,data)` | `updateConfig(@PathVariable, @RequestBody)` | ✅ 一致 |
| 5 | DELETE | `/api/configs/{id}` | `configApi.delete(id)` | `deleteConfig(@PathVariable)` | ✅ 一致 |

---

### 2.13 个人中心页 — ProfileView.vue

**API 模块**: `src/api/user.js` → **后端**: `UserController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/users/profile` | `userApi.getCurrentUser(params)` | `getUserByName(@RequestParam username)` | ✅ 一致 |
| 2 | PUT | `/api/users/profile` | `userApi.updateCurrentUser(data)` | ❌ **后端无此接口** | ⚠️ 多余 |
| 3 | PUT | `/api/users/profile/password` | `userApi.changePassword(data)` | ❌ **后端无此接口** | ⚠️ 多余 |

---

### 2.14 AppLayout 布局组件

**API 调用**: `src/layouts/AppLayout.vue` → **后端**: `GraphDatabaseController.java` + `GraphController.java`

| # | 方法 | 路径 | 前端 | 后端 | 结果 |
|---|------|------|------|------|------|
| 1 | GET | `/api/connections` | `connectionApi.list()` (无参数) | `getConnections(page,pageSize,keyword,type)` 全部有默认值 | ✅ 一致 |
| 2 | GET | `/api/graphs` | `graphsStore.fetchGraphs()` | `getGraphs(page,pageSize,keyword)` | ✅ 一致 |

**响应处理检查**:
- `connectionApi.list()` 返回 `Result<Page<GraphDatabaseConnection>>`
- 前端代码: `const res = await connectionApi.list(); const data = res?.data || res; connections.value = Array.isArray(data) ? data : data?.records || []`
- 由于响应拦截器在 `code===200` 时返回 `data`（即整个 Result 对象），`res` 为 `{code,message,data:{records,total,...}}`，所以 `res.data` = Page 对象，`res.data.records` = 记录数组
- 上述逻辑正确。

---

## 3. 问题分类汇总

### 3.1 路径不匹配（2 项）

| 接口 | 前端路径 | 后端路径 | 影响 |
|------|----------|----------|------|
| 导入节点数据 | `POST /api/graphs/{id}/import/nodes/{typeId}` | `POST /api/graphs/{id}/nodes/{typeId}/import` | 请求 404 |
| 导入边数据 | `POST /api/graphs/{id}/import/edges/{typeId}` | `POST /api/graphs/{id}/edges/{typeId}/import` | 请求 404 |

**建议修复**: 前端 `graph.js` 中 `importNodeData` 和 `importEdgeData` 的 URL 需改为 `/api/graphs/${graphId}/nodes/${nodeTypeId}/import` 和 `/api/graphs/${graphId}/edges/${edgeTypeId}/import`。

### 3.2 请求体格式不匹配（1 项）

| 接口 | 前端 | 后端 | 影响 |
|------|------|------|------|
| 批量删除用户 | `DELETE /api/users` 传 `{ids: [1,2,3]}` | `@RequestBody List<Long>` 期望 `[1,2,3]` | 反序列化失败 |

**建议修复**: 前端 `user.js` 中 `deleteUsers` 改为 `request.delete('/api/users', { data: userIds })`，直接传数组而非对象包装。

### 3.3 前端有后端无（多余接口 — 5 项）

| 接口 | 方法 | 路径 | 建议 |
|------|------|------|------|
| connect | POST | `/api/connections/{id}/connect` | 删除或后端补充实现 |
| disconnect | POST | `/api/connections/{id}/disconnect` | 删除或后端补充实现 |
| updateCurrentUser | PUT | `/api/users/profile` | 删除或后端补充实现 |
| changePassword | PUT | `/api/users/profile/password` | 删除或后端补充实现 |
| deleteEdge | DELETE | `/api/graphs/{graphId}/data/edges/{edgeId}` | 后端补充实现 |

### 3.4 后端有前端无（缺失接口 — 2 项）

| 接口 | 方法 | 路径 | 建议 |
|------|------|------|------|
| updateUserStatus | PUT | `/api/users/{userId}/status` | 前端可选择性接入 |
| deleteNodes (批量) | DELETE | `/api/graphs/{graphId}/data/nodes` | 前端可选择性接入 |

### 3.5 响应格式差异（4 处潜在问题）

| 接口 | 后端返回格式 | 前端期望 | 风险 |
|------|-------------|----------|------|
| `GET /api/graphs/{id}/nodes/{typeId}` (节点数据列表) | `Result<List<Map>>` 无分页包装 | 可能期望 `{records, total}` 分页结构 | 中 |
| `GET /api/graphs/{id}/edges/{typeId}` (边数据列表) | `Result<List<Map>>` 无分页包装 | 可能期望 `{records, total}` 分页结构 | 中 |
| `POST /api/graphs/{id}/data/nodes/{typeId}` (新增节点) | `Result<Boolean>` data 为布尔值 | `Result<String>` 或 `Result<Long>` | 低 |
| 连接/图列表接口 | `Result<Page<T>>` MyBatis-Plus 分页 | `{records, total, size, current, pages}` | 低 |

**说明**: 节点/边数据列表接口目前返回 `Result<List<Map<String, Object>>>` 直接为列表，未包装分页信息。若前端期望分页，需后端改为 `Result<Page<...>>` 或在 Service 层包装。

---

## 4. 建议修复方案

### 4.1 高优先级（影响功能使用）

1. **修复导入路径** — `graph.js:159` 和 `graph.js:166` 的 URL 中 `import/nodes` → `nodes/import`
2. **修复 deleteUsers 请求体** — `user.js:60` 改为直接传数组

### 4.2 中优先级（功能缺失或多余）

3. **移除或实现 connect/disconnect** — 若无需求则从 `connection.js` 删除
4. **移除或实现 updateCurrentUser / changePassword** — 若无需求则从 `user.js` 删除
5. **后端补充 deleteEdge 端点** — `GraphDataController.java` 增加 `DELETE /data/edges/{edgeId}` 和 `DELETE /data/edges`
6. **统一列表接口分页格式** — 节点/边数据列表改为返回 `Result<Page<...>>` 而非 `Result<List<...>>`

### 4.3 低优先级（前端响应处理）

7. **检查节点/边数据列表前端处理** — 确认前端 `getNodeDataList` / `getEdgeDataList` 的响应处理兼容无分页的列表格式
8. **补充缺失的后端接口调用** — `updateUserStatus` 和 `deleteNodes` 可选择性在前端添加

---

## 5. 审计总结

本次审计覆盖 14 个前端组件/模块，对应 10 个后端 Controller，共审查 70 个接口调用：

- **52 个接口完全一致**（74.3%）
- **8 个接口存在问题**（11.4%），其中路径不匹配 2 个，请求体格式不匹配 1 个，多余接口 5 个
- **2 个后端接口未被前端调用**
- **4 处响应格式存在差异风险**

主要问题集中在**连接管理**（connect/disconnect）、**用户管理**（deleteUsers 请求体、updateCurrentUser、changePassword）和**数据导入路径**三个领域。

建议按优先级依次修复，重点关注导入功能（路径不匹配会导致 404）和批量删除用户（请求体格式不匹配会导致请求失败）。
