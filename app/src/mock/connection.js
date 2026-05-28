/**
 * 连接管理 Mock 数据
 */

import { mockDelay, mockSuccess, mockError } from './index'

// 模拟连接列表数据
let mockConnections = [
  {
    id: 1,
    name: 'Neo4j 本地测试',
    type: 'NEO4J',
    host: 'localhost',
    port: 7687,
    username: 'neo4j',
    password: 'password',
    description: '本地 Neo4j 数据库连接',
    status: 1,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString()
  },
  {
    id: 2,
    name: 'NebulaGraph 测试环境',
    type: 'NEBULA',
    host: '192.168.1.100',
    port: 9669,
    username: 'root',
    password: 'password',
    description: 'NebulaGraph 测试环境',
    status: 1,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString()
  },
  {
    id: 3,
    name: 'JanusGraph 集群',
    type: 'JANUS',
    host: '10.0.0.50',
    port: 8182,
    username: 'admin',
    password: 'password',
    description: 'JanusGraph 分布式集群',
    status: 1,
    storageBackend: 'cql',
    storageHost: 'cassandra.example.com',
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString()
  },
  {
    id: 4,
    name: '开发数据库',
    type: 'NEO4J',
    host: '127.0.0.1',
    port: 7687,
    username: 'developer',
    password: 'dev123',
    description: '开发环境数据库',
    status: 0,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString()
  }
]

export const mockListConnections = async (config) => {
  await mockDelay()
  const params = config?.params || {}
  const page = params.page || 1
  const pageSize = params.pageSize || 10
  const keyword = params.keyword || ''
  const type = params.type || ''

  // 按 keyword 和 type 过滤
  let filtered = mockConnections
  if (keyword) {
    const kw = keyword.toLowerCase()
    filtered = filtered.filter(item =>
      item.name.toLowerCase().includes(kw) ||
      (item.host || '').toLowerCase().includes(kw)
    )
  }
  if (type) {
    filtered = filtered.filter(item => (item.type || '').toUpperCase() === type.toUpperCase())
  }

  const start = (page - 1) * pageSize
  const paged = filtered.slice(start, start + pageSize)
  return mockSuccess({
    records: paged,
    total: filtered.length,
    current: page,
    size: pageSize
  })
}

export const mockCreateConnection = async (data) => {
  await mockDelay()
  const newConnection = {
    id: mockConnections.length > 0 ? Math.max(...mockConnections.map(c => c.id)) + 1 : 1,
    ...data,
    status: 1,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString()
  }
  mockConnections.push(newConnection)
  return mockSuccess(newConnection)
}

export const mockUpdateConnection = async (id, data) => {
  await mockDelay()
  const index = mockConnections.findIndex(c => c.id === parseInt(id))
  if (index === -1) {
    return mockError('连接不存在', 404)
  }
  mockConnections[index] = { ...mockConnections[index], ...data, updateTime: new Date().toISOString() }
  return mockSuccess(mockConnections[index])
}

export const mockDeleteConnection = async (id) => {
  await mockDelay()
  const index = mockConnections.findIndex(c => c.id === parseInt(id))
  if (index === -1) {
    return mockError('连接不存在', 404)
  }
  mockConnections.splice(index, 1)
  return mockSuccess(null)
}

export const mockTestConnection = async (id) => {
  await mockDelay(500)
  const connection = mockConnections.find(c => c.id === parseInt(id))
  if (!connection) {
    return mockError('连接不存在', 404)
  }
  const success = Math.random() > 0.2
  connection.status = success ? 1 : 0
  return success
    ? mockSuccess(true)
    : mockError('连接测试失败：无法连接到服务器', 500)
}
