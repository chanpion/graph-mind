import request from '@/utils/request'

// 用户管理API
export const userApi = {
  // 获取用户列表
  getUsers(params) {
    return request.get('/api/users', { params }).then(response => {
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

  // 获取用户详情
  getUser(userId) {
    return request.get(`/api/users/${userId}`)
  },
  
  // 获取当前用户信息
  getCurrentUser(params) {
    return request.get('/api/users/profile', { params })
  },

  // 新增用户
  createUser(data) {
    return request.post('/api/users', data)
  },

  // 更新用户
  updateUser(userId, data) {
    return request.put(`/api/users/${userId}`, data)
  },
  
  // 更新当前用户信息
  updateCurrentUser(data) {
    return request.put('/api/users/profile', data)
  },
  
  // 修改密码
  changePassword(data) {
    return request.put('/api/users/profile/password', data)
  },

  // 删除用户
  deleteUser(userId) {
    return request.delete(`/api/users/${userId}`)
  },

  // 批量删除用户
  deleteUsers(userIds) {
    return request.delete('/api/users', { data: { ids: userIds } })
  },

  // 重置用户密码
  resetPassword(userId, password) {
    return request.post(`/api/users/${userId}/password/reset`, { password })
  }
}

export default userApi