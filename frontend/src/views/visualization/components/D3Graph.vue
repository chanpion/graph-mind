<template>
  <div class="d3-graph">
    <!-- SVG 画布 -->
    <div ref="container" class="d3-graph-container" :style="{ width: width + 'px', height: height + 'px' }"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount, nextTick } from 'vue'
import * as d3 from 'd3'

const props = defineProps({
  data: { type: Object, default: () => ({ nodes: [], edges: [] }) },
  width: { type: Number, default: 800 },
  height: { type: Number, default: 600 },
  layoutType: { type: String, default: 'force' }
})

const emit = defineEmits(['node-click', 'edge-click'])

const container = ref(null)

let svg = null
let simulation = null
let gRoot = null
let zoomBehavior = null
let linkElements = null
let nodeElements = null
let labelElements = null

// 为节点生成唯一 ID
function prepareData(data) {
  const nodes = (data.nodes || []).map((n, i) => ({
    ...n,
    id: n.id || n.uid || `node-${i}`,
    index: undefined,
    x: undefined,
    y: undefined,
    fx: undefined,
    fy: undefined
  }))
  const nodeMap = new Map(nodes.map(n => [n.id, n]))

  const edges = (data.edges || []).map((e, i) => {
    const sourceVal = typeof e.source === 'object' ? (e.source.id || e.source.uid || e.source) : (e.source || e.startUid)
    const targetVal = typeof e.target === 'object' ? (e.target.id || e.target.uid || e.target) : (e.target || e.endUid)
    return {
      ...e,
      id: e.id || e.uid || `edge-${i}`,
      source: sourceVal,
      target: targetVal
    }
  }).filter(e => nodeMap.has(e.source) && nodeMap.has(e.target))

  // 将边的 source 和 target 从 ID 转换为对象引用（供层次图等非力导布局使用）
  edges.forEach(e => {
    e.source = nodeMap.get(e.source) || e.source
    e.target = nodeMap.get(e.target) || e.target
  })

  return { nodes, edges }
}

// 计算层次布局 (自上而下的树)
function applyHierarchicalLayout(nodes, edges, w, h) {
  if (nodes.length === 0) return
  // 构建邻接表找根
  const children = new Map()
  const hasParent = new Set()
  nodes.forEach(n => children.set(n.id, []))
  edges.forEach(e => {
    const sid = typeof e.source === 'object' ? e.source.id : e.source
    const tid = typeof e.target === 'object' ? e.target.id : e.target
    if (children.has(sid)) children.get(sid).push(tid)
    hasParent.add(tid)
  })
  const roots = nodes.filter(n => !hasParent.has(n.id))
  if (roots.length === 0) roots.push(nodes[0])

  const levelMap = new Map()
  const maxDepth = { val: 0 }
  function assignLevel(id, depth) {
    if (levelMap.has(id) && levelMap.get(id) <= depth) return
    levelMap.set(id, depth)
    maxDepth.val = Math.max(maxDepth.val, depth)
    ;(children.get(id) || []).forEach(cid => assignLevel(cid, depth + 1))
  }
  roots.forEach(r => assignLevel(r.id, 0))

  const levels = []
  levelMap.forEach((depth, id) => {
    if (!levels[depth]) levels[depth] = []
    levels[depth].push(id)
  })

  const padding = { top: 40, bottom: 40, left: 60, right: 60 }
  const levelHeight = (h - padding.top - padding.bottom) / Math.max(levels.length - 1, 1)
  levels.forEach((ids, depth) => {
    const y = padding.top + depth * levelHeight
    const spacing = (w - padding.left - padding.right) / Math.max(ids.length, 1)
    ids.forEach((id, i) => {
      const node = nodes.find(n => n.id === id)
      if (node) {
        node.x = padding.left + i * spacing + spacing / 2
        node.y = y
        node.fx = node.x
        node.fy = node.y
      }
    })
  })
}

function initChart() {
  if (!container.value) return

  // 清理旧实例
  if (svg) d3.select(container.value).selectAll('svg').remove()

  svg = d3.select(container.value)
    .append('svg')
    .attr('width', props.width)
    .attr('height', props.height)
    .style('cursor', 'grab')

  // 缩放行为
  zoomBehavior = d3.zoom()
    .scaleExtent([0.1, 8])
    .on('zoom', (event) => {
      gRoot.attr('transform', event.transform)
    })

  svg.call(zoomBehavior)

  gRoot = svg.append('g')
    .attr('class', 'graph-root')

  // 绘制
  render()
}

