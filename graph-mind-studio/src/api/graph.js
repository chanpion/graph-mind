import request, { uploadRequest } from '@/utils/request'

// 图管理API
export const graphApi = {
  // 获取图列表
  getGraphs(params) {
    return request.get('/api/graphs', { params }).then(response => {
      // 处理后端返回的分页数据格式
      if (response.data && typeof response.data === 'object') {
        // 确保records字段存在
        if (!response.data.records) {
          response.data.records = response.data.list || []
        }
        // 确保total字段存在
        if (response.data.total === undefined) {
          response.data.total = response.data.records.length
        }
      }
      return response
    })
  },

  // 根据连接ID获取图列表
  getGraphsByConnectionId(connectionId, params) {
    return request.get(`/api/graphs/connection/${connectionId}`, { params }).then(response => {
      // 处理后端返回的分页数据格式
      if (response.data && typeof response.data === 'object') {
        // 确保records字段存在
        if (!response.data.records) {
          response.data.records = response.data.list || []
        }
        // 确保total字段存在
        if (response.data.total === undefined) {
          response.data.total = response.data.records.length
        }
      }
      return response
    })
  },

  // 新增图
  createGraph(data) {
    return request.post('/api/graphs', data)
  },

  // 更新图
  updateGraph(id, data) {
    return request.put(`/api/graphs/${id}`, data)
  },

  // 删除图
  deleteGraph(id) {
    return request.delete(`/api/graphs/${id}`)
  },

  // 获取图详情
  getGraph(id) {
    return request.get(`/api/graphs/${id}`)
  },
  
  // 获取点定义列表
  getNodeDefs(graphId) {
    return request.get(`/api/graphs/${graphId}/nodes`)
  },
  
  // 获取边定义列表
  getEdgeDefs(graphId) {
    return request.get(`/api/graphs/${graphId}/edges`)
  },
  
  // 新增点定义
  addNodeDef(graphId, data) {
    return request.post(`/api/graphs/${graphId}/nodes`, data)
  },
  
  // 更新点定义
  updateNodeDef(graphId, nodeId, data) {
    return request.put(`/api/graphs/${graphId}/nodes/${nodeId}`, data)
  },
  
  // 删除点定义
  deleteNodeDef(graphId, nodeId) {
    return request.delete(`/api/graphs/${graphId}/nodes/${nodeId}`)
  },
  
  // 新增边定义
  addEdgeDef(graphId, data) {
    return request.post(`/api/graphs/${graphId}/edges`, data)
  },
  
  // 更新边定义
  updateEdgeDef(graphId, edgeId, data) {
    return request.put(`/api/graphs/${graphId}/edges/${edgeId}`, data)
  },
  
  // 删除边定义
  deleteEdgeDef(graphId, edgeId) {
    return request.delete(`/api/graphs/${graphId}/edges/${edgeId}`)
  },
  
  // 导入点数据
  importNodeData(graphId, nodeTypeId, data) {
    // 使用FormData时，让浏览器自动设置Content-Type（包括boundary）
    return uploadRequest.post(`/api/graphs/${graphId}/nodes/${nodeTypeId}/import`, data)
  },
  
  // 导入边数据
  importEdgeData(graphId, edgeTypeId, data) {
    // 使用FormData时，让浏览器自动设置Content-Type（包括boundary）
    return uploadRequest.post(`/api/graphs/${graphId}/edges/${edgeTypeId}/import`, data)
  },
  
  // 图查询
  queryGraph(graphId, cypher) {
    return request.post(`/api/graphs/${graphId}/query`, { cypher })
  }
}

export default graphApi