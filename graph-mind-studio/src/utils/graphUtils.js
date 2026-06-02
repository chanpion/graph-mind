/**
 * 图数据处理相关工具函数
 */

/**
 * 生成示例图数据
 * @param {number} nodeCount 节点数量
 * @returns {Object} 包含节点和边的数据对象
 */
export const generateSampleGraphData = (nodeCount) => {
  const nodes = []
  const edges = []

  // 生成节点
  for (let i = 0; i < nodeCount; i++) {
    nodes.push({
      id: `node_${i}`,
      label: `节点${i}`,
      group: 'normal',
      x: Math.random() * 600,
      y: Math.random() * 400
    })
  }

  // 生成边
  const edgeCount = Math.min(nodeCount * 2, nodeCount * (nodeCount - 1) / 2)
  for (let i = 0; i < edgeCount; i++) {
    const sourceIndex = Math.floor(Math.random() * nodeCount)
    let targetIndex
    do {
      targetIndex = Math.floor(Math.random() * nodeCount)
    } while (targetIndex === sourceIndex)

    edges.push({
      source: nodes[sourceIndex].id,
      target: nodes[targetIndex].id,
      value: Math.random() * 5
    })
  }

  return { nodes, edges }
}

/**
 * 生成K层展开示例数据
 * @returns {Object} 包含节点和边的数据对象
 */
export const generateKLayerExpandData = () => {
  const nodes = []
  const edges = []

  // 生成中心节点
  const centerNode = {
    id: 'center',
    label: '中心节点',
    group: 'center',
    x: 300,
    y: 200
  }
  nodes.push(centerNode)

  // 生成第一层节点
  for (let i = 1; i <= 5; i++) {
    nodes.push({
      id: `layer1_${i}`,
      label: `第一层节点${i}`,
      group: 'layer1',
      x: 300 + Math.random() * 100 - 50,
      y: 200 + Math.random() * 100 - 50
    })
  }

  // 生成第二层节点
  for (let i = 1; i <= 8; i++) {
    nodes.push({
      id: `layer2_${i}`,
      label: `第二层节点${i}`,
      group: 'layer2',
      x: 300 + Math.random() * 150 - 75,
      y: 200 + Math.random() * 150 - 75
    })
  }

  // 生成边
  for (let i = 1; i <= 5; i++) {
    edges.push({
      source: 'center',
      target: `layer1_${i}`,
      value: 1.2
    })
  }

  for (let i = 1; i <= 8; i++) {
    edges.push({
      source: `layer1_${Math.floor(Math.random() * 5) + 1}`,
      target: `layer2_${i}`,
      value: 0.8
    })
  }

  return { nodes, edges }
}

/**
 * 生成路径查询示例数据
 * @returns {Object} 包含节点和边的数据对象
 */
export const generatePathQueryData = () => {
  const nodes = [
    {id: '1', label: '起点', group: 'path', x: 100, y: 100},
    {id: '3', label: '节点3', group: 'path', x: 200, y: 150},
    {id: '5', label: '节点5', group: 'path', x: 300, y: 100},
    {id: '7', label: '节点7', group: 'path', x: 400, y: 150},
    {id: '9', label: '终点', group: 'path', x: 500, y: 100},
    {id: '2', label: '普通节点', group: 'normal', x: 150, y: 250},
    {id: '4', label: '普通节点', group: 'normal', x: 350, y: 250},
    {id: '6', label: '普通节点', group: 'normal', x: 250, y: 300}
  ]
  
  const edges = [
    {source: '1', target: '3', value: 1.2},
    {source: '3', target: '5', value: 1.5},
    {source: '5', target: '7', value: 0.8},
    {source: '7', target: '9', value: 0.7},
    {source: '1', target: '2', value: 2.1},
    {source: '2', target: '4', value: 1.3},
    {source: '4', target: '6', value: 1.1},
    {source: '6', target: '5', value: 1.9}
  ]

  return { nodes, edges }
}

/**
 * 处理边数据，确保source和target引用节点对象
 * @param {Array} edges 边数据数组
 * @param {Array} nodes 节点数据数组
 * @returns {Array} 处理后的边数据
 */
export const processEdgesData = (edges, nodes) => {
  // 创建uid到节点的映射
  const nodeMap = {}
  nodes.forEach(node => {
    nodeMap[node.uid || node.id] = node
  })

  // 处理边数据
  return edges.map(edge => {
    const sourceNode = nodeMap[edge.startUid || edge.source]
    const targetNode = nodeMap[edge.endUid || edge.target]
    
    if (sourceNode && targetNode) {
      return {
        ...edge,
        source: sourceNode,
        target: targetNode
      }
    }
    return edge
  }).filter(edge => edge.source && edge.target)
}

export default {
  generateSampleGraphData,
  generateKLayerExpandData,
  generatePathQueryData,
  processEdgesData
}