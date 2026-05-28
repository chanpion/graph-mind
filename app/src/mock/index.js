/**
 * Mock 数据入口
 * 统一导出所有 Mock 数据，并提供请求分发处理器
 */

// ====== Mock 工具函数 ======
export function mockDelay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export function mockSuccess(data) {
  return { code: 200, data, message: '操作成功' }
}

export function mockError(message = '请求失败', code = 500) {
  return { code, data: null, message }
}

// ====== 导入各模块 Mock 数据 ======
import {
  mockListConnections, mockCreateConnection, mockUpdateConnection,
  mockDeleteConnection, mockTestConnection
} from './connection'
import {
  mockListGraphs, mockGetGraphSchema, mockCreateGraph, mockDeleteGraph,
  mockGetVertexTypes, mockCreateVertexType, mockDeleteVertexType,
  mockUpdateVertexType,
  mockGetEdgeTypes, mockCreateEdgeType, mockDeleteEdgeType,
  mockUpdateEdgeType, mockSchemas
} from './graph'
import {
  mockQueryVertices, mockGetVertex, mockUpdateVertex, mockDeleteVertex,
  mockCreateVertex, mockQueryEdges, mockCreateEdge, mockGetEdge,
  mockUpdateEdge, mockDeleteEdge, mockGetDataStats, mockExecuteQuery,
  mockImportCsv, mockNativeQuery
} from './data'

// ====== Helper ======
function matchPath(pattern, url) {
  const regex = new RegExp('^' + pattern.replace(/:\w+/g, '([^/]+)') + '$')
  const match = url.match(regex)
  if (!match) return null
  return match.slice(1) // captured params
}

function parseIntOr(val, fallback) {
  const n = parseInt(val, 10)
  return isNaN(n) ? fallback : n
}

// ====== Mock route table ======
const routes = []

function register(method, pattern, handler) {
  routes.push({ method: method.toLowerCase(), pattern, handler })
}

// URL 解析辅助：从 /api/graphs/:graphId/nodes/:nodeId 中提取参数
function extractParams(pattern, url) {
  const regex = new RegExp('^' + pattern.replace(/:\w+/g, '([^/]+)') + '$')
  const match = url.match(regex)
  if (!match) return null
  const keys = pattern.match(/:(\w+)/g) || []
  const params = {}
  keys.forEach((key, i) => {
    params[key.replace(':', '')] = match[i + 1]
  })
  return params
}

// ====== 认证 ======
register('post', '/api/auth/login', async (config) => {
  await mockDelay(500)
  const { username, password } = config.data || {}
  if (!username || !password) {
    return mockError('用户名和密码不能为空', 400)
  }
  return mockSuccess({
    token: 'mock_jwt_token_' + Date.now(),
    username,
    nickname: username === 'admin' ? '管理员' : '用户',
    avatar: '',
    roles: username === 'admin' ? ['admin', 'user'] : ['user'],
    userId: username === 'admin' ? 1 : 2
  })
})

register('post', '/api/auth/logout', async () => {
  await mockDelay(200)
  return mockSuccess(null)
})

// ====== 连接管理 ======
register('get', '/api/connections', async (config) => {
  return mockListConnections(config)
})

register('post', '/api/connections', async (config) => {
  return mockCreateConnection(config.data)
})

register('put', '/api/connections/:id', async (config, params) => {
  return mockUpdateConnection(params.id, config.data)
})

register('delete', '/api/connections/:id', async (config, params) => {
  return mockDeleteConnection(params.id)
})

register('post', '/api/connections/:id/test', async (config, params) => {
  return mockTestConnection(params.id)
})

register('post', '/api/connections/:id/connect', async (config, params) => {
  await mockDelay(300)
  return mockSuccess({ connected: true })
})

register('post', '/api/connections/:id/disconnect', async (config, params) => {
  await mockDelay(200)
  return mockSuccess({ connected: false })
})

// ====== 图管理 ======
register('get', '/api/graphs', async () => {
  return mockListGraphs()
})

register('get', '/api/graphs/connection/:connectionId', async (config, params) => {
  return mockListGraphs(params.connectionId)
})

