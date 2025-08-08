import request from '@/utils/request'

// 图连接管理API
export const connectionApi = {
  // 获取连接列表
  getConnections(params) {
    return request.get('/api/connections', { params }).then(response => {
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

  // 新增连接
  createConnection(data) {
    return request.post('/api/connections', data)
  },

  // 更新连接
  updateConnection(id, data) {
    return request.put(`/api/connections/${id}`, data)
  },

  // 删除连接
  deleteConnection(id) {
    return request.delete(`/api/connections/${id}`)
  },

  // 连接数据库
  connectDatabase(id) {
    return request.post(`/api/connections/${id}/connect`)
  },

  // 断开连接
  disconnectDatabase(id) {
    return request.post(`/api/connections/${id}/disconnect`)
  },

  // 测试连接
  testConnection(id) {
    return request.post(`/api/connections/${id}/test`)
  }
}

export default connectionApi