<template>
  <div class="graph-visualization graph-visualization-d3">
    <el-container>
      <!-- 左侧查询面板 -->
      <el-aside width="300px" class="query-panel">
        <div class="panel-header">
          <h3>图查询</h3>
        </div>

        <div class="section">
          <el-button type="primary" @click="loadSampleData">加载示例数据</el-button>
        </div>

        <!-- 使用 QueryEditor 组件 -->
        <QueryEditor
          v-model="queryStatement"
          :loading="queryLoading"
          :progress="queryProgress"
          :show-header="false"
          placeholder=""
          history-key="graphQueryHistoryD3"
          @execute="onExecuteQuery"
          @format="onFormatQuery"
          @clear="onClearQuery"
          @history-load="onHistoryLoad"
        />
      </el-aside>

      <!-- 主内容区：可视化展示 -->
      <el-main class="visualization-area">
        <!-- 可视化画布 -->
        <div class="viz-canvas-container" ref="vizContainer">
          <div class="graph-container">
            <D3Graph
              ref="d3GraphRef"
              :data="graphData"
              :width="vizWidth"
              :height="vizHeight"
              :layout-type="layoutType"
              @node-click="onNodeClick"
              @edge-click="onEdgeClick"
              class="viz-graph"
            />

            <!-- 画布工具栏 -->
            <div class="canvas-toolbar">
              <el-tag size="small" :type="graphTypeTagType" effect="dark" v-if="graphTypeLabel">
                {{ graphTypeLabel }}
              </el-tag>

              <div class="toolbar-divider"></div>

              <el-select v-model="layoutType" size="small" style="width: 110px" @change="onLayoutChange">
                <el-option label="力导向" value="force" />
                <el-option label="圆形" value="circular" />
                <el-option label="层次" value="hierarchical" />
                <el-option label="网格" value="grid" />
              </el-select>

              <div class="toolbar-divider"></div>

              <el-button-group size="small">
                <el-button size="small" @click="expandNeighbors" :disabled="!selectedElement" title="展开邻居">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button size="small" @click="collapseNeighbors" :disabled="!selectedElement" title="收起邻居">
                  <el-icon><Minus /></el-icon>
                </el-button>
                <el-button size="small" v-if="isFiltered" @click="resetFilter" title="恢复完整视图">
                  <el-icon><Close /></el-icon>
                </el-button>
              </el-button-group>

              <div class="toolbar-divider"></div>

              <el-button-group class="zoom-group">
                <el-button size="small" @click="zoomIn" title="放大">
                  <el-icon><ZoomIn /></el-icon>
                </el-button>
                <el-button size="small" @click="zoomOut" title="缩小">
                  <el-icon><ZoomOut /></el-icon>
                </el-button>
                <el-button size="small" @click="resetView" title="适应画布">
                  <el-icon><FullScreen /></el-icon>
                </el-button>
              </el-button-group>

              <div class="toolbar-divider"></div>

              <el-dropdown trigger="click" @command="handleExport">
                <el-button size="small">
                  导出 <el-icon><Download /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="png">导出 PNG</el-dropdown-item>
                    <el-dropdown-item command="svg">导出 SVG</el-dropdown-item>
                    <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <!-- 图例 -->
            <div class="canvas-legend" v-if="legendItems.nodes.length > 0 || legendItems.edges.length > 0">
              <div class="legend-title">图例</div>
              <div class="legend-items">
                <div class="legend-group" v-if="legendItems.nodes.length > 0">
                  <div class="legend-group-title">节点</div>
                  <div v-for="item in legendItems.nodes" :key="item.label" class="legend-item">
                    <span class="legend-node-dot" :style="{ background: item.color }"></span>
                    <span class="legend-label">{{ item.label }}</span>
                  </div>
                </div>
                <div class="legend-group" v-if="legendItems.edges.length > 0">
                  <div class="legend-group-title">边</div>
                  <div v-for="item in legendItems.edges" :key="item.label" class="legend-item">
                    <span class="legend-edge-line" :style="{ background: item.color, borderColor: item.color }"></span>
                    <span class="legend-label">{{ item.label }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 统计 -->
            <div class="canvas-stats">
              <div class="stat-item">
                <span class="stat-label">节点</span>
                <span class="stat-value">{{ graphData.nodes.length }}</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-label">边</span>
                <span class="stat-value">{{ graphData.edges.length }}</span>
              </div>
            </div>

            <!-- 节点/边详情浮动面板 -->
            <div v-if="detailDrawerVisible && selectedElement" class="detail-panel">
              <div class="detail-header">
                <h3>{{ selectedElement.type === 'node' ? '节点详情' : '边详情' }}</h3>
                <el-button
                  type="text"
                  size="small"
                  @click="closeDetailPanel"
                  class="close-btn"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>

              <div class="detail-content">
                <div class="detail-section">
                  <h4>基础信息</h4>
                  <el-descriptions :column="1" border size="small" class="compact-descriptions">
                    <el-descriptions-item label="ID">{{ selectedElement.id }}</el-descriptions-item>
                    <el-descriptions-item label="标签" v-if="selectedElement.type === 'node'">{{ selectedElement.label }}</el-descriptions-item>
                    <el-descriptions-item label="类型" v-if="selectedElement.type === 'edge'">{{ selectedElement.label }}</el-descriptions-item>
                  </el-descriptions>
                </div>

                <div class="detail-section" v-if="selectedElement.properties && Object.keys(selectedElement.properties).length > 0">
                  <h4>属性</h4>
                  <div class="property-list">
                    <div
                      v-for="(value, key) in selectedElement.properties"
                      :key="key"
                      class="property-item"
                    >
                      <span class="property-key">{{ key }}:</span>
                      <span class="property-value">{{ value }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import { graphApi } from '@/views/graphs/api/graph'
import D3Graph from './components/D3Graph.vue'
import QueryEditor from '@/components/QueryEditor/QueryEditor.vue'
import { ElMessage } from 'element-plus'
import {
  Close, ZoomIn, ZoomOut, FullScreen, Download, Plus, Minus
} from '@element-plus/icons-vue'

// 默认查询语句
const DEFAULT_QUERIES = {
  neo4j: 'MATCH p=(n)-[r]->() RETURN p LIMIT 10',
  nebula: 'FETCH PROP ON * LIMIT 10',
  janusgraph: 'g.V().limit(10)',
  janus: 'g.V().limit(10)'
}

// 状态管理
const graphsStore = useGraphsStore()

// 图类型信息（兼容 mock 的 databaseType 和真实 API 的 graphType）
const graphType = computed(() => {
  const raw = graphsStore.currentGraph?.graphType || graphsStore.currentGraph?.databaseType || ''
  return raw.toLowerCase()
})

// 布局类型
const layoutType = ref('force')

// 初始化查询语句（在 watch 之前初始化，避免访问顺序错误）
const graphTypeRaw = graphsStore.currentGraph?.graphType || graphsStore.currentGraph?.databaseType || ''
const queryStatement = ref(DEFAULT_QUERIES[graphTypeRaw.toLowerCase()] || '')
const queryLoading = ref(false)
const graphData = ref({
  nodes: [],
  edges: []
})

// 当图类型变化时，自动填充默认查询语句
watch(graphType, (val, oldVal) => {
  if (val) {
    const defaultQuery = DEFAULT_QUERIES[val] || ''
    // 如果当前查询语句为空，或是旧的默认查询语句，则更新为新的默认查询语句
    if (!queryStatement.value ||
        (oldVal && queryStatement.value === DEFAULT_QUERIES[oldVal])) {
      queryStatement.value = defaultQuery
    }
  }
}, { immediate: true })

// 当全局切换图时，清除旧数据并重置
watch(() => graphsStore.currentGraphId, (newId, oldId) => {
  if (newId !== oldId) {
    graphData.value = { nodes: [], edges: [] }
    queryStatement.value = DEFAULT_QUERIES[graphType.value] || ''
    detailDrawerVisible.value = false
    selectedElement.value = null
  }
})

const vizContainer = ref(null)
const vizWidth = ref(800)
const vizHeight = ref(600)
const d3GraphRef = ref(null)

const detailDrawerVisible = ref(false)
const selectedElement = ref(null)

// 扩展/折叠相关
const originalGraphData = ref(null)
const isFiltered = ref(false)

// 进度相关
const queryProgress = ref(0)
const estimatedTime = ref(0)

// 生命周期
onMounted(() => {
  updateVizDimensions()
  window.addEventListener('resize', updateVizDimensions)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateVizDimensions)
})

// 方法
const updateVizDimensions = () => {
  if (vizContainer.value) {
    const containerRect = vizContainer.value.getBoundingClientRect()
    vizWidth.value = containerRect.width
    vizHeight.value = containerRect.height
  }
}

// QueryEditor 事件处理
const onExecuteQuery = async (statement) => {
  if (!graphsStore.currentGraphId) {
    ElMessage.warning('请先选择图')
    return
  }

  queryLoading.value = true
  queryProgress.value = 0
  estimatedTime.value = 5 // 预估5秒完成

  let progressInterval
  try {
    // 模拟进度更新
    progressInterval = setInterval(() => {
      queryProgress.value += 0.1
      if (queryProgress.value >= 1) {
        clearInterval(progressInterval)
        queryProgress.value = 1
      }
    }, 100)

    // 调用真实API执行查询
    const response = await graphApi.queryGraph(graphsStore.currentGraphId, statement)

    // 根据API返回的数据结构转换格式
    const transformedData = transformApiResponseToGraphData(response)
    graphData.value = transformedData

    ElMessage.success('查询执行成功')
  } catch (error) {
    console.error('查询执行失败:', error)
    ElMessage.error('查询执行失败: ' + error.message)
  } finally {
    queryLoading.value = false
    queryProgress.value = 0
    if (progressInterval) clearInterval(progressInterval)
  }
}

const onFormatQuery = (statement) => {
  // 简单的格式化逻辑
  const formatted = statement
    .replace(/\s+/g, ' ')
    .replace(/;\s*/g, ';\n')
    .replace(/\b(MATCH|RETURN|WHERE|WITH|ORDER BY|LIMIT)\b/g, '\n$1')
    .trim()
  queryStatement.value = formatted
  ElMessage.success('查询已格式化')
}

const onClearQuery = () => {
  // 已由 v-model 清空
}

const onHistoryLoad = (historyItem) => {
  // 历史记录已加载到编辑器
}

const loadSampleData = () => {
  graphData.value = {
    nodes: [
      { id: 's1', label: 'Person', properties: { name: 'John', age: 32 } },
      { id: 's2', label: 'Person', properties: { name: 'Mary', age: 28 } },
      { id: 's3', label: 'Company', properties: { name: 'Acme Corp', industry: 'Manufacturing' } }
    ],
    edges: [
      { id: 'e1', source: 's1', target: 's2', label: 'KNOWS', properties: { since: '2018' } },
      { id: 'e2', source: 's1', target: 's3', label: 'WORKS_FOR', properties: { position: 'Manager' } }
    ]
  }
  ElMessage.success('已加载示例数据')
}

const onNodeClick = (node) => {
  selectedElement.value = {
    type: 'node',
    id: node.id,
    label: node.label,
    properties: node.properties
  }
  detailDrawerVisible.value = true
}

const onEdgeClick = (edge) => {
  selectedElement.value = {
    type: 'edge',
    id: edge.id,
    label: edge.label,
    properties: edge.properties,
    source: edge.source,
    target: edge.target
  }
  detailDrawerVisible.value = true
}

const closeDetailPanel = () => {
  detailDrawerVisible.value = false
  selectedElement.value = null
}

// 展开邻居：仅显示选中节点及其直接邻居
function expandNeighbors() {
  if (!selectedElement.value || selectedElement.value.type !== 'node') {
    ElMessage.warning('请先选择一个节点')
    return
  }

  // 保存原始数据（首次展开时）
  if (!isFiltered.value) {
    originalGraphData.value = {
      nodes: [...graphData.value.nodes],
      edges: [...graphData.value.edges]
    }
  }

  const nodeId = selectedElement.value.id

  // 找到所有与选中节点直接相连的边
  const connectedEdges = graphData.value.edges.filter(e => {
    const sourceId = typeof e.source === 'object' ? e.source.id : e.source
    const targetId = typeof e.target === 'object' ? e.target.id : e.target
    return sourceId === nodeId || targetId === nodeId
  })

  // 收集邻居节点 ID
  const neighborIds = new Set([nodeId])
  connectedEdges.forEach(e => {
    const sourceId = typeof e.source === 'object' ? e.source.id : e.source
    const targetId = typeof e.target === 'object' ? e.target.id : e.target
    neighborIds.add(sourceId)
    neighborIds.add(targetId)
  })

  // 过滤节点和边
  graphData.value = {
    nodes: graphData.value.nodes.filter(n => neighborIds.has(n.id)),
    edges: connectedEdges
  }

  isFiltered.value = true
  ElMessage.success(`展开节点邻居: ${neighborIds.size - 1} 个邻居`)
}

// 收起邻居：仅显示选中节点
function collapseNeighbors() {
  if (!selectedElement.value || selectedElement.value.type !== 'node') {
    ElMessage.warning('请先选择一个节点')
    return
  }

  if (!isFiltered.value) {
    originalGraphData.value = {
      nodes: [...graphData.value.nodes],
      edges: [...graphData.value.edges]
    }
  }

  const nodeId = selectedElement.value.id
  const node = graphData.value.nodes.find(n => n.id === nodeId)

  graphData.value = {
    nodes: node ? [node] : [],
    edges: []
  }

  isFiltered.value = true
  ElMessage.success('已收起邻居节点')
}

// 重置过滤：恢复完整图数据
function resetFilter() {
  if (originalGraphData.value) {
    graphData.value = {
      nodes: [...originalGraphData.value.nodes],
      edges: [...originalGraphData.value.edges]
    }
    originalGraphData.value = null
    isFiltered.value = false
    ElMessage.success('已恢复完整视图')
  }
}

// 图类型标签配置
const GRAPH_TYPE_TAG = {
  neo4j: { label: 'Neo4j', type: 'primary' },
  nebula: { label: 'Nebula', type: 'success' },
  janusgraph: { label: 'JanusGraph', type: 'warning' },
  janus: { label: 'JanusGraph', type: 'warning' }
}

const graphTypeLabel = computed(() => GRAPH_TYPE_TAG[graphType.value]?.label || graphType.value)
const graphTypeTagType = computed(() => GRAPH_TYPE_TAG[graphType.value]?.type || 'info')

// 节点颜色映射
const NODE_COLORS = {
  person: '#6366f1',
  people: '#6366f1',
  company: '#f59e0b',
  org: '#f59e0b',
  place: '#10b981',
  location: '#10b981'
}

function getNodeColor(label) {
  if (!label) return '#6366f1'
  const key = label.toLowerCase()
  return NODE_COLORS[key] || stringToColor(key)
}

// 边颜色映射
const EDGE_COLORS = {
  knows: '#94a3b8',
  works_at: '#f59e0b',
  works_for: '#f59e0b',
  located_in: '#10b981'
}

function getEdgeColor(label) {
  if (!label) return '#94a3b8'
  const key = label.toLowerCase()
  return EDGE_COLORS[key] || stringToColor(key, 60)
}

// 基于哈希的颜色生成
function stringToColor(str, saturation = 70) {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  const hue = Math.abs(hash % 360)
  return `hsl(${hue}, ${saturation}%, 55%)`
}

// 图例项
const legendItems = computed(() => {
  const nodes = []
  const edges = []
  const nodeLabels = new Set()
  const edgeLabels = new Set()

  ;(graphData.value.nodes || []).forEach(n => {
    const label = n.label || 'Unknown'
    if (!nodeLabels.has(label)) {
      nodeLabels.add(label)
      nodes.push({ label, color: getNodeColor(label) })
    }
  })

  ;(graphData.value.edges || []).forEach(e => {
    const label = e.label || 'Unknown'
    if (!edgeLabels.has(label)) {
      edgeLabels.add(label)
      edges.push({ label, color: getEdgeColor(label) })
    }
  })

  return { nodes, edges }
})

// 缩放控制
function zoomIn() {
  d3GraphRef.value?.zoomIn()
}

function zoomOut() {
  d3GraphRef.value?.zoomOut()
}

function resetView() {
  d3GraphRef.value?.resetView()
}

// 导出功能
function handleExport(format) {
  if (format === 'png') exportAsPNG()
  else if (format === 'svg') exportAsSVG()
  else if (format === 'json') exportAsJSON()
}

function exportAsPNG() {
  const svgEl = document.querySelector('.d3-graph-container svg')
  if (!svgEl) { ElMessage.warning('没有可导出的内容'); return }

  const svgData = new XMLSerializer().serializeToString(svgEl)
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const img = new Image()

  const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)

  img.onload = () => {
    canvas.width = svgEl.clientWidth * 2
    canvas.height = svgEl.clientHeight * 2
    ctx.scale(2, 2)
    ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--el-bg-color').trim() || '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0)
    URL.revokeObjectURL(url)

    const link = document.createElement('a')
    link.download = 'graph-export.png'
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('已导出为 PNG')
  }

  img.src = url
}

