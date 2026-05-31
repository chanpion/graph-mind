<template>
  <div class="graph-modeling-view">
    <!-- 顶部工具栏 -->
    <div class="graph-toolbar">
      <div class="toolbar-left">
        <span class="layout-label">力导图布局</span>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-button size="small" @click="zoomIn" title="放大">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
          <el-button size="small" @click="zoomOut" title="缩小">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
          <el-button size="small" @click="resetZoom" title="重置">
            <el-icon><RefreshLeft /></el-icon>
          </el-button>
        </el-button-group>
        <el-button size="small" @click="toggleLegend" title="图例">
          <el-icon><List /></el-icon>
          图例
        </el-button>
        <el-button size="small" @click="fitToScreen" title="适应屏幕">
          <el-icon><FullScreen /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 图可视化区域 -->
    <div class="graph-container" ref="containerRef">
      <svg
        ref="svgRef"
        class="graph-svg"
        @mousedown="handleMouseDown"
        @mousemove="handleMouseMove"
        @mouseup="handleMouseUp"
        @wheel.prevent="handleWheel"
      >
        <g ref="zoomGroupRef" class="zoom-group">
          <!-- 边 -->
          <g class="edges">
            <g
              v-for="edge in filteredEdges"
              :key="edge.id"
              class="edge"
              :class="{ highlighted: isHighlighted(edge), 'is-loop': isSelfLoop(edge) }"
            >
              <!-- 自环边 (同节点类型) 用曲线渲染 -->
              <template v-if="isSelfLoop(edge)">
                <path
                  :d="getSelfLoopPath(edge)"
                  :stroke="getEdgeColor(edge)"
                  stroke-width="2"
                  fill="none"
                  marker-end="url(#arrowhead-loop)"
                  class="edge-line"
                  @click="selectEdge(edge)"
                />
                <path
                  :d="getSelfLoopPath(edge)"
                  stroke="transparent"
                  stroke-width="12"
                  fill="none"
                  @click="selectEdge(edge)"
                  class="edge-hit-area"
                />
                <text
                  :x="getSelfLoopLabelX(edge)"
                  :y="getSelfLoopLabelY(edge)"
                  text-anchor="middle"
                  font-size="11"
                  fill="var(--el-text-color-secondary)"
                  class="edge-label"
                >
                  {{ edge.label }}
                </text>
              </template>
              <!-- 普通边用线段渲染 -->
              <template v-else>
                <line
                  v-if="isHighlighted(edge)"
                  :x1="edge.source.x"
                  :y1="edge.source.y"
                  :x2="getEdgeTarget(edge).x"
                  :y2="getEdgeTarget(edge).y"
                  stroke="var(--el-color-primary)"
                  stroke-width="3"
                  stroke-opacity="0.6"
                  stroke-dasharray="10, 5"
                  class="edge-flow"
                />
                <line
                  :x1="edge.source.x"
                  :y1="edge.source.y"
                  :x2="edge.target.x"
                  :y2="edge.target.y"
                  stroke="transparent"
                  stroke-width="12"
                  @click="selectEdge(edge)"
                  class="edge-hit-area"
                />
                <line
                  :x1="edge.source.x"
                  :y1="edge.source.y"
                  :x2="getEdgeTarget(edge).x"
                  :y2="getEdgeTarget(edge).y"
                  :stroke="getEdgeColor(edge)"
                  stroke-width="2"
                  marker-end="url(#arrowhead)"
                  class="edge-line"
                />
                <text
                  :x="(edge.source.x + edge.target.x) / 2"
                  :y="(edge.source.y + edge.target.y) / 2 - 5"
                  text-anchor="middle"
                  font-size="11"
                  fill="var(--el-text-color-secondary)"
                  class="edge-label"
                >
                  {{ edge.label }}
                </text>
              </template>
            </g>
          </g>

          <!-- 节点 -->
          <g class="nodes">
            <g
              v-for="node in filteredNodes"
              :key="node.id"
              class="node"
              :class="{ selected: isNodeSelected(node.id), highlighted: isHighlighted(node), dragging: draggedNode?.id === node.id }"
              :transform="`translate(${node.x}, ${node.y})`"
              @mousedown="startDrag(node, $event)"
              @click="selectNode(node, $event)"
            >
              <circle
                v-if="draggedNode?.id === node.id"
                :r="getNodeRadius(node) + 7"
                fill="rgba(var(--el-color-primary-rgb), 0.2)"
                class="drag-halo"
              />
              <circle
                v-if="isNodeSelected(node.id)"
                :r="getNodeRadius(node) + 5"
                fill="rgba(var(--el-color-primary-rgb), 0.3)"
                class="select-halo"
              />
              <circle
                :r="getNodeRadius(node)"
                :fill="getNodeColor(node)"
                stroke="var(--el-border-color)"
                stroke-width="2"
                class="node-circle"
              />
              <text
                text-anchor="middle"
                dy="3"
                fill="var(--el-color-white)"
                font-size="11"
                font-weight="600"
                class="node-label"
              >
                {{ (node.label || node.name).substring(0, 6) }}
              </text>
              <text
                :dy="getNodeRadius(node) + 16"
                text-anchor="middle"
                fill="var(--el-text-color-secondary)"
                font-size="11"
                class="node-type"
              >
                {{ node.label || node.name }}
              </text>
              <circle
                v-if="node.propertyCount > 0"
                :cx="getNodeRadius(node) - 10"
                :cy="-getNodeRadius(node) + 10"
                r="12"
                fill="var(--el-color-success)"
                class="property-badge"
              />
              <text
                v-if="node.propertyCount > 0"
                :x="getNodeRadius(node) - 10"
                :y="-getNodeRadius(node) + 13"
                text-anchor="middle"
                fill="white"
                font-size="10"
                font-weight="600"
              >
                {{ node.propertyCount }}
              </text>
            </g>
          </g>
        </g>

        <defs>
          <marker
            id="arrowhead"
            markerWidth="12"
            markerHeight="8"
            refX="10"
            refY="4"
            orient="auto"
            markerUnits="userSpaceOnUse"
          >
            <polygon points="0 0, 12 4, 0 8" fill="var(--el-text-color-secondary)" />
          </marker>
          <marker
            id="arrowhead-loop"
            markerWidth="12"
            markerHeight="8"
            refX="10"
            refY="4"
            orient="auto"
            markerUnits="userSpaceOnUse"
          >
            <polygon points="0 0, 12 4, 0 8" fill="var(--el-text-color-secondary)" />
          </marker>
        </defs>
      </svg>

      <!-- 图例面板 -->
      <div v-if="showLegend" class="legend-panel">
        <div class="legend-header">
          <span>图例</span>
          <el-button link @click="toggleLegend">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="legend-content">
          <div class="legend-section">
            <div class="legend-title">点类型 ({{ nodes.length }})</div>
            <div
              v-for="node in nodes"
              :key="node.id"
              class="legend-item"
              @click="selectNode(node)"
            >
              <div class="legend-color" :style="{ backgroundColor: getNodeColor(node) }" />
              <span class="legend-label">{{ node.label || node.name }}</span>
              <el-tag size="small" type="info">{{ node.propertyCount }} 属性</el-tag>
            </div>
          </div>
          <div class="legend-section">
            <div class="legend-title">边类型 ({{ edges.length }})</div>
            <div v-for="edge in edges" :key="edge.id" class="legend-item">
              <div class="legend-edge" />
              <span class="legend-label">{{ edge.label || edge.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 属性详情面板 -->
      <div v-if="drawerVisible" class="detail-panel">
        <div class="detail-panel-header">
          <span>{{ drawerTitle }}</span>
          <el-button link @click="closeDetail">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="detail-panel-body">
          <template v-if="selectedNode">
            <div class="detail-item">
              <span class="detail-label">类型:</span>
              <el-tag type="success" size="small">点类型</el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">属性数量:</span>
              <span>{{ mergedNodeProperties.length }}</span>
            </div>
            <div v-if="selectedNode.description" class="detail-item">
              <span class="detail-label">描述:</span>
              <span>{{ selectedNode.description }}</span>
            </div>
            <div class="properties-section">
              <div class="section-title">属性列表</div>
              <div class="property-header">
                <span class="prop-col-code">标识</span>
                <span class="prop-col-name">名称</span>
                <span class="prop-col-type">类型</span>
                <span class="prop-col-flags">标记</span>
              </div>
              <div class="property-item" v-for="prop in mergedNodeProperties" :key="prop.code || prop.name">
                <span class="prop-col-code prop-code">{{ prop.code }}</span>
                <span class="prop-col-name prop-name">{{ prop.name }}</span>
                <span class="prop-col-type"><el-tag size="small" type="info">{{ prop.type || prop.dataType }}</el-tag></span>
                <span class="prop-col-flags">
                  <el-tag v-if="prop.indexed" size="small" type="success">索引</el-tag>
                </span>
              </div>
            </div>
          </template>
          <template v-if="selectedEdge">
            <div class="detail-item">
              <span class="detail-label">类型:</span>
              <el-tag type="warning" size="small">边类型</el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">起点:</span>
              <span>{{ selectedEdge.source?.label || selectedEdge.source?.name }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">终点:</span>
              <span>{{ selectedEdge.target?.label || selectedEdge.target?.name }}</span>
            </div>
            <div v-if="selectedEdge.description" class="detail-item">
              <span class="detail-label">描述:</span>
              <span>{{ selectedEdge.description }}</span>
            </div>
            <div class="properties-section">
              <div class="section-title">属性列表</div>
              <div class="property-header">
                <span class="prop-col-code">标识</span>
                <span class="prop-col-name">名称</span>
                <span class="prop-col-type">类型</span>
                <span class="prop-col-flags">标记</span>
              </div>
              <div class="property-item" v-for="prop in mergedEdgeProperties" :key="prop.code || prop.name">
                <span class="prop-col-code prop-code">{{ prop.code }}</span>
                <span class="prop-col-name prop-name">{{ prop.name }}</span>
                <span class="prop-col-type"><el-tag size="small" type="info">{{ prop.type || prop.dataType }}</el-tag></span>
                <span class="prop-col-flags">
                  <el-tag v-if="prop.indexed" size="small" type="success">索引</el-tag>
                </span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ZoomIn, ZoomOut, RefreshLeft, List, FullScreen, Close } from '@element-plus/icons-vue'
import * as d3 from 'd3'

const props = defineProps({
  nodeDefs: { type: Array, default: () => [] },
  edgeDefs: { type: Array, default: () => [] }
})

const containerRef = ref(null)
const svgRef = ref(null)
const zoomGroupRef = ref(null)

const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)
const selectedEdge = ref(null)
const drawerVisible = ref(false)
const drawerTitle = ref('')

const zoom = ref(1)
const showLegend = ref(false)

const isDragging = ref(false)
const isPanning = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const panStart = ref({ x: 0, y: 0 })
const draggedNode = ref(null)
const panOffset = ref({ x: 0, y: 0 })
const selectedNodes = ref(new Set())

const filteredNodes = computed(() => nodes.value)
const filteredEdges = computed(() => edges.value)

const mergedNodeProperties = computed(() => {
  if (!selectedNode.value) return []
  return (selectedNode.value.properties || []).map(p => ({ ...p, isBuiltIn: false }))
})

const mergedEdgeProperties = computed(() => {
  if (!selectedEdge.value) return []
  return (selectedEdge.value.properties || []).map(p => ({ ...p, isBuiltIn: false }))
})

const colorMap = [
  'var(--el-color-primary)',
  'var(--el-color-success)',
  'var(--el-color-warning)',
  'var(--el-color-danger)',
  '#8B5CF6',
  '#EC4899',
  '#06B6D4',
  '#84CC16'
]

function getNodeColor(node) {
  const index = nodes.value.findIndex(n => n.id === node.id)
  return colorMap[index % colorMap.length]
}

function adjustColorBrightness(hexColor, percent) {
  const num = parseInt(hexColor.replace('#', ''), 16)
  const amt = Math.round(2.55 * percent)
  const R = Math.min(255, Math.max(0, (num >> 16) + amt))
  const G = Math.min(255, Math.max(0, (num >> 8 & 0x00FF) + amt))
  const B = Math.min(255, Math.max(0, (num & 0x0000FF) + amt))
  return '#' + (0x1000000 + R * 0x10000 + G * 0x100 + B).toString(16).slice(1)
}

function getEdgeColor() {
  return 'var(--el-text-color-secondary)'
}

function getNodeRadius(node) {
  return 24 + Math.min(3, (node.propertyCount || 0) * 0.3)
}

function isSelfLoop(edge) {
  return edge.source && edge.target && (edge.source.id === edge.target.id || edge.source === edge.target)
}

function getSelfLoopPath(edge) {
  const cx = edge.source.x
  const cy = edge.source.y
  const r = getNodeRadius(edge.source)
  const loopRadius = 60
  const startX = cx + r
  const startY = cy
  const endX = cx - r
  const endY = cy
  const cp1X = cx + r + loopRadius
  const cp1Y = cy - loopRadius - 20
  const cp2X = cx - r - loopRadius
  const cp2Y = cy - loopRadius - 20
  return `M ${startX} ${startY} C ${cp1X} ${cp1Y}, ${cp2X} ${cp2Y}, ${endX} ${endY}`
}

function getSelfLoopLabelX(edge) {
  return edge.source.x
}

function getSelfLoopLabelY(edge) {
  return edge.source.y - getNodeRadius(edge.source) - 75
}

// 计算边的终点（在目标节点边缘处截断，露出箭头）
function getEdgeTarget(edge) {
  const s = edge.source, t = edge.target
  if (!s || !t) return { x: 0, y: 0 }
  const dx = t.x - s.x
  const dy = t.y - s.y
  const dist = Math.sqrt(dx * dx + dy * dy)
  if (dist === 0) return { x: t.x, y: t.y }
  const radius = getNodeRadius(t)
  const ratio = (dist - radius - 2) / dist
  return { x: s.x + dx * ratio, y: s.y + dy * ratio }
}

function isHighlighted(item) {
  if (selectedNodes.value.size === 0 && !selectedNode.value) return false
  if (item.id && selectedNodes.value.has(item.id)) return true
  if (item.source?.id && selectedNodes.value.has(item.source.id)) return true
  if (item.target?.id && selectedNodes.value.has(item.target.id)) return true
  if (selectedNode.value) {
    if (item.id === selectedNode.value.id) return true
    if (item.source?.id === selectedNode.value.id) return true
    if (item.target?.id === selectedNode.value.id) return true
  }
  return false
}

function isNodeSelected(nodeId) {
  return selectedNodes.value.has(nodeId) || (selectedNode.value && selectedNode.value.id === nodeId)
}

function selectNode(node, event) {
  if (event?.shiftKey) {
    if (selectedNodes.value.has(node.id)) {
      selectedNodes.value.delete(node.id)
      if (selectedNodes.value.size === 0) selectedNode.value = null
    } else {
      selectedNodes.value.add(node.id)
      selectedNode.value = node
    }
  } else {
    selectedNodes.value.clear()
    selectedNode.value = node
  }
  selectedEdge.value = null
  drawerTitle.value = node.label || node.name
  drawerVisible.value = true
}

function selectEdge(edge) {
  selectedEdge.value = edge
  selectedNode.value = null
  drawerTitle.value = edge.label || edge.name
  drawerVisible.value = true
}

function closeDetail() {
  drawerVisible.value = false
  selectedNode.value = null
  selectedEdge.value = null
  selectedNodes.value.clear()
}

function transformData() {
  // Map nodeDefs to D3 nodes, use numeric id for edge lookup
  nodes.value = (props.nodeDefs || []).map(vt => ({
    id: vt.id,
    label: vt.label || vt.name,
    name: vt.name,
    description: vt.description,
    properties: vt.properties || [],
    propertyCount: vt.properties?.length || 0,
    x: 0, y: 0
  }))

  // Map edgeDefs to D3 edges using from/to IDs (use == to handle String/Number type mismatch)
  edges.value = (props.edgeDefs || []).map(et => {
    // 优先按 node.id 匹配，兼容按 node.label 匹配
    let sourceNode = nodes.value.find(n => n.id == et.from)
    if (!sourceNode) sourceNode = nodes.value.find(n => n.label == et.from)
    let targetNode = nodes.value.find(n => n.id == et.to)
    if (!targetNode) targetNode = nodes.value.find(n => n.label == et.to)
    return {
      id: et.id,
      label: et.label || et.name,
      name: et.name,
      description: et.description,
      properties: et.properties || [],
      source: sourceNode,
      target: targetNode
    }
  }).filter(e => e.source && e.target) // only valid edges

  if (nodes.value.length > 0) applyLayout()
}

function applyLayout() {
  const width = containerRef.value?.clientWidth || 800
  const height = containerRef.value?.clientHeight || 600
  applyForceLayout(width, height)
}

function applyForceLayout(width, height) {
  const simulation = d3.forceSimulation(nodes.value)
    .force('link', d3.forceLink(edges.value).id(d => d.id).distance(120))
    .force('charge', d3.forceManyBody().strength(-250))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collide', d3.forceCollide().radius(50))

  simulation.on('tick', () => { /* Vue reactivity handles update */ })
  setTimeout(() => { simulation.stop(); nextTick(() => updateZoom()) }, 800)
}

function zoomIn() { zoom.value = Math.min(zoom.value + 0.1, 3); updateZoom() }
function zoomOut() { zoom.value = Math.max(zoom.value - 0.1, 0.3); updateZoom() }
function resetZoom() { zoom.value = 1; panOffset.value = { x: 0, y: 0 }; updateZoom() }
function fitToScreen() { resetZoom(); applyLayout() }

function updateZoom() {
  if (zoomGroupRef.value) {
    zoomGroupRef.value.style.transition = 'transform 0.1s ease-out'
    zoomGroupRef.value.setAttribute('transform', `translate(${panOffset.value.x}, ${panOffset.value.y}) scale(${zoom.value})`)
    setTimeout(() => { if (zoomGroupRef.value) zoomGroupRef.value.style.transition = '' }, 100)
  }
}

function toggleLegend() { showLegend.value = !showLegend.value }

function startDrag(node, event) {
  event.stopPropagation()
  isDragging.value = true
  draggedNode.value = node
  dragStart.value = { x: event.clientX, y: event.clientY }
}

function handleMouseDown(event) {
  if (event.target.tagName === 'svg' || event.target.classList.contains('graph-svg')) {
    isPanning.value = true
    panStart.value = { x: event.clientX - panOffset.value.x, y: event.clientY - panOffset.value.y }
  }
}

function handleMouseMove(event) {
  if (isDragging.value && draggedNode.value) {
    const dx = (event.clientX - dragStart.value.x) / zoom.value
    const dy = (event.clientY - dragStart.value.y) / zoom.value
    if (selectedNodes.value.size > 1 && selectedNodes.value.has(draggedNode.value.id)) {
      nodes.value.forEach(node => {
        if (selectedNodes.value.has(node.id)) { node.x += dx; node.y += dy }
      })
    } else {
      draggedNode.value.x += dx
      draggedNode.value.y += dy
    }
    dragStart.value = { x: event.clientX, y: event.clientY }
  } else if (isPanning.value) {
    panOffset.value = { x: event.clientX - panStart.value.x, y: event.clientY - panStart.value.y }
    updateZoom()
  }
}

function handleMouseUp() {
  isDragging.value = false
  draggedNode.value = null
  isPanning.value = false
}

function handleWheel(event) {
  const zoomFactor = event.deltaY > 0 ? 0.9 : 1.1
  const newZoom = Math.max(0.1, Math.min(5, zoom.value * zoomFactor))
  const rect = svgRef.value.getBoundingClientRect()
  const mx = event.clientX - rect.left
  const my = event.clientY - rect.top
  const wx = (mx - panOffset.value.x) / zoom.value
  const wy = (my - panOffset.value.y) / zoom.value
  zoom.value = newZoom
  panOffset.value = { x: mx - wx * zoom.value, y: my - wy * zoom.value }
  updateZoom()
}

function handleKeyDown(event) {
  if (event.key === 'Escape') {
    closeDetail()
  }
}

watch(() => [props.nodeDefs, props.edgeDefs], () => { transformData() }, { deep: true })

onMounted(() => {
  transformData()
  nextTick(() => applyLayout())
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.graph-modeling-view {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 400px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
  gap: 16px;
  flex-shrink: 0;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-label {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.graph-container {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
  background: var(--el-bg-color);
}

.graph-svg {
  width: 100%;
  height: 100%;
  cursor: grab;
}

.graph-svg:active { cursor: grabbing; }

.node { cursor: move; }

.node .node-circle {
  transition: filter 0.2s, stroke-width 0.2s;
}

.node:hover .node-circle {
  stroke-width: 3;
}

.node.selected .node-circle {
  stroke: var(--el-color-primary);
  stroke-width: 3;
}

.node.dragging .node-circle {
  stroke: var(--el-color-primary);
  stroke-width: 3;
}

@keyframes selectPulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.5; }
}

.select-halo { animation: selectPulse 2s ease-in-out infinite; }
.drag-halo { animation: selectPulse 1s ease-in-out infinite; }

@keyframes badgePop {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.property-badge {
  pointer-events: none;
  animation: badgePop 0.3s ease-out;
}

.edge .edge-line {
  cursor: pointer;
  transition: stroke 0.2s, stroke-width 0.2s;
}

.edge:hover .edge-line {
  stroke: var(--el-color-primary) !important;
  stroke-width: 3;
}

.edge.highlighted .edge-line {
  stroke: var(--el-color-primary) !important;
  stroke-width: 3;
}

@keyframes edgeFlow {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: 15; }
}

.edge .edge-flow { animation: edgeFlow 1.5s linear infinite; }

.edge-label { pointer-events: none; }

.edge:hover .edge-label,
.edge.highlighted .edge-label {
  fill: var(--el-color-primary);
}

/* 属性详情面板（画布内右侧） */
.detail-panel {
  position: absolute;
  top: 12px;
  right: 0;
  width: 260px;
  max-height: calc(100% - 24px);
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow);
  z-index: 10;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color);
  font-weight: 600;
  font-size: 13px;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
  background: var(--el-bg-color-overlay);
}

.detail-panel-body {
  padding: 8px 12px;
  overflow-y: auto;
  flex: 1;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
}

.detail-label {
  color: var(--el-text-color-secondary);
  min-width: 60px;
  flex-shrink: 0;
}

.properties-section {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.property-header {
  display: flex;
  align-items: center;
  padding: 4px 0;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color);
  gap: 2px;
}

.property-item {
  display: flex;
  align-items: center;
  padding: 5px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  gap: 2px;
  font-size: 12px;
}

.property-item:last-child { border-bottom: none; }

.prop-col-code { width: 60px; flex-shrink: 0; }
.prop-col-name { width: 60px; flex-shrink: 0; }
.prop-col-type { width: 60px; flex-shrink: 0; }
.prop-col-flags { display: flex; gap: 2px; }

.prop-code {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: var(--el-text-color-regular);
}

.prop-name {
  font-size: 12px;
  color: var(--el-text-color-primary);
}

.legend-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 260px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow);
  z-index: 10;
  max-height: calc(100% - 24px);
  overflow-y: auto;
}

.legend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--el-border-color);
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.legend-content { padding: 10px; }
.legend-section { margin-bottom: 12px; }
.legend-section:last-child { margin-bottom: 0; }

.legend-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.legend-item:hover { background: var(--el-fill-color-light); }

.legend-color {
  width: 18px; height: 18px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-edge {
  width: 28px; height: 2px;
  background: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