function render() {
  if (!gRoot) return
  gRoot.selectAll('*').remove()

  const { nodes, edges } = prepareData(props.data)
  if (nodes.length === 0) return

  // 应用布局
  if (props.layoutType === 'hierarchical') {
    applyHierarchicalLayout(nodes, edges, props.width, props.height)
  }
  // force 布局由力模拟处理，此处不预设坐标

  // 箭头标记
  const defs = gRoot.append('defs')
  defs.append('marker')
    .attr('id', 'arrowhead')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 20)
    .attr('refY', 0)
    .attr('markerWidth', 6)
    .attr('markerHeight', 6)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M0,-5L10,0L0,5')
    .attr('fill', '#94a3b8')

  // 边（用 g 包裹，内部加透明宽线扩大点击区域）
  const edgeGroup = gRoot.append('g')
    .attr('class', 'edges')
    .selectAll('g')
    .data(edges)
    .join('g')
    .style('cursor', 'pointer')
    .on('click', (event, d) => {
      event.stopPropagation()
      emit('edge-click', d)
    })

  // 透明宽点击热区
  edgeGroup.append('line')
    .attr('class', 'edge-hit')
    .attr('stroke', 'transparent')
    .attr('stroke-width', 14)

  // 可见边线
  edgeGroup.append('line')
    .attr('class', 'edge-visual')
    .attr('stroke', '#94a3b8')
    .attr('stroke-width', 2)
    .attr('stroke-opacity', 0.6)
    .attr('marker-end', 'url(#arrowhead)')

  // 保存引用（用于 tick 更新）
  linkElements = edgeGroup

  // 边标签
  const edgeLabels = gRoot.append('g')
    .attr('class', 'edge-labels')
    .selectAll('text')
    .data(edges)
    .join('text')
    .text(d => d.label || '')
    .attr('font-size', 10)
    .attr('fill', '#94a3b8')
    .attr('text-anchor', 'middle')
    .attr('dy', -4)

  // 节点组
  const nodeGroup = gRoot.append('g')
    .attr('class', 'nodes')
    .selectAll('g')
    .data(nodes)
    .join('g')
    .attr('cursor', 'pointer')
    .call(d3.drag()
      .on('start', (event, d) => {
        if (!event.active && simulation) simulation.alphaTarget(0.3).restart()
        d.fx = d.x
        d.fy = d.y
      })
      .on('drag', (event, d) => {
        d.fx = event.x
        d.fy = event.y
      })
      .on('end', (event, d) => {
        if (!event.active && simulation) simulation.alphaTarget(0)
        if (props.layoutType !== 'force') return
        d.fx = null
        d.fy = null
      })
    )
    .on('click', (event, d) => {
      event.stopPropagation()
      emit('node-click', d)
    })

  // 节点圆形
  nodeElements = nodeGroup.append('circle')
    .attr('r', 16)
    .attr('fill', d => {
      const label = (d.label || '').toLowerCase()
      if (label.includes('person') || label.includes('people')) return '#6366f1'
      if (label.includes('company') || label.includes('org')) return '#f59e0b'
      if (label.includes('place') || label.includes('location')) return '#10b981'
      return '#6366f1'
    })
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)

  // 节点标签（居中显示在节点圆形内）
  labelElements = nodeGroup.append('text')
    .text(d => {
      const uid = d.properties?.uid || d.uid || d.id || ''
      return String(uid).substring(0, 6)
    })
    .attr('text-anchor', 'middle')
    .attr('dy', '0.35em')
    .attr('font-size', 10)
    .attr('fill', '#fff')
    .attr('font-weight', 600)
    .attr('pointer-events', 'none')

  // 力导向布局
  if (props.layoutType === 'force') {
    simulation = d3.forceSimulation(nodes)
      .force('link', d3.forceLink(edges).id(d => d.id).distance(80))
      .force('charge', d3.forceManyBody().strength(-120))
      .force('center', d3.forceCenter(props.width / 2, props.height / 2))
      .force('collision', d3.forceCollide(25))
      .on('tick', () => {
        linkElements.selectAll('line')
          .attr('x1', d => d.source.x)
          .attr('y1', d => d.source.y)
          .attr('x2', d => d.target.x)
          .attr('y2', d => d.target.y)
        edgeLabels
          .attr('x', d => (d.source.x + d.target.x) / 2)
          .attr('y', d => (d.source.y + d.target.y) / 2)
        nodeGroup.attr('transform', d => `translate(${d.x},${d.y})`)
      })
      .on('end', () => {
        // 模拟稳定后再适应视图
        safeFitToView()
      })
  } else {
    // 非力导向：直接定位
    nodeGroup.attr('transform', d => `translate(${d.x},${d.y})`)
    linkElements.selectAll('line')
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y)
    edgeLabels
      .attr('x', d => (d.source.x + d.target.x) / 2)
      .attr('y', d => (d.source.y + d.target.y) / 2)
  }

  // 非力导向：立即适应；力导向：由 on('end') 触发 safeFitToView
  if (props.layoutType !== 'force') {
    requestAnimationFrame(() => requestAnimationFrame(safeFitToView))
  }
}

