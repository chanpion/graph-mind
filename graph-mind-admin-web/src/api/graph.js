import request from '@/utils/request'

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
  }
}

export default graphApi