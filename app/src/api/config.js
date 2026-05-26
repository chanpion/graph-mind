import request from '@/api/request'

/**
 * 配置管理 API
 * 遵循对象导出风格，与 userApi、roleApi、permissionApi 保持一致
 */
export const configApi = {
  /**
   * 获取配置列表
   * @param {Object} params 查询参数
   * @returns 配置列表
   */
  list(params) {
    return request({
      url: '/api/configs',
      method: 'get',
      params
    })
  },

  /**
   * 获取配置详情
   * @param {number} id 配置ID
   * @returns 配置详情
   */
  get(id) {
    return request({
      url: `/api/configs/${id}`,
      method: 'get'
    })
  },

  /**
   * 新增配置
   * @param {Object} data 配置数据
   * @returns 操作结果
   */
  create(data) {
    return request({
      url: '/api/configs',
      method: 'post',
      data
    })
  },

  /**
   * 更新配置
   * @param {number} id 配置ID
   * @param {Object} data 配置数据
   * @returns 操作结果
   */
  update(id, data) {
    return request({
      url: `/api/configs/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除配置
   * @param {number} id 配置ID
   * @returns 操作结果
   */
  delete(id) {
    return request({
      url: `/api/configs/${id}`,
      method: 'delete'
    })
  }
}

export default configApi