let fitToViewTimer = null

function destroySimulation() {
  if (simulation) {
    simulation.stop()
    simulation = null
  }
  // 清理待执行的 fitToView 定时器
  if (fitToViewTimer) {
    clearTimeout(fitToViewTimer)
    fitToViewTimer = null
  }
}

// 安全调用 fitToView：防抖，避免重复触发
function safeFitToView() {
  if (fitToViewTimer) clearTimeout(fitToViewTimer)
  fitToViewTimer = setTimeout(() => {
    fitToViewTimer = null
    fitToView()
  }, 100)
}

// 自动缩放适应视图：计算所有节点的边界框，缩放并平移使所有节点可见
function fitToView() {
  if (!svg || !zoomBehavior || !gRoot) return
  const gRootNode = gRoot.node()
  if (!gRootNode) return
  const bounds = gRootNode.getBBox()
  if (bounds.width === 0 || bounds.height === 0) return
  // 用 SVG 实际渲染尺寸，而非 props（避免 props 与真实渲染尺寸不一致）
  const svgNode = svg.node()
  if (!svgNode) return
  const clientRect = svgNode.getBoundingClientRect()
  const viewW = clientRect.width || props.width || 800
  const viewH = clientRect.height || props.height || 600
  const padding = 60
  const availW = viewW - padding * 2
  const availH = viewH - padding * 2
  if (availW <= 0 || availH <= 0) return
  const scale = Math.min(availW / bounds.width, availH / bounds.height, 2)
  // tx, ty 是 gRoot 的 translate，使得边界框在视口中居中
  const tx = padding + (availW - bounds.width * scale) / 2 - bounds.x * scale
  const ty = padding + (availH - bounds.height * scale) / 2 - bounds.y * scale
  svg.transition().duration(600).call(
    zoomBehavior.transform,
    d3.zoomIdentity.translate(tx, ty).scale(scale)
  )
}

// 公开方法
function resetView() {
  if (svg && zoomBehavior) {
    svg.transition().duration(500).call(zoomBehavior.transform, d3.zoomIdentity)
  }
}

function zoomIn() {
  if (svg && zoomBehavior) {
    svg.transition().duration(300).call(zoomBehavior.scaleBy, 1.3)
  }
}

function zoomOut() {
  if (svg && zoomBehavior) {
    svg.transition().duration(300).call(zoomBehavior.scaleBy, 0.7)
  }
}

watch(() => props.data, () => {
  destroySimulation()
  nextTick(render)
}, { deep: true })

watch(() => props.layoutType, () => {
  destroySimulation()
  nextTick(render)
})

watch(() => [props.width, props.height], () => {
  initChart()
})

onMounted(() => {
  initChart()
})

onBeforeUnmount(() => {
  destroySimulation()
})

defineExpose({ resetView, zoomIn, zoomOut })
</script>

<style scoped>
.d3-graph {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.d3-graph-container {
  overflow: hidden;
  background: transparent;
}

.d3-graph-container :deep(svg) {
  display: block;
}
</style>
