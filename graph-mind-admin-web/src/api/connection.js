import request from '@/utils/request'

// 图连接管理API
export const connectionApi = {
  // 获取连接列表
  getConnections(params) {
    return request.get('/connections', { params })
  },

  // 新增连接
  createConnection(data) {
    return request.post('/connections', data)
  },

  // 更新连接
  updateConnection(id, data) {
    return request.put(`/connections/${id}`, data)
  },

  // 删除连接
  deleteConnection(id) {
    return request.delete(`/connections/${id}`)
  },

  // 连接数据库
  connectDatabase(id) {
    return request.post(`/connections/${id}/connect`)
  },

  // 断开连接
  disconnectDatabase(id) {
    return request.post(`/connections/${id}/disconnect`)
  },

  // 测试连接
  testConnection(id) {
    return request.post(`/connections/${id}/test`)
  }
}

export default connectionApi 