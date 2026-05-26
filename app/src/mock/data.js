/**
 * 数据查询 Mock 数据
 */

import { mockDelay, mockSuccess, mockError } from './index'

const mockVertices = {
  'Person': [
    { uid: 'person_001', label: '张三', properties: { name: '张三', age: 28, email: 'zhangsan@example.com', city: '北京' } },
    { uid: 'person_002', label: '李四', properties: { name: '李四', age: 32, email: 'lisi@example.com', city: '上海' } },
    { uid: 'person_003', label: '王五', properties: { name: '王五', age: 25, email: 'wangwu@example.com', city: '广州' } },
    { uid: 'person_004', label: '赵六', properties: { name: '赵六', age: 30, email: 'zhaoliu@example.com', city: '深圳' } },
    { uid: 'person_005', label: '孙七', properties: { name: '孙七', age: 27, email: 'sunqi@example.com', city: '杭州' } }
  ],
  'Company': [
    { uid: 'company_001', label: '腾讯科技', properties: { name: '腾讯科技', industry: '互联网', founded: '1998', city: '深圳' } },
    { uid: 'company_002', label: '阿里巴巴', properties: { name: '阿里巴巴', industry: '电子商务', founded: '1999', city: '杭州' } },
    { uid: 'company_003', label: '字节跳动', properties: { name: '字节跳动', industry: '互联网', founded: '2012', city: '北京' } }
  ]
}

const mockEdges = [
  { id: 'edge_001', source: 'person_001', sourceLabel: 'Person', target: 'company_001', targetLabel: 'Company', label: 'WORKS_AT', properties: { position: '软件工程师', since: '2020-01-15' } },
  { id: 'edge_002', source: 'person_002', sourceLabel: 'Person', target: 'company_002', targetLabel: 'Company', label: 'WORKS_AT', properties: { position: '产品经理', since: '2019-06-10' } },
  { id: 'edge_003', source: 'person_003', sourceLabel: 'Person', target: 'company_003', targetLabel: 'Company', label: 'WORKS_AT', properties: { position: '算法工程师', since: '2021-03-20' } },
  { id: 'edge_004', source: 'person_001', sourceLabel: 'Person', target: 'person_002', targetLabel: 'Person', label: 'KNOWS', properties: { years: 5 } },
  { id: 'edge_005', source: 'person_001', sourceLabel: 'Person', target: 'person_003', targetLabel: 'Person', label: 'KNOWS', properties: { years: 3 } },
  { id: 'edge_006', source: 'person_002', sourceLabel: 'Person', target: 'person_003', targetLabel: 'Person', label: 'KNOWS', properties: { years: 2 } }
]

export const mockQueryVertices = async (connectionId, graphName, params) => {
  await mockDelay()
  const label = params.label
  const vertices = label ? mockVertices[label] || [] : []
  const pageNum = params.pageNum || 1
  const pageSize = params.pageSize || 10
  const start = (pageNum - 1) * pageSize
  const end = start + pageSize
  const pagedVertices = vertices.slice(start, end)
  return mockSuccess({ list: pagedVertices, total: vertices.length, pageNum, pageSize })
}

export const mockGetVertex = async (connectionId, graphName, uid) => {
  await mockDelay()
  for (const label in mockVertices) {
    const vertex = mockVertices[label].find(v => v.uid === uid)
    if (vertex) return mockSuccess(vertex)
  }
  return mockError('节点不存在', 404)
}

export const mockUpdateVertex = async (connectionId, graphName, uid, data) => {
  await mockDelay()
  for (const label in mockVertices) {
    const index = mockVertices[label].findIndex(v => v.uid === uid)
    if (index !== -1) {
      mockVertices[label][index].properties = { ...mockVertices[label][index].properties, ...data.properties }
      return mockSuccess(mockVertices[label][index])
    }
  }
  return mockError('节点不存在', 404)
}

export const mockDeleteVertex = async (connectionId, graphName, uid) => {
  await mockDelay()
  for (const label in mockVertices) {
    const index = mockVertices[label].findIndex(v => v.uid === uid)
    if (index !== -1) {
      mockVertices[label].splice(index, 1)
      return mockSuccess({ message: '删除成功' })
    }
  }
  return mockError('节点不存在', 404)
}

export const mockCreateVertex = async (connectionId, graphName, data) => {
  await mockDelay()
  const label = data.label || 'Person'
  if (!mockVertices[label]) mockVertices[label] = []
  const newVertex = { uid: `vertex_${Date.now()}`, label, properties: data.properties || {} }
  mockVertices[label].push(newVertex)
  return mockSuccess(newVertex)
}

