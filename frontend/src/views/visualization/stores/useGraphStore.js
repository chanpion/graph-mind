import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 图可视化状态 Store
 * 仅管理可视化相关状态（布局、高亮、展开等），不包含图列表/CRUD
 */
export const useGraphStore = defineStore('graph-viz', () => {
  // 高亮的路径（节点ID数组）
  const highlightedPath = ref([])

  // 已展开的子图ID集合
  const expandedSubgraphs = ref(new Set())

  // 选中的节点ID
  const selectedNode = ref(null)

  // 图布局算法
  const layoutAlgorithm = ref('force-directed')

  // 计算属性
  const hasHighlightedPath = computed(() => highlightedPath.value.length > 0)
  const expandedSubgraphCount = computed(() => expandedSubgraphs.value.size)

  /** 设置高亮路径 */
  function setHighlightedPath(path) {
    highlightedPath.value = Array.isArray(path) ? path : []
  }

  /** 清除高亮路径 */
  function clearHighlightedPath() {
    highlightedPath.value = []
  }

  /** 展开子图 */
  function expandSubgraph(subgraphId) {
    expandedSubgraphs.value.add(subgraphId)
  }

  /** 折叠子图 */
  function collapseSubgraph(subgraphId) {
    expandedSubgraphs.value.delete(subgraphId)
  }

  /** 切换子图展开状态 */
  function toggleSubgraph(subgraphId) {
    if (expandedSubgraphs.value.has(subgraphId)) {
      collapseSubgraph(subgraphId)
    } else {
      expandSubgraph(subgraphId)
    }
  }

  /** 设置选中的节点 */
  function setSelectedNode(nodeId) {
    selectedNode.value = nodeId
  }

  /** 清除选中的节点 */
  function clearSelectedNode() {
    selectedNode.value = null
  }

  /** 设置布局算法 */
  function setLayoutAlgorithm(algorithm) {
    layoutAlgorithm.value = algorithm
  }

  /** 重置所有可视化状态 */
  function resetVisualizationState() {
    highlightedPath.value = []
    expandedSubgraphs.value.clear()
    selectedNode.value = null
    layoutAlgorithm.value = 'force-directed'
  }

  return {
    highlightedPath,
    expandedSubgraphs,
    selectedNode,
    layoutAlgorithm,
    hasHighlightedPath,
    expandedSubgraphCount,
    setHighlightedPath,
    clearHighlightedPath,
    expandSubgraph,
    collapseSubgraph,
    toggleSubgraph,
    setSelectedNode,
    clearSelectedNode,
    setLayoutAlgorithm,
    resetVisualizationState
  }
})