register('get', '/api/graphs/:id', async (config, params) => {
  await mockDelay(300)
  const graphs = await mockListGraphs()
  const graph = graphs.data.find(g => g.id === parseIntOr(params.id, 0) || g.name === params.id)
  return graph ? mockSuccess(graph) : mockError('图不存在', 404)
})

register('post', '/api/graphs', async (config) => {
  return mockCreateGraph(config.data)
})

register('put', '/api/graphs/:id', async (config, params) => {
  await mockDelay(300)
  const id = parseIntOr(params.id, 0)
  const graph = mockGraphs.find(g => g.id === id)
  if (graph) {
    Object.assign(graph, config.data, { updateTime: new Date().toISOString() })
  }
  return mockSuccess({ id: params.id, ...config.data, updateTime: new Date().toISOString() })
})

register('delete', '/api/graphs/:id', async (config, params) => {
  return mockDeleteGraph(params.id)
})

// ====== Schema ======
register('get', '/api/graphs/:graphId/nodes', async (config, params) => {
  const res = await mockGetVertexTypes(null, params.graphId)
  return res
})

register('post', '/api/graphs/:graphId/nodes', async (config, params) => {
  return mockCreateVertexType(null, params.graphId, config.data)
})

register('put', '/api/graphs/:graphId/nodes/:nodeId', async (config, params) => {
  await mockDelay(300)
  return mockSuccess({ id: params.nodeId, ...config.data })
})

register('delete', '/api/graphs/:graphId/nodes/:nodeId', async (config, params) => {
  return mockDeleteVertexType(null, params.graphId, params.nodeId)
})

register('get', '/api/graphs/:graphId/edges', async (config, params) => {
  const res = await mockGetEdgeTypes(null, params.graphId)
  return res
})

register('post', '/api/graphs/:graphId/edges', async (config, params) => {
  return mockCreateEdgeType(null, params.graphId, config.data)
})

register('put', '/api/graphs/:graphId/edges/:edgeId', async (config, params) => {
  await mockDelay(300)
  return mockSuccess({ id: params.edgeId, ...config.data })
})

register('delete', '/api/graphs/:graphId/edges/:edgeId', async (config, params) => {
  return mockDeleteEdgeType(null, params.graphId, params.edgeId)
})

register('post', '/api/graphs/:graphId/publish', async () => {
  await mockDelay(500)
  return mockSuccess({ published: true, publishedAt: new Date().toISOString() })
})

// ====== 图数据 ======
register('get', '/api/graphs/:graphId/nodes/:nodeTypeId', async (config, params) => {
  // 从 schema mock 中解析节点类型 label
  const schema = mockSchemas[params.graphId]
  const vertexLabel = schema?.vertexLabels?.find(v => v.id == params.nodeTypeId)?.label || params.nodeTypeId
  return mockQueryVertices(null, params.graphId, { ...config.params, label: vertexLabel })
})

register('get', '/api/graphs/:graphId/edges/:edgeTypeId', async (config, params) => {
  // 从 schema mock 中解析边类型 label
  const schema = mockSchemas[params.graphId]
  const edgeLabel = schema?.edgeLabels?.find(e => e.id == params.edgeTypeId)?.label || params.edgeTypeId
  return mockQueryEdges(null, params.graphId, { ...config.params, label: edgeLabel })
})

register('post', '/api/graphs/:graphId/data/nodes/:nodeTypeId', async (config, params) => {
  // 从 schema mock 中解析节点类型 label
  const schema = mockSchemas[params.graphId]
  const vertexLabel = schema?.vertexLabels?.find(v => v.id == params.nodeTypeId)?.label || config.data.label || params.nodeTypeId
  return mockCreateVertex(null, params.graphId, { ...config.data, label: vertexLabel })
})

register('post', '/api/graphs/:graphId/data/edges/:edgeTypeId', async (config, params) => {
  // 将 UI 的 startUid/endUid 转为 mock 内部字段 source/target
  const data = {
    ...config.data,
    source: config.data.startUid,
    target: config.data.endUid
  }
  const schema = mockSchemas[params.graphId]
  const edgeLabel = schema?.edgeLabels?.find(e => e.id == params.edgeTypeId)?.label || config.data.label || params.edgeTypeId
  return mockCreateEdge(null, params.graphId, { ...data, label: edgeLabel })
})

register('put', '/api/graphs/:graphId/data/nodes/:nodeId', async (config, params) => {
  return mockUpdateVertex(null, params.graphId, params.nodeId, config.data)
})

