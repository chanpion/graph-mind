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
  },
  
  // 展开节点（查询与指定节点相关的邻居节点和边）
  expandNode(graphId, nodeId, depth = 1) {
    return request.post(`/api/graphs/${graphId}/expand`, { 
      nodeId, 
      depth 
    })
  },
  
  // 查找两个节点之间的路径
  findPath(graphId, startNodeId, endNodeId, maxDepth = 5) {
    return request.post(`/api/graphs/${graphId}/path`, { 
      startNodeId, 
      endNodeId, 
      maxDepth 
    })
  },

  // 获取图统计信息
  getGraphSummary(graphId) {
    return request.get(`/api/graphs/${graphId}/summary`)
  },

  publishSchema(graphId){
    return request.post(`/api/graphs/${graphId}/publish`)
  },

  deleteNode(graphId, nodeId, label) {
    return request.delete(`/api/graphs/${graphId}/data/nodes/${nodeId}`, {
      params: { label }
    })
  },
  
  // 图数据管理相关接口
  // 获取点数据列表
  getNodeDataList(graphId, nodeTypeId, params) {
    return request.get(`/api/graphs/${graphId}/nodes/${nodeTypeId}`, { params })
  },
  
  // 获取边数据列表
  getEdgeDataList(graphId, edgeTypeId, params) {
    return request.get(`/api/graphs/${graphId}/edges/${edgeTypeId}`, { params })
  },
  
  // 获取点数据详情
  getNodeData(graphId, nodeId) {
    return request.get(`/api/graphs/${graphId}/data/nodes/${nodeId}`)
  },
  
  // 获取边数据详情
  getEdgeData(graphId, edgeId) {
    return request.get(`/api/graphs/${graphId}/data/edges/${edgeId}`)
  },
  
  // 新增点数据
  addNodeData(graphId, nodeTypeId, data) {
    return request.post(`/api/graphs/${graphId}/data/nodes/${nodeTypeId}`, data)
  },
  
  // 新增边数据
  addEdgeData(graphId, edgeTypeId, data) {
    return request.post(`/api/graphs/${graphId}/data/edges/${edgeTypeId}`, data)
  },
  
  // 更新点数据
  updateNodeData(graphId, nodeId, data) {
    return request.put(`/api/graphs/${graphId}/data/nodes/${nodeId}`, data)
  },
  
  // 更新边数据
  updateEdgeData(graphId, edgeId, data) {
    return request.put(`/api/graphs/${graphId}/data/edges/${edgeId}`, data)
  },
  
  // 删除边数据
  deleteEdge(graphId, edgeId, label) {
    return request.delete(`/api/graphs/${graphId}/data/edges/${edgeId}`, {
      params: { label }
    })
  }
}

export default graphApi