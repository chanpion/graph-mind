import request from '@/api/request'

/**
 * 角色管理 API
 * 遵循 /api/roles/{id} 风格
 */
export const roleApi = {
  /** 获取角色列表 */
  list(params) {
    return request.get('/api/roles', { params })
  },

  /** 获取角色详情 */
  get(roleId) {
    return request.get(`/api/roles/${roleId}`)
  },

  /** 新增角色 */
  create(data) {
    return request.post('/api/roles', data)
  },

  /** 更新角色 */
  update(roleId, data) {
    return request.put(`/api/roles/${roleId}`, data)
  },

  /** 删除角色（支持批量） */
  delete(roleIds) {
    return request.delete('/api/roles', { data: roleIds })
  },

  /** 更新角色状态 */
  updateStatus(roleId, status) {
    return request.put(`/api/roles/${roleId}/status`, null, { params: { status } })
  },

  /** 获取角色数据权限 */
  getDataScope(roleId) {
    return request.get(`/api/roles/${roleId}/dataScope`)
  },

  /** 更新角色数据权限 */
  updateDataScope(roleId, dataScope) {
    return request.put(`/api/roles/${roleId}/dataScope`, null, { params: { dataScope } })
  }
}

export default roleApi
