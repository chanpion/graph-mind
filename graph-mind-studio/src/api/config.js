import request from '@/utils/request.js'

/**
 * 获取配置列表
 * @param {Object} params 查询参数
 * @returns 配置列表
 */
export function getConfigList(params) {
  return request({
    url: '/api/configs',
    method: 'get',
    params
  })
}

/**
 * 获取配置详情
 * @param {number} id 配置ID
 * @returns 配置详情
 */
export function getConfig(id) {
  return request({
    url: `/api/configs/${id}`,
    method: 'get'
  })
}

/**
 * 新增配置
 * @param {Object} data 配置数据
 * @returns 操作结果
 */
export function addConfig(data) {
  return request({
    url: '/api/configs',
    method: 'post',
    data
  })
}

/**
 * 更新配置
 * @param {number} id 配置ID
 * @param {Object} data 配置数据
 * @returns 操作结果
 */
export function updateConfig(id, data) {
  return request({
    url: `/api/configs/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除配置
 * @param {number} id 配置ID
 * @returns 操作结果
 */
export function deleteConfig(id) {
  return request({
    url: `/api/configs/${id}`,
    method: 'delete'
  })
}