function exportAsSVG() {
  const svgEl = document.querySelector('.d3-graph-container svg')
  if (!svgEl) { ElMessage.warning('没有可导出的内容'); return }

  const svgData = new XMLSerializer().serializeToString(svgEl)
  const blob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.download = 'graph-export.svg'
  link.href = url
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出为 SVG')
}

function exportAsJSON() {
  const data = graphData.value
  if (!data.nodes || data.nodes.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.download = 'graph-export.json'
  link.href = url
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出为 JSON')
}

const onLayoutChange = (val) => {
  layoutType.value = val
}

const toggleFullscreen = () => {
  // TODO: 全屏切换逻辑
  ElMessage.info('全屏功能开发中...')
}

/**
 * 将API返回的数据转换为D3图组件需要的格式
 * @param {Object} apiResponse - API返回的数据
 * @returns {Object} 转换后的图数据
 */
const transformApiResponseToGraphData = (apiResponse) => {
  // API可能返回不同的数据结构，需要兼容处理
  const rawData = apiResponse.data || apiResponse || []

  // 优先处理 vertices/edges 格式（API 实际返回 startUid/endUid）
  if (rawData.vertices && rawData.edges) {
    return {
      nodes: rawData.vertices.map(v => ({
        id: v.uid || v.id,
        label: v.label,
        properties: v.properties || {}
      })),
      edges: rawData.edges.map(e => ({
        id: e.uid || e.id,
        source: e.startUid || e.sourceUid || e.source,
        target: e.endUid || e.targetUid || e.target,
        label: e.label,
        properties: e.properties || {}
      }))
    }
  }

  // 如果返回的是标准图数据结构，直接使用
  if (rawData.nodes && rawData.edges) {
    return {
      nodes: rawData.nodes || [],
      edges: rawData.edges || []
    }
  }

  // 如果返回的是数组，尝试解析为图数据
  if (Array.isArray(rawData)) {
    const nodes = []
    const edges = []
    const nodeMap = new Map()
    const edgeMap = new Map()

    // 遍历结果，提取节点和边
    rawData.forEach((item, index) => {
      // 处理不同的返回格式
      if (item && typeof item === 'object') {
        // 如果是节点
        if (item.uid && item.label) {
          const nodeId = item.uid
          if (!nodeMap.has(nodeId)) {
            nodeMap.set(nodeId, {
              id: nodeId,
              label: item.label,
              properties: item.properties || {}
            })
          }
        }

        // 如果是边（根据实际API返回的边格式调整）
        if (item.sourceUid && item.targetUid && item.label) {
          const edgeId = `${item.sourceUid}_${item.label}_${item.targetUid}`
          if (!edgeMap.has(edgeId)) {
            edgeMap.set(edgeId, {
              id: edgeId,
              source: item.sourceUid,
              target: item.targetUid,
              label: item.label,
              properties: item.properties || {}
            })
          }
        }
      }
    })

    // 如果没有解析出数据，返回默认结构
    if (nodeMap.size === 0 && edgeMap.size === 0) {
      return {
        nodes: [],
        edges: []
      }
    }

    return {
      nodes: Array.from(nodeMap.values()),
      edges: Array.from(edgeMap.values())
    }
  }

  // 默认返回空数据
  return {
    nodes: [],
    edges: []
  }
}
</script>

<style scoped>
.graph-visualization {
  height: calc(100vh - 64px); /* 减去顶部导航栏高度 */
  overflow: hidden;
  display: flex;
}

.graph-visualization :deep(.el-container) {
  height: 100%;
  display: flex;
}

.graph-visualization :deep(.el-aside) {
  height: 100%;
  overflow-y: auto;
}

:deep(.el-main) {
  padding: 0;
  height: 100%;
}

.query-panel {
  background: var(--el-fill-color-light);
  border-right: 1px solid var(--el-border-color-light);
  padding: 16px;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section {
  margin-bottom: 24px;
}

.visualization-area {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.viz-canvas-container {
  flex: 1;
  padding: 0;
  overflow: hidden;
  background-color: var(--el-bg-color);
  background-image:
    linear-gradient(var(--el-border-color-lighter) 1px, transparent 1px),
    linear-gradient(90deg, var(--el-border-color-lighter) 1px, transparent 1px);
  background-size: 20px 20px;
  position: relative;
}

.graph-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.viz-graph {
  width: 100%;
  height: 100%;
}

/* 详情浮动面板样式 */
.detail-panel {
  position: absolute;
  top: 20px;
  right: 12px;
  width: 180px;
  max-height: calc(100% - 80px);
  background: var(--el-bg-color-overlay);
  border-radius: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
}

.detail-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.close-btn {
  padding: 2px;
  color: var(--el-text-color-secondary);
  transition: color 0.2s ease;
}

.close-btn:hover {
  color: var(--el-color-danger);
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px 16px;
}

.detail-section {
  margin-top: 16px;
}

.detail-section:first-child {
  margin-top: 0;
}

.detail-section h4 {
  margin: 0 0 6px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.property-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 10px;
  background: var(--el-fill-color-light);
  border-radius: 3px;
  border-left: 3px solid var(--el-color-primary);
}

.property-key {
  font-weight: 500;
  color: var(--el-text-color-primary);
  font-size: 12px;
}

.property-value {
  color: var(--el-text-color-regular);
  max-width: 90px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 紧凑型描述列表样式 */
.compact-descriptions :deep(.el-descriptions__label) {
  font-size: 12px !important;
  padding: 6px 8px !important;
}

.compact-descriptions :deep(.el-descriptions__content) {
  font-size: 12px !important;
  padding: 6px 8px !important;
}

.compact-descriptions :deep(.el-descriptions__cell) {
  padding: 0 !important;
}

.compact-descriptions :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  padding: 6px 8px !important;
}

.compact-descriptions :deep(.el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
  padding: 6px 8px !important;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .query-panel {
    width: 280px;
  }
}

/* 画布工具栏 */
.canvas-toolbar {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
  flex-wrap: nowrap;
  max-width: calc(100% - 24px);
  overflow-x: auto;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--el-border-color-light);
}

.zoom-group .el-button {
  padding: 5px 8px;
}

/* 图例 */
.canvas-legend {
  position: absolute;
  bottom: 12px;
  left: 12px;
  max-width: calc(100% - 120px);
  padding: 10px 14px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: min(60%, 300px);
  overflow-y: auto;
  overflow-x: hidden;
}

.legend-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.legend-group {
  margin-bottom: 8px;
}

.legend-group:last-child {
  margin-bottom: 0;
}

.legend-group-title {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}

.legend-node-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-edge-line {
  width: 18px;
  height: 3px;
  border-radius: 2px;
  flex-shrink: 0;
  position: relative;
}

.legend-edge-line::after {
  content: '';
  position: absolute;
  right: -5px;
  top: -3px;
  width: 0;
  height: 0;
  border-left: 6px solid;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left-color: inherit;
}

.legend-label {
  font-size: 11px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

/* 统计 */
.canvas-stats {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-width: calc(100% - 24px);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.stat-divider {
  width: 1px;
  height: 16px;
  background: var(--el-border-color-light);
}

/* 暗色模式覆盖 */
.dark .canvas-toolbar,
.dark .canvas-legend,
.dark .canvas-stats {
  border-color: var(--el-border-color-light);
}

@media (max-width: 768px) {
  .query-panel {
    width: 100%;
    height: auto;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .graph-visualization {
    flex-direction: column;
  }
}
</style>
