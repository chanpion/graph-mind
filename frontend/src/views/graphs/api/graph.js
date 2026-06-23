import request from '@/api/request'

/**
 * 图管理 API
 * 遵循 /api/graphs/{id} 风格
 */
export const graphApi = {
  // ====== 图 CRUD ======

  /** 获取图列表 */
  list(params) {
    return request.get('/api/graphs', { params })
  },

  /** 根据连接ID获取图列表 */
  listByConnection(connectionId, params) {
    return request.get('/api/graphs/list', { params: { ...params, connectionId } })
  },

  /** 获取图详情 */
  get(id) {
    return request.get(`/api/graphs/${id}`)
  },

  /** 创建图 */
  create(data) {
    return request.post('/api/graphs', data)
  },

  /** 更新图 */
  update(id, data) {
    return request.put(`/api/graphs/${id}`, data)
  },

  /** 删除图 — 对于发现的图（负ID）需传入 connectionId + graphCode */
  delete(id, params) {
    return request.delete(`/api/graphs/${id}`, { params })
  },

  // ====== Schema 管理 ======

  /** 获取点定义列表 */
  getVertexDefs(graphId, params) {
    return request.get('/api/graphs/schema/vertices', { params: { graphId, ...params } })
  },

  /** 新增点定义 */
  addVertexDef(graphId, data) {
    return request.post('/api/graphs/schema/vertices', data, { params: { graphId } })
  },

  /** 更新点定义（支持 discovered graph） */
  updateVertexDef(graphId, vertexId, data, params = {}) {
    return request.put('/api/graphs/schema/vertex', data, { params: { graphId, vertexId, ...params } })
  },

  /** 删除点定义 */
  deleteVertexDef(graphId, vertexId) {
    return request.delete('/api/graphs/schema/vertex', { params: { graphId, vertexId } })
  },

  /** 获取边定义列表 */
  getEdgeDefs(graphId, params) {
    return request.get('/api/graphs/schema/edges', { params: { graphId, ...params } })
  },

  /** 新增边定义（支持 discovered graph） */
  addEdgeDef(graphId, data, params = {}) {
    return request.post('/api/graphs/schema/edges', data, { params: { graphId, ...params } })
  },

  /** 更新边定义（支持 discovered graph） */
  updateEdgeDef(graphId, edgeId, data, params = {}) {
    return request.put('/api/graphs/schema/edge', data, { params: { graphId, edgeId, ...params } })
  },

  /** 删除边定义 */
  deleteEdgeDef(graphId, edgeId) {
    return request.delete('/api/graphs/schema/edge', { params: { graphId, edgeId } })
  },

  /** 发布 Schema */
  publishSchema(graphId) {
    return request.post('/api/graphs/schema/publish', null, { params: { graphId } })
  },

  // ====== 图数据操作 ======

  /** 获取点数据列表 */
  getVertexDataList(graphId, vertexTypeId, params) {
    return request.get('/api/graph/data/vertices', { params: { graphId, vertexTypeId, ...params } })
  },

  /** 获取边数据列表 */
  getEdgeDataList(graphId, edgeTypeId, params) {
    return request.get('/api/graph/data/edges', { params: { graphId, edgeTypeId, ...params } })
  },

  /** 新增点数据 */
  addVertexData(graphId, vertexTypeId, data, params) {
    return request.post('/api/graph/data/vertex', data, { params: { graphId, vertexTypeId, ...params } })
  },

  /** 新增边数据 */
  addEdgeData(graphId, edgeTypeId, data, params) {
    return request.post('/api/graph/data/edge', data, { params: { graphId, edgeTypeId, ...params } })
  },

  /** 更新点数据 */
  updateVertexData(graphId, vertexId, data, params) {
    return request.put('/api/graph/data/vertex', data, { params: { graphId, vertexId, ...params } })
  },

  /** 更新边数据 */
  updateEdgeData(graphId, edgeId, data, params) {
    return request.put('/api/graph/data/edge', data, { params: { graphId, edgeId, ...params } })
  },

  /** 删除节点 */
  deleteVertexData(graphId, vertexId, label, params) {
    return request.delete('/api/graph/data/vertex', {
      params: { graphId, vertexId, label, ...params }
    })
  },

  /** 删除边 */
  deleteEdge(graphId, edgeId, label, params) {
    return request.delete('/api/graph/data/edge', {
      params: { graphId, edgeId, label, ...params }
    })
  },

  // ====== 查询与分析 ======

  /** 执行查询 */
  queryGraph(graphId, query, params) {
    return request.post('/api/graphs/query', { query }, { params: { graphId, ...params } })
  },

  /** 展开节点 */
  expandVertex(graphId, vertexId, depth = 1, connectionId, graphCode, label, property) {
    return request.post('/api/graphs/expand', {
      graphId,
      vertexId,
      depth,
      label,
      property,
      connectionId,
      graphCode
    })
  },

  /** 查找路径 — 支持按 label+property+value 查找起点/终点 */
  findPath(params) {
    return request.post('/api/graphs/path', params)
  },

  /** 获取图统计 */
  getGraphSummary(graphId, params) {
    return request.get('/api/graphs/summary', { params: { graphId, ...params } })
  },

  // ====== Schema 导入导出 ======

  /** 导出 Schema（节点定义和边定义） */
  exportSchema(graphId) {
    return request.get('/api/graphs/schema/export', { params: { graphId } })
  },

  /** 导入 Schema */
  importSchema(graphId, data) {
    return request.post('/api/graphs/schema/import', data, { params: { graphId } })
  },

  // ====== 数据导入 ======

  /** 导入节点数据（CSV） */
  importVertexData(graphId, vertexTypeId, formData) {
    return request.post('/api/graph/data/importVertices', formData, {
      params: { graphId, vertexTypeId },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /** 导入边数据（CSV） */
  importEdgeData(graphId, edgeTypeId, formData) {
    return request.post('/api/graph/data/importEdges', formData, {
      params: { graphId, edgeTypeId },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default graphApi
