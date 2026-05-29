import request from '@/api/request'

/**
 * 权限菜单管理 API
 * 遵循 /api/permissions/{id} 风格
 */
export const permissionApi = {
  /** 获取权限菜单树 */
  list() {
    return request.get('/api/permissions')
  },

  /** 获取权限详情 */
  get(permissionId) {
    return request.get(`/api/permissions/${permissionId}`)
  },

  /** 新增权限 */
  create(data) {
    return request.post('/api/permissions', data)
  },

  /** 更新权限 */
  update(permissionId, data) {
    return request.put(`/api/permissions/${permissionId}`, data)
  },

  /** 删除权限 */
  delete(permissionId) {
    return request.delete(`/api/permissions/${permissionId}`)
  },

  /** 更新权限状态 */
  updateStatus(permissionId, status) {
    return request.put(`/api/permissions/${permissionId}/status`, null, { params: { status } })
  }
}

export default permissionApi