register('put', '/api/graphs/:graphId/data/edges/:edgeId', async (config, params) => {
  return mockUpdateEdge(null, params.graphId, params.edgeId, config.data)
})

register('delete', '/api/graphs/:graphId/data/nodes/:nodeId', async (config, params) => {
  return mockDeleteVertex(null, params.graphId, params.nodeId)
})

register('delete', '/api/graphs/:graphId/data/edges/:edgeId', async (config, params) => {
  return mockDeleteEdge(null, params.graphId, params.edgeId)
})

// ====== 查询分析 ======
register('post', '/api/graphs/:graphId/query', async (config, params) => {
  return mockExecuteQuery(null, params.graphId, config.data?.cypher || '')
})

register('post', '/api/graphs/:graphId/expand', async () => {
  await mockDelay(400)
  return mockSuccess({
    vertices: [
      { uid: 'expanded_1', label: 'Person', properties: { name: '展开节点1', relation: 'friend' } },
      { uid: 'expanded_2', label: 'Person', properties: { name: '展开节点2', relation: 'colleague' } }
    ],
    edges: [
      { id: 'exp_e_1', source: 'root', target: 'expanded_1', label: 'RELATES_TO' }
    ]
  })
})

register('post', '/api/graphs/:graphId/path', async () => {
  await mockDelay(500)
  return mockSuccess({
    paths: [
      [
        { uid: 'start', label: 'Person', properties: { name: '起点' } },
        { uid: 'mid', label: 'Company', properties: { name: '中间节点' } },
        { uid: 'end', label: 'Person', properties: { name: '终点' } }
      ]
    ],
    pathCount: 1
  })
})

register('get', '/api/graphs/:graphId/summary', async () => {
  return mockGetDataStats()
})

// ====== 数据导入 ======
register('post', '/api/graphs/:graphId/import/nodes/:nodeTypeId', async (config, params) => {
  return mockImportCsv(null, params.graphId, config.data, null)
})

register('post', '/api/graphs/:graphId/import/edges/:edgeTypeId', async (config, params) => {
  return mockImportCsv(null, params.graphId, config.data, null)
})

// ====== 用户管理 ======
const mockUsers = [
  { userId: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: '13800000001', status: '0', createTime: '2024-01-01 00:00:00', roleNames: ['管理员'], roles: ['admin'] },
  { userId: 2, username: 'user1', nickname: '用户一', email: 'user1@example.com', phone: '13800000002', status: '0', createTime: '2024-01-15 10:30:00', roleNames: ['普通用户'], roles: ['user'] },
  { userId: 3, username: 'guest', nickname: '访客', email: 'guest@example.com', phone: '13800000003', status: '1', createTime: '2024-02-01 08:00:00', roleNames: ['访客'], roles: ['guest'] }
]

register('get', '/api/users', async (config) => {
  await mockDelay(300)
  const { pageNum = 1, pageSize = 10 } = config.params || {}
  let list = [...mockUsers]
  if (config.params?.username) {
    list = list.filter(u => u.username.includes(config.params.username))
  }
  if (config.params?.status !== undefined && config.params?.status !== '') {
    list = list.filter(u => u.status === config.params.status)
  }
  const start = (pageNum - 1) * pageSize
  const data = {
    records: list.slice(start, start + pageSize),
    total: list.length,
    pageNum: parseInt(pageNum, 10),
    pageSize: parseInt(pageSize, 10)
  }
  return mockSuccess(data)
})

register('get', '/api/users/profile', async () => {
  await mockDelay(200)
  return mockSuccess({ ...mockUsers[0] })
})

register('put', '/api/users/profile', async (config) => {
  await mockDelay(300)
  Object.assign(mockUsers[0], config.data)
  return mockSuccess(mockUsers[0])
})

register('put', '/api/users/profile/password', async () => {
  await mockDelay(300)
  return mockSuccess({ message: '密码修改成功' })
})

register('get', '/api/users/:userId', async (config, params) => {
  await mockDelay(200)
  const user = mockUsers.find(u => u.userId === parseIntOr(params.userId, 0))
  return user ? mockSuccess(user) : mockError('用户不存在', 404)
})

