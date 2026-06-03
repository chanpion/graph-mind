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

// URL 解析辅助：从 /api/graphs/:graphId/vertices/:vertexId 中提取参数
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
register('get', '/api/graphs/:graphId/vertices', async (config, params) => {
  const res = await mockGetVertexTypes(null, params.graphId)
  return res
})

register('post', '/api/graphs/:graphId/vertices', async (config, params) => {
  return mockCreateVertexType(null, params.graphId, config.data)
})

register('put', '/api/graphs/:graphId/vertices/:vertexId', async (config, params) => {
  await mockDelay(300)
  return mockSuccess({ id: params.vertexId, ...config.data })
})

register('delete', '/api/graphs/:graphId/vertices/:vertexId', async (config, params) => {
  return mockDeleteVertexType(null, params.graphId, params.vertexId)
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
register('get', '/api/graphs/:graphId/vertices/:vertexTypeId', async (config, params) => {
  // 从 schema mock 中解析节点类型 label
  const schema = mockSchemas[params.graphId]
  const vertexLabel = schema?.vertexLabels?.find(v => v.id == params.vertexTypeId)?.label || params.vertexTypeId
  return mockQueryVertices(null, params.graphId, { ...config.params, label: vertexLabel })
})

register('get', '/api/graphs/:graphId/edges/:edgeTypeId', async (config, params) => {
  // 从 schema mock 中解析边类型 label
  const schema = mockSchemas[params.graphId]
  const edgeLabel = schema?.edgeLabels?.find(e => e.id == params.edgeTypeId)?.label || params.edgeTypeId
  return mockQueryEdges(null, params.graphId, { ...config.params, label: edgeLabel })
})

register('post', '/api/graphs/:graphId/data/vertices/:vertexTypeId', async (config, params) => {
  // 从 schema mock 中解析节点类型 label
  const schema = mockSchemas[params.graphId]
  const vertexLabel = schema?.vertexLabels?.find(v => v.id == params.vertexTypeId)?.label || config.data.label || params.vertexTypeId
  return mockCreateVertex(null, params.graphId, { ...config.data, label: vertexLabel })
})

register('post', '/api/graphs/:graphId/data/edges/:edgeTypeId', async (config, params) => {
  const schema = mockSchemas[params.graphId]
  const edgeLabel = schema?.edgeLabels?.find(e => e.id == params.edgeTypeId)?.label || config.data.label || params.edgeTypeId
  return mockCreateEdge(null, params.graphId, { ...config.data, label: edgeLabel })
})

register('put', '/api/graphs/:graphId/data/vertices/:vertexId', async (config, params) => {
  return mockUpdateVertex(null, params.graphId, params.vertexId, config.data)
})

register('put', '/api/graphs/:graphId/data/edges/:edgeId', async (config, params) => {
  return mockUpdateEdge(null, params.graphId, params.edgeId, config.data)
})

register('delete', '/api/graphs/:graphId/data/vertices/:vertexId', async (config, params) => {
  return mockDeleteVertex(null, params.graphId, params.vertexId)
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
register('post', '/api/graphs/:graphId/import/vertices/:vertexTypeId', async (config, params) => {
  return mockImportCsv(null, params.graphId, config.data, null)
})

register('post', '/api/graphs/:graphId/import/edges/:edgeTypeId', async (config, params) => {
  return mockImportCsv(null, params.graphId, config.data, null)
})

// ====== 用户管理 ======
const mockUsers = [
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
