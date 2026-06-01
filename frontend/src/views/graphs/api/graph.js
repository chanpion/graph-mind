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
    return request.get(`/api/graphs/connection/${connectionId}`, { params })
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

  /** 删除图 */
  delete(id) {
    return request.delete(`/api/graphs/${id}`)
  },

  // ====== Schema 管理 ======

  /** 获取点定义列表 */
  getNodeDefs(graphId) {
    return request.get(`/api/graphs/${graphId}/nodes`)
  },

  /** 新增点定义 */
  addNodeDef(graphId, data) {
    return request.post(`/api/graphs/${graphId}/nodes`, data)
  },

  /** 更新点定义 */
  updateNodeDef(graphId, nodeId, data) {
    return request.put(`/api/graphs/${graphId}/nodes/${nodeId}`, data)
  },

  /** 删除点定义 */
  deleteNodeDef(graphId, nodeId) {
    return request.delete(`/api/graphs/${graphId}/nodes/${nodeId}`)
  },

  /** 获取边定义列表 */
  getEdgeDefs(graphId) {
    return request.get(`/api/graphs/${graphId}/edges`)
  },

  /** 新增边定义 */
  addEdgeDef(graphId, data) {
    return request.post(`/api/graphs/${graphId}/edges`, data)
  },

  /** 更新边定义 */
  updateEdgeDef(graphId, edgeId, data) {
    return request.put(`/api/graphs/${graphId}/edges/${edgeId}`, data)
  },

  /** 删除边定义 */
  deleteEdgeDef(graphId, edgeId) {
    return request.delete(`/api/graphs/${graphId}/edges/${edgeId}`)
  },

  /** 发布 Schema */
  publishSchema(graphId) {
    return request.post(`/api/graphs/${graphId}/publish`)
  },

  // ====== 图数据操作 ======

  /** 获取点数据列表 */
  getNodeDataList(graphId, nodeTypeId, params) {
    return request.get(`/api/graphs/${graphId}/nodes/${nodeTypeId}`, { params })
  },

  /** 获取边数据列表 */
  getEdgeDataList(graphId, edgeTypeId, params) {
    return request.get(`/api/graphs/${graphId}/edges/${edgeTypeId}`, { params })
  },

  /** 新增点数据 */
  addNodeData(graphId, nodeTypeId, data) {
    return request.post(`/api/graphs/${graphId}/data/nodes/${nodeTypeId}`, data)
  },

  /** 新增边数据 */
  addEdgeData(graphId, edgeTypeId, data) {
    return request.post(`/api/graphs/${graphId}/data/edges/${edgeTypeId}`, data)
  },

  /** 更新点数据 */
  updateNodeData(graphId, nodeId, data) {
    return request.put(`/api/graphs/${graphId}/data/nodes/${nodeId}`, data)
  },

  /** 更新边数据 */
  updateEdgeData(graphId, edgeId, data) {
    return request.put(`/api/graphs/${graphId}/data/edges/${edgeId}`, data)
  },

  /** 删除节点 */
  deleteNode(graphId, nodeId, label) {
    return request.delete(`/api/graphs/${graphId}/data/nodes/${nodeId}`, {
      params: { label }
    })
  },

  /** 删除边 */
  deleteEdge(graphId, edgeId, label) {
    return request.delete(`/api/graphs/${graphId}/data/edges/${edgeId}`, {
      params: { label }
    })
  },

  // ====== 查询与分析 ======

  /** 执行查询 */
  queryGraph(graphId, cypher) {
    return request.post(`/api/graphs/${graphId}/query`, { cypher })
  },

  /** 展开节点 */
  expandNode(graphId, nodeId, depth = 1) {
    return request.post(`/api/graphs/${graphId}/expand`, { nodeId, depth })
  },

  /** 查找路径 */
  findPath(graphId, startNodeId, endNodeId, maxDepth = 5) {
    return request.post(`/api/graphs/${graphId}/path`, { startNodeId, endNodeId, maxDepth })
  },

  /** 获取图统计 */
  getGraphSummary(graphId) {
    return request.get(`/api/graphs/${graphId}/summary`)
  },

  // ====== Schema 导入导出 ======

  /** 导出 Schema（节点定义和边定义） */
  exportSchema(graphId) {
    return request.get(`/api/graphs/${graphId}/schema/export`)
  },

  /** 导入 Schema */
  importSchema(graphId, data) {
    return request.post(`/api/graphs/${graphId}/schema/import`, data)
  },

  // ====== 数据导入 ======

  /** 导入节点数据（CSV） */
  importNodeData(graphId, nodeTypeId, formData) {
    return request.post(`/api/graphs/${graphId}/import/nodes/${nodeTypeId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /** 导入边数据（CSV） */
  importEdgeData(graphId, edgeTypeId, formData) {
    return request.post(`/api/graphs/${graphId}/import/edges/${edgeTypeId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default graphApi
