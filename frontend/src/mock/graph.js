/**
 * 图管理 Mock 数据
 */

import { mockDelay, mockSuccess, mockError } from './index'

const mockGraphs = [
  {
    id: 1,
    name: 'social_network',
    graphName: 'social_network',
    graphType: 'NEO4J',
    connectionId: 1,
    vertexCount: 12580,
    edgeCount: 34256,
    sourceType: 'PLATFORM',
    status: 0,
    createTime: new Date('2024-01-15T10:30:00').toISOString(),
    createdAt: new Date('2024-01-15T10:30:00').toISOString(),
    description: '社交网络图'
  },
  {
    id: 2,
    name: 'product_graph',
    graphName: 'product_graph',
    graphType: 'NEBULA',
    connectionId: 2,
    vertexCount: 8432,
    edgeCount: 15678,
    sourceType: 'PLATFORM',
    status: 0,
    createTime: new Date('2024-02-20T14:20:00').toISOString(),
    createdAt: new Date('2024-02-20T14:20:00').toISOString(),
    description: '产品关系图'
  },
  {
    id: 3,
    name: 'knowledge_base',
    graphName: 'knowledge_base',
    graphType: 'NEO4J',
    connectionId: 1,
    vertexCount: 45210,
    edgeCount: 78932,
    sourceType: 'EXISTING',
    status: 0,
    createTime: new Date('2024-03-10T09:15:00').toISOString(),
    createdAt: new Date('2024-03-10T09:15:00').toISOString(),
    description: '知识库图'
  },
  {
    id: 4,
    name: 'archived_graph',
    graphName: 'archived_graph',
    graphType: 'JANUS',
    connectionId: 3,
    vertexCount: 5620,
    edgeCount: 9843,
    sourceType: 'PLATFORM',
    status: 2,
    createTime: new Date('2023-12-05T16:45:00').toISOString(),
    createdAt: new Date('2023-12-05T16:45:00').toISOString(),
    description: '已归档的图'
  }
]

const mockSchemas = {
  1: {
    graphId: 1,
    graphName: 'social_network',
    graphType: 'NEO4J',
    vertexLabels: [
      {
        id: 101, label: 'Person', name: '人员',
        description: '人员顶点',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 },
          { code: 'name', name: '姓名', type: 'String', indexed: true, status: 1 },
          { code: 'age', name: '年龄', type: 'Int', indexed: false, status: 1 },
          { code: 'email', name: '邮箱', type: 'String', indexed: true, status: 1 }
        ]
      },
      {
        id: 102, label: 'Company', name: '公司',
        description: '公司顶点',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 },
          { code: 'name', name: '公司名称', type: 'String', indexed: true, status: 1 },
          { code: 'industry', name: '行业', type: 'String', indexed: false, status: 0 }
        ]
      }
    ],
    edgeLabels: [
      {
        id: 201, label: 'WORKS_AT', name: '就职于',
        startLabel: 'Person', endLabel: 'Company',
        from: 101, to: 102,
        description: '工作关系',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 },
          { code: 'since', name: '入职时间', type: 'Date', indexed: false, status: 1 },
          { code: 'position', name: '职位', type: 'String', indexed: false, status: 1 }
        ]
      },
      {
        id: 202, label: 'KNOWS', name: '认识',
        startLabel: 'Person', endLabel: 'Person',
        from: 101, to: 101,
        description: '认识关系',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 }
        ]
      }
    ]
  },
  2: {
    graphId: 2,
    graphName: 'product_graph',
    graphType: 'NEBULA',
    vertexLabels: [
      {
        id: 103, label: 'Product', name: '产品',
        description: '产品顶点',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 },
          { code: 'name', name: '产品名称', type: 'String', indexed: true, status: 1 },
          { code: 'price', name: '价格', type: 'Float', indexed: false, status: 1 }
        ]
      },
      {
        id: 104, label: 'Category', name: '分类',
        description: '产品分类',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 },
          { code: 'name', name: '分类名称', type: 'String', indexed: true, status: 1 }
        ]
      }
    ],
    edgeLabels: [
      {
        id: 203, label: 'BELONGS_TO', name: '属于',
        from: 103, to: 104,
        description: '产品属于分类',
        status: 1,
        properties: [
          { code: 'uid', name: '唯一标识', type: 'String', indexed: true, status: 1 }
        ]
      }
    ]
  }
}

export const mockListGraphs = async (connectionId) => {
  await mockDelay()
  if (connectionId) {
    const id = parseInt(connectionId)
    return mockSuccess(mockGraphs.filter(g => g.connectionId === id))
  }
  return mockSuccess([...mockGraphs])
}

