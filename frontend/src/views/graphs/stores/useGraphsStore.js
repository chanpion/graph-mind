import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { graphApi } from '../api/graph'

/**
 * 图管理 Store
 * 负责图列表元数据、当前选中图、CRUD 操作
 */
export const useGraphsStore = defineStore('graphs', () => {
  // ====== 状态 ======
  const graphs = ref([])
  const currentGraph = ref(null)
  const selectedConnectionId = ref('')
  const loading = ref(false)
  const error = ref(null)

  // ====== 计算属性 ======
  const hasSelection = computed(() => !!selectedConnectionId.value && !!currentGraph.value)
  const currentGraphId = computed(() => currentGraph.value?.id || '')
  const currentGraphName = computed(() => currentGraph.value?.name || currentGraph.value?.graphName || '')

  // ====== 操作 ======

  /** 设置当前图 */
  function setCurrentGraph(graph) {
    currentGraph.value = graph
  }

  /** 设置选中连接 */
  function setSelectedConnection(connectionId) {
    selectedConnectionId.value = connectionId
  }

  /** 加载图列表 */
  async function fetchGraphs(params) {
    loading.value = true
    error.value = null
    try {
      const res = await graphApi.list(params)
      const data = res?.data || res
      graphs.value = Array.isArray(data) ? data : data?.records || data?.list || []
      return graphs.value
    } catch (err) {
      error.value = err.message || '加载图列表失败'
      console.error('加载图列表失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** 根据连接ID加载图列表 */
  async function fetchGraphsByConnection(connectionId, params) {
    if (!connectionId) {
      graphs.value = []
      return []
    }
    loading.value = true
    error.value = null
    try {
      const res = await graphApi.listByConnection(connectionId, params)
      const data = res?.data || res
      graphs.value = Array.isArray(data) ? data : data?.records || data?.list || []
      return graphs.value
    } catch (err) {
      error.value = err.message || '加载图列表失败'
      console.error('加载图列表失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** 创建图 */
  async function createGraph(data) {
    error.value = null
    try {
      const res = await graphApi.create(data)
      return res
    } catch (err) {
      error.value = err.message || '创建图失败'
      throw err
    }
  }

  /** 更新图 */
  async function updateGraph(id, data) {
    error.value = null
    try {
      const res = await graphApi.update(id, data)
      return res
    } catch (err) {
      error.value = err.message || '更新图失败'
      throw err
    }
  }

  /** 删除图 */
  async function deleteGraph(id) {
    error.value = null
    try {
      const res = await graphApi.delete(id)
      return res
    } catch (err) {
      error.value = err.message || '删除图失败'
      throw err
    }
  }

  /** 清空数据 */
  function clearData() {
    graphs.value = []
    currentGraph.value = null
    error.value = null
  }

  return {
    // 状态
    graphs,
    currentGraph,
    selectedConnectionId,
    loading,
    error,
    // 计算属性
    hasSelection,
    currentGraphId,
    currentGraphName,
    // 操作
    setCurrentGraph,
    setSelectedConnection,
    fetchGraphs,
    fetchGraphsByConnection,
    createGraph,
    updateGraph,
    deleteGraph,
    clearData
  }
})
