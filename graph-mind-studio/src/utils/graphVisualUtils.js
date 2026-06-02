/**
 * 图可视化相关工具函数
 */
import * as d3 from 'd3'

/**
 * 清除SVG内容
 * @param {Object} simulation 力导向模拟对象
 * @param {Object} svgRef SVG引用
 */
export const clearSvg = (simulation, svgRef) => {
  if (simulation) {
    simulation.stop()
  }

  if (svgRef && svgRef.value) {
    d3.select(svgRef.value).selectAll('*').remove()
  }
}

/**
 * 创建箭头标记
 * @param {Object} container SVG容器
 * @param {Array} edges 边数据数组
 */
export const createArrowMarkers = (container, edges) => {
  const defs = container.append("defs")
  
  // 为每条边创建独立的箭头标记，以适应不同颜色和大小
  edges.forEach((d, i) => {
    const isPath = d.source.group === 'path' && d.target.group === 'path'
    const color = isPath ? '#f56c6c' : '#999'
    const size = isPath ? 17 : 12 // 节点半径 + 2
    
    defs.append("marker")
      .attr("id", `arrow-${i}`)
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", size)
      .attr("refY", 0)
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M 0 -5 L 10 0 L 0 5")
      .attr("fill", color)
  })
}

/**
 * 创建力导向模拟
 * @param {Array} nodes 节点数据数组
 * @param {Array} edges 边数据数组
 * @param {Object} container 容器对象
 * @param {Function} ticked 回调函数
 * @returns {Object} 力导向模拟对象
 */
export const createForceSimulation = (nodes, edges, container, ticked) => {
  const simulation = d3.forceSimulation(nodes)
    .force('link', d3.forceLink(edges).id(d => d.id).distance(100))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(
      container.clientWidth / 2, 
      container.clientHeight / 2
    ))
    .on('tick', ticked)
    
  return simulation
}

/**
 * 绘制节点
 * @param {Object} container SVG容器
 * @param {Array} nodes 节点数据数组
 * @param {Object} analysisForm 分析表单对象
 * @param {Function} dragStarted 拖拽开始回调
 * @param {Function} dragged 拖拽中回调
 * @param {Function} dragEnded 拖拽结束回调
 * @returns {Object} 节点元素
 */
export const drawNodes = (container, nodes, analysisForm, dragStarted, dragged, dragEnded) => {
  const node = container.append('g')
    .attr('class', 'nodes')
    .selectAll('circle')
    .data(nodes)
    .enter()
    .append('circle')
    .attr('r', d => d.group === 'path' ? 15 : 10)
    .attr('fill', d => {
      if (d.group === 'path') return '#f56c6c'
      if (d.group === 'center') return '#e6a23c'
      if (d.group === 'layer1') return '#409EFF'
      if (d.group === 'layer2') return '#67c23a'
      if (d.id === analysisForm.sourceId) return '#67c23a'
      if (d.id === analysisForm.targetId) return '#e6a23c'
      return '#409EFF'
    })
    .call(d3.drag()
      .on('start', dragStarted)
      .on('drag', dragged)
      .on('end', dragEnded)
    )
    
  return node
}

/**
 * 绘制边
 * @param {Object} container SVG容器
 * @param {Array} edges 边数据数组
 * @returns {Object} 边元素
 */
export const drawEdges = (container, edges) => {
  const link = container.append('g')
    .attr('class', 'links')
    .selectAll('line')
    .data(edges)
    .enter()
    .append('line')
    .attr('stroke', d => d.source.group === 'path' && d.target.group === 'path' ? '#f56c6c' : '#999')
    .attr('stroke-width', d => d.source.group === 'path' && d.target.group === 'path' ? 3 : 2)
    .attr("marker-end", (d, i) => `url(#arrow-${i})`)
    
  return link
}

/**
 * 绘制节点标签
 * @param {Object} container SVG容器
 * @param {Array} nodes 节点数据数组
 * @returns {Object} 标签元素
 */
export const drawLabels = (container, nodes) => {
  const text = container.append('g')
    .attr('class', 'labels')
    .selectAll('text')
    .data(nodes)
    .enter()
    .append('text')
    .text(d => d.label)
    .attr('text-anchor', 'middle')
    .attr('dy', 25)
    .attr('fill', '#333')
    .attr('font-size', '12px')
    
  return text
}

/**
 * 缩放功能
 * @param {Object} svg SVG对象
 * @param {Object} zoom 缩放对象
 * @param {number} scale 缩放比例
 */
export const zoomBy = (svg, zoom, scale) => {
  if (svg && zoom) {
    svg.transition().call(zoom.scaleBy, scale)
  }
}

/**
 * 重置视图
 * @param {Object} svg SVG对象
 * @param {Object} zoom 缩放对象
 */
export const resetView = (svg, zoom) => {
  if (svg && zoom) {
    svg.transition().call(zoom.transform, d3.zoomIdentity)
  }
}

export default {
  clearSvg,
  createArrowMarkers,
  createForceSimulation,
  drawNodes,
  drawEdges,
  drawLabels,
  zoomBy,
  resetView
}