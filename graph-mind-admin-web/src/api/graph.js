import request from '@/utils/request'

const graphApi = {
  getGraphList(params) {
    return request.get('/graphs', { params })
  },
  getNodeDefs(graphId) {
    return request.get(`/graphs/${graphId}/nodes`)
  },
  getEdgeDefs(graphId) {
    return request.get(`/graphs/${graphId}/edges`)
  },
  addNodeDef(graphId, data) {
    return request.post(`/graphs/${graphId}/nodes`, data)
  },
  updateNodeDef(graphId, nodeId, data) {
    return request.put(`/graphs/${graphId}/nodes/${nodeId}`, data)
  },
  deleteNodeDef(graphId, nodeId) {
    return request.delete(`/graphs/${graphId}/nodes/${nodeId}`)
  },
  addEdgeDef(graphId, data) {
    return request.post(`/graphs/${graphId}/edges`, data)
  },
  updateEdgeDef(graphId, edgeId, data) {
    return request.put(`/graphs/${graphId}/edges/${edgeId}`, data)
  },
  deleteEdgeDef(graphId, edgeId) {
    return request.delete(`/graphs/${graphId}/edges/${edgeId}`)
  },
  queryGraph(graphId, query) {
    return request.post(`/graphs/${graphId}/query`, { query })
  },
  // 图数据加工相关接口
  prepareImport(graphId, file, dataType) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('dataType', dataType)
    return request.post(`/graphs/${graphId}/import/prepare`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  executeImport(graphId, data) {
    return request.post(`/graphs/${graphId}/import/execute`, data)
  }
}

export default graphApi