register('post', '/api/users', async (config) => {
  await mockDelay(300)
  const newUser = {
    userId: mockUsers.length + 1,
    ...config.data,
    createTime: new Date().toISOString().replace('T', ' ').slice(0, 19)
  }
  mockUsers.push(newUser)
  return mockSuccess(newUser)
})

register('put', '/api/users/:userId', async (config, params) => {
  await mockDelay(300)
  const user = mockUsers.find(u => u.userId === parseIntOr(params.userId, 0))
  if (!user) return mockError('用户不存在', 404)
  Object.assign(user, config.data)
  return mockSuccess(user)
})

register('delete', '/api/users/:userId', async (config, params) => {
  await mockDelay(300)
  const idx = mockUsers.findIndex(u => u.userId === parseIntOr(params.userId, 0))
  if (idx === -1) return mockError('用户不存在', 404)
  mockUsers.splice(idx, 1)
  return mockSuccess(null)
})

register('delete', '/api/users', async (config) => {
  await mockDelay(300)
  const ids = config.data?.ids || []
  for (let i = mockUsers.length - 1; i >= 0; i--) {
    if (ids.includes(mockUsers[i].userId)) mockUsers.splice(i, 1)
  }
  return mockSuccess(null)
})

register('post', '/api/users/:userId/password/reset', async () => {
  await mockDelay(300)
  return mockSuccess({ message: '密码重置成功' })
})

// ====== 角色管理 ======
const mockRoles = [
  { roleId: 1, roleName: '管理员', roleKey: 'admin', roleSort: 1, status: '0', createTime: '2024-01-01 00:00:00', remark: '超级管理员', dataScope: '1', menuCheckStrictly: true },
  { roleId: 2, roleName: '普通用户', roleKey: 'user', roleSort: 2, status: '0', createTime: '2024-01-15 10:00:00', remark: '普通用户', dataScope: '5', menuCheckStrictly: true },
  { roleId: 3, roleName: '访客', roleKey: 'guest', roleSort: 3, status: '1', createTime: '2024-02-01 08:00:00', remark: '只读用户', dataScope: '5', menuCheckStrictly: true }
]

register('get', '/api/roles', async (config) => {
  await mockDelay(300)
  const { pageNum = 1, pageSize = 10 } = config.params || {}
  let list = [...mockRoles]
  if (config.params?.roleName) {
    list = list.filter(r => r.roleName.includes(config.params.roleName))
  }
  if (config.params?.status !== undefined && config.params?.status !== '') {
    list = list.filter(r => r.status === config.params.status)
  }
  const start = (pageNum - 1) * pageSize
  const data = {
    records: list.slice(start, start + pageSize),
    total: list.length,
    pageNum: parseInt(pageNum, 10),
    pageSize: parseInt(pageSize, 10)
  }
  return mockSuccess(data)
})

register('get', '/api/roles/:roleId', async (config, params) => {
  await mockDelay(200)
  const role = mockRoles.find(r => r.roleId === parseIntOr(params.roleId, 0))
  return role ? mockSuccess(role) : mockError('角色不存在', 404)
})

register('post', '/api/roles', async (config) => {
  await mockDelay(300)
  const newRole = {
    roleId: mockRoles.length + 1,
    ...config.data,
    createTime: new Date().toISOString().replace('T', ' ').slice(0, 19)
  }
  mockRoles.push(newRole)
  return mockSuccess(newRole)
})

register('put', '/api/roles/:roleId', async (config, params) => {
  await mockDelay(300)
  const role = mockRoles.find(r => r.roleId === parseIntOr(params.roleId, 0))
  if (!role) return mockError('角色不存在', 404)
  Object.assign(role, config.data)
  return mockSuccess(role)
})

register('delete', '/api/roles', async (config) => {
  await mockDelay(300)
  const ids = config.data || []
  for (let i = mockRoles.length - 1; i >= 0; i--) {
    if (ids.includes(mockRoles[i].roleId)) mockRoles.splice(i, 1)
  }
  return mockSuccess(null)
})

register('put', '/api/roles/:roleId/status', async (config, params) => {
  await mockDelay(200)
  const role = mockRoles.find(r => r.roleId === parseIntOr(params.roleId, 0))
  if (!role) return mockError('角色不存在', 404)
  role.status = config.params?.status || '0'
  return mockSuccess(role)
})

