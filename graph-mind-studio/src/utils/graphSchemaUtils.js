/**
 * 图模式相关工具函数
 */

import graphApi from '@/api/graph'

/**
 * 获取图的节点定义列表
 * @param {number} graphId - 图ID
 * @returns {Promise<Array>} 节点定义列表
 */
export const fetchNodeDefs = async (graphId) => {
  if (!graphId) {
    throw new Error('Graph ID is required')
  }
  
  try {
    const res = await graphApi.getNodeDefs(graphId)
    return res.data || []
  } catch (e) {
    console.error('获取点定义列表失败:', e)
    throw new Error('获取点定义列表失败: ' + (e.message || '未知错误'))
  }
}

/**
 * 获取图的边定义列表
 * @param {number} graphId - 图ID
 * @returns {Promise<Array>} 边定义列表
 */
export const fetchEdgeDefs = async (graphId) => {
  if (!graphId) {
    throw new Error('Graph ID is required')
  }
  
  try {
    const res = await graphApi.getEdgeDefs(graphId)
    return res.data || []
  } catch (e) {
    console.error('获取边定义列表失败:', e)
    throw new Error('获取边定义列表失败: ' + (e.message || '未知错误'))
  }
}

/**
 * 获取图的已发布模式定义
 * @param {number} graphId - 图ID
 * @returns {Promise<Object>} 图模式定义
 */
export const fetchGraphSchema = async (graphId) => {
  if (!graphId) {
    throw new Error('Graph ID is required')
  }

  try {
    const res = await graphApi.getPublishedSchema(graphId)
    return res.data || { entities: [], relations: [] }
  } catch (error) {
    console.error('获取图schema失败:', error)
    throw new Error('获取图schema失败: ' + (error.message || '未知错误'))
  }
}

export default {
  fetchNodeDefs,
  fetchEdgeDefs,
  fetchGraphSchema
}