export const mockGetGraphSchema = async (connectionId, graphName) => {
  await mockDelay()
  const schema = mockSchemas[graphName] || {
    graphName,
    graphType: 'NEO4J',
    vertexLabels: [],
    edgeLabels: []
  }
  return mockSuccess(schema)
}

export const mockCreateGraph = async (connectionId, data) => {
  await mockDelay()
  const maxId = mockGraphs.length > 0 ? Math.max(...mockGraphs.map(g => g.id)) : 0
  const newId = maxId + 1
  const newGraph = {
    id: newId,
    name: data.graphName || `graph_${Date.now()}`,
    graphName: data.graphName || `graph_${Date.now()}`,
    connectionId: data.connectionId || connectionId,
    graphType: 'NEO4J',
    vertexCount: 0,
    edgeCount: 0,
    sourceType: 'PLATFORM',
    status: 0,
    createTime: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    description: data.description || data.graphDisplayName || '新建图'
  }
  mockGraphs.unshift(newGraph)
  // 为新图创建空的 schema 数据
  mockSchemas[newId] = {
    graphId: newId,
    graphName: data.graphName || `graph_${Date.now()}`,
    graphType: 'NEO4J',
    vertexLabels: [],
    edgeLabels: []
  }
  return mockSuccess(newGraph)
}

export const mockDeleteGraph = async (graphId) => {
  await mockDelay()
  const id = parseInt(graphId)
  const index = mockGraphs.findIndex(g => g.id === id)
  if (index === -1) {
    return mockError('图不存在', 404)
  }
  mockGraphs.splice(index, 1)
  delete mockSchemas[id]
  return mockSuccess({ message: '删除成功' })
}

export const mockGetVertexTypes = async (connectionId, graphId) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  return mockSuccess(schema?.vertexLabels || [])
}

export const mockCreateVertexType = async (connectionId, graphId, data) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  if (!schema) {
    return mockError('图不存在', 404)
  }
  const maxId = schema.vertexLabels.length > 0 ? Math.max(...schema.vertexLabels.map(v => v.id)) : 100
  const newVertex = {
    ...data,
    id: maxId + 1,
    properties: data.properties || []
  }
  schema.vertexLabels.push(newVertex)
  return mockSuccess(newVertex)
}

export const mockDeleteVertexType = async (connectionId, graphId, vertexId) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  if (!schema) {
    return mockError('图不存在', 404)
  }
  const id = parseInt(vertexId)
  schema.vertexLabels = schema.vertexLabels.filter(v => v.id !== id)
  return mockSuccess({ message: '删除成功' })
}

export const mockGetEdgeTypes = async (connectionId, graphId) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  return mockSuccess(schema?.edgeLabels || [])
}

export const mockCreateEdgeType = async (connectionId, graphId, data) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  if (!schema) {
    return mockError('图不存在', 404)
  }
  const maxId = schema.edgeLabels.length > 0 ? Math.max(...schema.edgeLabels.map(e => e.id)) : 200
  const newEdge = {
    ...data,
    id: maxId + 1,
    properties: data.properties || []
  }
  schema.edgeLabels.push(newEdge)
  return mockSuccess(newEdge)
}

export const mockUpdateVertexType = async (connectionId, graphId, vertexId, data) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  if (!schema) return mockError('图不存在', 404)
  const id = parseInt(vertexId)
  const index = schema.vertexLabels.findIndex(v => v.id === id)
  if (index === -1) return mockError('点定义不存在', 404)
  schema.vertexLabels[index] = { ...schema.vertexLabels[index], ...data }
  return mockSuccess(schema.vertexLabels[index])
}

export const mockUpdateEdgeType = async (connectionId, graphId, edgeId, data) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  if (!schema) return mockError('图不存在', 404)
  const id = parseInt(edgeId)
  const index = schema.edgeLabels.findIndex(e => e.id === id)
  if (index === -1) return mockError('边定义不存在', 404)
  schema.edgeLabels[index] = { ...schema.edgeLabels[index], ...data }
  return mockSuccess(schema.edgeLabels[index])
}

export { mockSchemas }

export const mockDeleteEdgeType = async (connectionId, graphId, edgeId) => {
  await mockDelay()
  const schema = mockSchemas[graphId]
  if (!schema) {
    return mockError('图不存在', 404)
  }
  const id = parseInt(edgeId)
  schema.edgeLabels = schema.edgeLabels.filter(e => e.id !== id)
  return mockSuccess({ message: '删除成功' })
}