register('get', '/api/roles/:roleId/dataScope', async (config, params) => {
  await mockDelay(200)
  const role = mockRoles.find(r => r.roleId === parseIntOr(params.roleId, 0))
  return role ? mockSuccess({ dataScope: role.dataScope }) : mockError('角色不存在', 404)
})

register('put', '/api/roles/:roleId/dataScope', async (config, params) => {
  await mockDelay(200)
  const role = mockRoles.find(r => r.roleId === parseIntOr(params.roleId, 0))
  if (!role) return mockError('角色不存在', 404)
  role.dataScope = config.params?.dataScope || '5'
  return mockSuccess(role)
})

// ====== 权限菜单管理 ======
const mockPermissions = [
  { menuId: 1, menuName: '用户管理', parentId: 0, orderNum: 1, path: '/home/user', component: 'system/user', menuType: 'C', perms: 'system:user:list', icon: 'User', status: '0', createTime: '2024-01-01', visible: '0', isCache: '0', isFrame: '1', children: [
    { menuId: 2, menuName: '用户新增', parentId: 1, orderNum: 1, path: '', component: '', menuType: 'F', perms: 'system:user:add', icon: '', status: '0', createTime: '2024-01-01', visible: '0', isCache: '0', isFrame: '1' },
    { menuId: 3, menuName: '用户修改', parentId: 1, orderNum: 2, path: '', component: '', menuType: 'F', perms: 'system:user:edit', icon: '', status: '0', createTime: '2024-01-01', visible: '0', isCache: '0', isFrame: '1' },
    { menuId: 4, menuName: '用户删除', parentId: 1, orderNum: 3, path: '', component: '', menuType: 'F', perms: 'system:user:delete', icon: '', status: '0', createTime: '2024-01-01', visible: '0', isCache: '0', isFrame: '1' }
  ]},
  { menuId: 5, menuName: '系统设置', parentId: 0, orderNum: 2, path: '/home/admin', component: '', menuType: 'M', perms: '', icon: 'Setting', status: '0', createTime: '2024-01-01', visible: '0', isCache: '0', isFrame: '1', children: [
    { menuId: 6, menuName: '系统配置', parentId: 5, orderNum: 1, path: '/home/admin/config', component: 'system/config', menuType: 'C', perms: 'system:config:list', icon: 'Tools', status: '0', createTime: '2024-01-01', visible: '0', isCache: '0', isFrame: '1' }
  ]}
]

register('get', '/api/permissions', async () => {
  await mockDelay(300)
  return mockSuccess(mockPermissions)
})

register('get', '/api/permissions/:permissionId', async (config, params) => {
  await mockDelay(200)
  function findPermission(list, id) {
    for (const p of list) {
      if (p.menuId === parseIntOr(id, 0)) return p
      if (p.children) {
        const found = findPermission(p.children, id)
        if (found) return found
      }
    }
    return null
  }
  const perm = findPermission(mockPermissions, params.permissionId)
  return perm ? mockSuccess(perm) : mockError('权限不存在', 404)
})

register('post', '/api/permissions', async (config) => {
  await mockDelay(300)
  return mockSuccess({ menuId: Date.now(), ...config.data })
})

register('put', '/api/permissions/:permissionId', async (config, params) => {
  await mockDelay(300)
  return mockSuccess({ menuId: parseIntOr(params.permissionId, 0), ...config.data })
})

register('delete', '/api/permissions/:permissionId', async () => {
  await mockDelay(300)
  return mockSuccess(null)
})

register('put', '/api/permissions/:permissionId/status', async (config, params) => {
  await mockDelay(200)
  return mockSuccess({ menuId: parseIntOr(params.permissionId, 0), status: config.params?.status || '0' })
})

// ====== 系统配置 ======
const mockConfigs = [
  { configId: 1, configName: '系统名称', configKey: 'sys.name', configValue: 'Graph Mind', configType: 'Y', remark: '系统名称', createTime: '2024-01-01' },
  { configId: 2, configName: '系统版本', configKey: 'sys.version', configValue: '1.0.0', configType: 'Y', remark: '系统版本号', createTime: '2024-01-01' },
  { configId: 3, configName: '是否开启注册', configKey: 'sys.registerEnabled', configValue: 'true', configType: 'Y', remark: '是否允许新用户注册', createTime: '2024-01-01' },
  { configId: 4, configName: 'Token过期时间', configKey: 'sys.tokenExpire', configValue: '86400', configType: 'Y', remark: 'Token过期时间(秒)', createTime: '2024-01-01' }
]

