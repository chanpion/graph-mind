/**
 * 图页面相关公共工具函数
 */

import { ElMessage } from 'element-plus'
import graphApi from '@/api/graph'
import { processEdgesData } from '@/utils/graphUtils.js'

/**
 * 格式化属性数据用于表格展示
 * @param {Object} properties 属性对象
 * @returns {Array} 格式化后的属性数组
 */
export const formatProperties = (properties) => {
  if (!properties) return []
  return Object.keys(properties).map(key => ({
    name: key,
    value: properties[key]
  }))
}

/**
 * 获取节点类型列表
 * @param {number} graphId 图ID
 * @returns {Promise<Array>} 节点类型列表
 */
export const fetchNodeTypes = async (graphId) => {
  try {
    const res = await graphApi.getNodeDefs(graphId)
    return res.data || []
  } catch (e) {
    console.error('获取点类型列表失败:', e)
    throw new Error('获取点类型列表失败: ' + (e.message || '未知错误'))
  }
}

/**
 * 获取边类型列表
 * @param {number} graphId 图ID
 * @returns {Promise<Array>} 边类型列表
 */
export const fetchEdgeTypes = async (graphId) => {
  try {
    const res = await graphApi.getEdgeDefs(graphId)
    return res.data || []
  } catch (e) {
    console.error('获取边类型列表失败:', e)
    throw new Error('获取边类型列表失败: ' + (e.message || '未知错误'))
  }
}

/**
 * 处理边数据，确保source和target引用节点对象
 * @param {Array} edges 边数据数组
 * @param {Array} nodes 节点数据数组
 * @returns {Array} 处理后的边数据
 */
export const processEdges = (edges, nodes) => {
  return processEdgesData(edges, nodes)
}

export default {
  formatProperties,
  fetchNodeTypes,
  fetchEdgeTypes,
  processEdges
}