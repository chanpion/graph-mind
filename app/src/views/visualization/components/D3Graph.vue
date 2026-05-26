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
    const sourceVal = typeof e.source === 'object' ? (e.source.id || e.source.uid || e.source) : (e.source || e.startUid || e.sourceUid)
    const targetVal = typeof e.target === 'object' ? (e.target.id || e.target.uid || e.target) : (e.target || e.endUid || e.targetUid)
    return {
      ...e,
      id: e.id || e.uid || `edge-${i}`,
      source: sourceVal,
      target: targetVal
    }
  }).filter(e => nodeMap.has(e.source) && nodeMap.has(e.target))

  return { nodes, edges }
}

// 计算圆形布局
function applyCircularLayout(nodes, w, h) {
  const cx = w / 2, cy = h / 2
  const radius = Math.min(w, h) * 0.35
  nodes.forEach((n, i) => {
    const angle = (2 * Math.PI * i) / nodes.length
    n.x = cx + radius * Math.cos(angle)
    n.y = cy + radius * Math.sin(angle)
    n.fx = n.x
    n.fy = n.y
  })
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

// 计算网格布局
function applyGridLayout(nodes, w, h) {
  const cols = Math.ceil(Math.sqrt(nodes.length))
  const rows = Math.ceil(nodes.length / cols)
  const padding = { top: 30, bottom: 30, left: 40, right: 40 }
  const cellW = (w - padding.left - padding.right) / cols
  const cellH = (h - padding.top - padding.bottom) / rows
  nodes.forEach((n, i) => {
    const col = i % cols
    const row = Math.floor(i / cols)
    n.x = padding.left + col * cellW + cellW / 2
    n.y = padding.top + row * cellH + cellH / 2
    n.fx = n.x
    n.fy = n.y
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
  if (props.layoutType === 'circular') {
    applyCircularLayout(nodes, props.width, props.height)
  } else if (props.layoutType === 'hierarchical') {
    applyHierarchicalLayout(nodes, edges, props.width, props.height)
  } else if (props.layoutType === 'grid') {
    applyGridLayout(nodes, props.width, props.height)
  }

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

  // 边
  linkElements = gRoot.append('g')
    .attr('class', 'edges')
    .selectAll('line')
    .data(edges)
    .join('line')
    .attr('stroke', '#94a3b8')
    .attr('stroke-width', 1.5)
    .attr('stroke-opacity', 0.6)
    .attr('marker-end', 'url(#arrowhead)')
    .on('click', (event, d) => {
      event.stopPropagation()
      emit('edge-click', d)
    })

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
    .attr('r', 8)
    .attr('fill', d => {
      const label = (d.label || '').toLowerCase()
      if (label.includes('person') || label.includes('people')) return '#6366f1'
      if (label.includes('company') || label.includes('org')) return '#f59e0b'
      if (label.includes('place') || label.includes('location')) return '#10b981'
      return '#6366f1'
    })
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)

  // 节点标签
  labelElements = nodeGroup.append('text')
    .text(d => d.label || d.id)
    .attr('dx', 12)
    .attr('dy', 4)
    .attr('font-size', 11)
    .attr('fill', '#e2e8f0')
    .attr('pointer-events', 'none')

  // 力导向布局
  if (props.layoutType === 'force') {
    simulation = d3.forceSimulation(nodes)
      .force('link', d3.forceLink(edges).id(d => d.id).distance(100))
      .force('charge', d3.forceManyBody().strength(-200))
      .force('center', d3.forceCenter(props.width / 2, props.height / 2))
      .force('collision', d3.forceCollide(20))
      .on('tick', () => {
        linkElements
          .attr('x1', d => d.source.x)
          .attr('y1', d => d.source.y)
          .attr('x2', d => d.target.x)
          .attr('y2', d => d.target.y)
        edgeLabels
          .attr('x', d => (d.source.x + d.target.x) / 2)
          .attr('y', d => (d.source.y + d.target.y) / 2)
        nodeGroup.attr('transform', d => `translate(${d.x},${d.y})`)
      })
  } else {
    // 非力导向：直接定位
    nodeGroup.attr('transform', d => `translate(${d.x},${d.y})`)
    linkElements
      .attr('x1', d => d.source.x || d.source)
      .attr('y1', d => d.source.y || 0)
      .attr('x2', d => d.target.x || d.target)
      .attr('y2', d => d.target.y || 0)
    edgeLabels
      .attr('x', d => {
        const sx = typeof d.source === 'object' ? d.source.x : 0
        const tx = typeof d.target === 'object' ? d.target.x : 0
        return (sx + tx) / 2
      })
      .attr('y', d => {
        const sy = typeof d.source === 'object' ? d.source.y : 0
        const ty = typeof d.target === 'object' ? d.target.y : 0
        return (sy + ty) / 2
      })
  }
}

function destroySimulation() {
  if (simulation) {
    simulation.stop()
    simulation = null
  }
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