register('get', '/api/configs', async (config) => {
  await mockDelay(300)
  const { pageNum = 1, pageSize = 10 } = config.params || {}
  const start = (pageNum - 1) * pageSize
  const data = {
    records: mockConfigs.slice(start, start + pageSize),
    total: mockConfigs.length,
    pageNum: parseInt(pageNum, 10),
    pageSize: parseInt(pageSize, 10)
  }
  return mockSuccess(data)
})

register('get', '/api/configs/:id', async (config, params) => {
  await mockDelay(200)
  const cfg = mockConfigs.find(c => c.configId === parseIntOr(params.id, 0))
  return cfg ? mockSuccess(cfg) : mockError('配置不存在', 404)
})

register('post', '/api/configs', async (config) => {
  await mockDelay(300)
  const newConfig = { configId: mockConfigs.length + 1, ...config.data, createTime: new Date().toISOString() }
  mockConfigs.push(newConfig)
  return mockSuccess(newConfig)
})

register('put', '/api/configs/:id', async (config, params) => {
  await mockDelay(300)
  const cfg = mockConfigs.find(c => c.configId === parseIntOr(params.id, 0))
  if (!cfg) return mockError('配置不存在', 404)
  Object.assign(cfg, config.data)
  return mockSuccess(cfg)
})

register('delete', '/api/configs/:id', async (config, params) => {
  await mockDelay(300)
  const idx = mockConfigs.findIndex(c => c.configId === parseIntOr(params.id, 0))
  if (idx === -1) return mockError('配置不存在', 404)
  mockConfigs.splice(idx, 1)
  return mockSuccess(null)
})

// ====== 导出任务 ======
let mockExportTasks = [
  { id: 1, name: '导出_社交网络', exportType: 'vertices', exportFormat: 'csv', status: 'completed', progress: 100, createTime: new Date(Date.now() - 86400000).toISOString(), finishTime: new Date(Date.now() - 86000000).toISOString(), fileSize: 1024 * 50, downloadUrl: '/api/export/tasks/1/download' },
  { id: 2, name: '导出_产品图', exportType: 'edges', exportFormat: 'json', status: 'running', progress: 45, createTime: new Date().toISOString() },
  { id: 3, name: '导出_全量数据', exportType: 'graph', exportFormat: 'graphml', status: 'pending', progress: 0, createTime: new Date().toISOString() }
]

register('get', '/api/export/tasks', async () => {
  await mockDelay(300)
  return mockSuccess(mockExportTasks)
})

register('post', '/api/export/tasks', async (config) => {
  await mockDelay(300)
  const newTask = {
    id: mockExportTasks.length + 1,
    ...config.data,
    status: 'pending',
    progress: 0,
    createTime: new Date().toISOString()
  }
  mockExportTasks.unshift(newTask)
  return mockSuccess(newTask)
})

register('put', '/api/export/tasks/:id/cancel', async (config, params) => {
  await mockDelay(200)
  const task = mockExportTasks.find(t => t.id === parseIntOr(params.id, 0))
  if (task) task.status = 'cancelled'
  return mockSuccess({ cancelled: true })
})

register('delete', '/api/export/tasks/:id', async (config, params) => {
  await mockDelay(200)
  const idx = mockExportTasks.findIndex(t => t.id === parseIntOr(params.id, 0))
  if (idx !== -1) mockExportTasks.splice(idx, 1)
  return mockSuccess(null)
})

// ====== Mock 请求分发处理器 ======
export default async function mockHandler(config) {
  const { method = 'get', url = '' } = config

  // 遍历路由表匹配
  for (const route of routes) {
    if (route.method !== method.toLowerCase()) continue
    const params = extractParams(route.pattern, url)
    if (params !== null) {
      return route.handler(config, params)
    }
  }

  // 未匹配的路由
  console.warn(`[Mock] No handler for ${method.toUpperCase()} ${url}`)
  return mockError(`未模拟的请求: ${method.toUpperCase()} ${url}`, 404)
}