export const mockQueryEdges = async (connectionId, graphName, params) => {
  await mockDelay()
  const label = params.label
  const edges = label ? mockEdges.filter(e => e.label === label) : mockEdges
  const pageNum = params.pageNum || 1
  const pageSize = params.pageSize || 10
  const start = (pageNum - 1) * pageSize
  const end = start + pageSize
  const pagedEdges = edges.slice(start, end).map(e => ({
    ...e,
    uid: e.id,
    sourceUid: e.source,
    targetUid: e.target
  }))
  return mockSuccess({ list: pagedEdges, total: edges.length, pageNum, pageSize })
}

export const mockCreateEdge = async (connectionId, graphName, data) => {
  await mockDelay()
  const newEdge = { id: `edge_${Date.now()}`, source: data.source, sourceLabel: data.sourceLabel, target: data.target, targetLabel: data.targetLabel, label: data.label, properties: data.properties || {} }
  mockEdges.push(newEdge)
  return mockSuccess(newEdge)
}

export const mockGetEdge = async (connectionId, graphName, uid) => {
  await mockDelay()
  const edge = mockEdges.find(e => e.id === uid)
  return edge ? mockSuccess(edge) : mockError('边不存在', 404)
}

export const mockUpdateEdge = async (connectionId, graphName, uid, data) => {
  await mockDelay()
  const index = mockEdges.findIndex(e => e.id === uid)
  if (index !== -1) {
    mockEdges[index].properties = { ...mockEdges[index].properties, ...data.properties }
    return mockSuccess(mockEdges[index])
  }
  return mockError('边不存在', 404)
}

export const mockDeleteEdge = async (connectionId, graphName, uid) => {
  await mockDelay()
  const index = mockEdges.findIndex(e => e.id === uid)
  if (index !== -1) {
    mockEdges.splice(index, 1)
    return mockSuccess({ message: '删除成功' })
  }
  return mockError('边不存在', 404)
}

export const mockGetDataStats = async (connectionId, graphName) => {
  await mockDelay()
  let totalVertices = 0
  const vertexLabelCount = {}
  for (const label in mockVertices) {
    vertexLabelCount[label] = mockVertices[label].length
    totalVertices += mockVertices[label].length
  }
  const edgeLabelCount = {}
  for (const edge of mockEdges) {
    edgeLabelCount[edge.label] = (edgeLabelCount[edge.label] || 0) + 1
  }
  return mockSuccess({
    vertexCount: totalVertices,
    edgeCount: mockEdges.length,
    vertexLabelCount,
    edgeLabelCount
  })
}

export const mockExecuteQuery = async (connectionId, graphName, query) => {
  await mockDelay(500)
  const queryLower = query.toLowerCase()
  let vertices = []
  if (queryLower.includes('match') && queryLower.includes('person')) vertices = mockVertices.Person.slice(0, 3)
  else if (queryLower.includes('match') && queryLower.includes('company')) vertices = mockVertices.Company
  else vertices = mockVertices.Person.slice(0, 2)
  const edges = mockEdges.filter(e => vertices.some(v => v.uid === e.source) || vertices.some(v => v.uid === e.target))
  return mockSuccess({ query, vertices, edges, executionTime: Math.floor(Math.random() * 100 + 50), resultCount: vertices.length + edges.length })
}

export const mockImportCsv = async (connectionId, graphName, config, file) => {
  await mockDelay(800)
  return mockSuccess({
    success: true, message: 'CSV文件导入成功',
    summary: { totalRows: 100, importedVertices: 45, importedEdges: 55, failedRows: 0, elapsedTime: 1200 },
    details: [{ rowNumber: 1, status: 'SUCCESS', type: 'VERTEX', label: 'Person', properties: { name: '张三', age: 28 } }]
  })
}

export const mockNativeQuery = async (connectionId, graphName, data) => {
  await mockDelay(500)
  return mockSuccess({
    success: true, message: '原生查询执行成功',
    queryLanguage: data.queryLanguage || 'Cypher',
    queryStatement: data.queryStatement || '',
    executionTime: Math.floor(Math.random() * 200 + 100),
    resultCount: 10,
    results: [
      { id: 'v1', label: 'Person', properties: { name: 'Alice', age: 30 } },
      { id: 'v2', label: 'Person', properties: { name: 'Bob', age: 25 } },
      { id: 'e1', type: 'KNOWS', source: 'v1', target: 'v2', properties: { since: '2020' } }
    ]
  })
}
