<template>
  <div class="graph-visual-container">
    <div class="visual-toolbar">
      <el-select v-model="selectedGraphId" placeholder="请选择图" style="width: 200px" @change="handleGraphChange">
        <el-option v-for="g in graphList" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-input v-model="query" placeholder="请输入查询语句" style="width: 400px; margin: 0 12px;" />
      <el-button type="primary" @click="handleQuery">查询</el-button>
      
      <!-- 添加布局切换控件 -->
      <el-select v-model="layoutType" placeholder="选择布局" style="width: 120px; margin-left: 12px;" @change="handleLayoutChange">
        <el-option label="力导向图" value="force" />
        <el-option label="层次布局" value="hierarchy" />
      </el-select>
      
      <!-- 添加样式设置按钮 -->
      <el-button type="primary" @click="styleDrawerVisible = true" style="margin-left: 12px;">样式设置</el-button>
    </div>
    <div class="visual-area">
      <svg ref="svgRef" :width="width" :height="height"></svg>
    </div>
    
    <!-- 节点/关系详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" :title="detailType==='node' ? '点详情' : '边详情'" direction="rtl" size="350px">
      <div v-if="detailType==='node'">
        <div><b>ID：</b>{{ detailData.id }}</div>
        <div><b>名称：</b>{{ detailData.label }}</div>
        <div><b>类型：</b>{{ detailData.type || '-' }}</div>
        <div v-if="detailData.properties && detailData.properties.length">
          <b>属性：</b>
          <el-table :data="detailData.properties" size="small" border style="margin-top: 8px;">
            <el-table-column prop="name" label="属性名" />
            <el-table-column prop="value" label="属性值" />
          </el-table>
        </div>
      </div>
      <div v-else>
        <div><b>ID：</b>{{ detailData.id }}</div>
        <div><b>类型：</b>{{ detailData.label || detailData.type || '-' }}</div>
        <div><b>起点：</b>{{ detailData.source && detailData.source.id ? detailData.source.id : detailData.source }}</div>
        <div><b>终点：</b>{{ detailData.target && detailData.target.id ? detailData.target.id : detailData.target }}</div>
        <div v-if="detailData.properties && detailData.properties.length">
          <b>属性：</b>
          <el-table :data="detailData.properties" size="small" border style="margin-top: 8px;">
            <el-table-column prop="name" label="属性名" />
            <el-table-column prop="value" label="属性值" />
          </el-table>
        </div>
      </div>
    </el-drawer>
    
    <!-- 样式设置抽屉 -->
    <el-drawer v-model="styleDrawerVisible" title="样式设置" direction="rtl" size="450px">
      <el-tabs v-model="styleTabActive">
        <!-- 节点样式设置 -->
        <el-tab-pane label="节点样式" name="node">
          <el-form :model="nodeStyleConfig" label-width="100px">
            <el-form-item label="默认节点大小">
              <el-slider v-model="nodeStyleConfig.defaultSize" :min="10" :max="50" show-input />
            </el-form-item>
            <el-form-item label="默认节点颜色">
              <el-color-picker v-model="nodeStyleConfig.defaultColor" />
            </el-form-item>
            <el-form-item label="按类型配置">
              <el-button @click="addNodeTypeStyle">添加类型配置</el-button>
              <div v-for="(style, index) in nodeStyleConfig.typeStyles" :key="index" class="type-style-item">
                <el-select v-model="style.type" placeholder="选择节点类型" style="width: 120px; margin-right: 10px;">
                  <el-option 
                    v-for="nodeType in nodeTypes" 
                    :key="nodeType" 
                    :label="nodeType" 
                    :value="nodeType" 
                  />
                </el-select>
                <el-color-picker v-model="style.color" style="margin-right: 10px;" />
                <el-input-number v-model="style.size" :min="10" :max="50" controls-position="right" style="width: 80px;" />
                <el-button type="danger" icon="Delete" circle @click="removeNodeTypeStyle(index)" style="margin-left: 10px;" />
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 关系样式设置 -->
        <el-tab-pane label="关系样式" name="edge">
          <el-form :model="edgeStyleConfig" label-width="100px">
            <el-form-item label="默认线宽">
              <el-slider v-model="edgeStyleConfig.defaultWidth" :min="1" :max="10" show-input />
            </el-form-item>
            <el-form-item label="默认线颜色">
              <el-color-picker v-model="edgeStyleConfig.defaultColor" />
            </el-form-item>
            <el-form-item label="按类型配置">
              <el-button @click="addEdgeTypeStyle">添加类型配置</el-button>
              <div v-for="(style, index) in edgeStyleConfig.typeStyles" :key="index" class="type-style-item">
                <el-select v-model="style.type" placeholder="选择关系类型" style="width: 120px; margin-right: 10px;">
                  <el-option 
                    v-for="edgeType in edgeTypes" 
                    :key="edgeType" 
                    :label="edgeType" 
                    :value="edgeType" 
                  />
                </el-select>
                <el-color-picker v-model="style.color" style="margin-right: 10px;" />
                <el-input-number v-model="style.width" :min="1" :max="10" controls-position="right" style="width: 80px;" />
                <el-button type="danger" icon="Delete" circle @click="removeEdgeTypeStyle(index)" style="margin-left: 10px;" />
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      
      <div class="style-drawer-footer">
        <el-button @click="resetStyleConfig">重置默认</el-button>
        <el-button type="primary" @click="applyStyleConfig">应用样式</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="GraphVisual">
import { ref, onMounted, computed } from 'vue'
import * as d3 from 'd3'
import graphApi from '@/api/graph'
import { ElMessage } from 'element-plus'

const width = 800
const height = 500
const svgRef = ref(null)

const graphList = ref([])
const selectedGraphId = ref()
const query = ref('MATCH (n)-[r]->(m) RETURN n,r,m')
const nodes = ref([])
const edges = ref([])

// 添加布局类型控制
const layoutType = ref('force') // force 或 hierarchy

// 样式设置相关
const styleDrawerVisible = ref(false)
const styleTabActive = ref('node')

// 节点样式配置
const nodeStyleConfig = ref({
  defaultSize: 22,
  defaultColor: '#409EFF',
  typeStyles: []
})

// 关系样式配置
const edgeStyleConfig = ref({
  defaultWidth: 2,
  defaultColor: '#aaa',
  typeStyles: []
})

// 抽取所有节点类型和关系类型
const nodeTypes = computed(() => {
  const types = new Set()
  nodes.value.forEach(node => {
    if (node.label) {
      types.add(node.label)
    }
  })
  return Array.from(types)
})

const edgeTypes = computed(() => {
  const types = new Set()
  edges.value.forEach(edge => {
    if (edge.label) {
      types.add(edge.label)
    }
  })
  return Array.from(types)
})

const detailDrawerVisible = ref(false)
detailDrawerVisible.value = false
const detailType = ref('node') // node or edge
const detailData = ref({})

const fetchGraphList = async () => {
  try {
    const res = await graphApi.getGraphList()
    graphList.value = res.data.list
    if (graphList.value.length && !selectedGraphId.value) {
      selectedGraphId.value = graphList.value[0].id
    }
  } catch (e) {
    ElMessage.error('获取图列表失败')
  }
}

const handleGraphChange = () => {
  nodes.value = []
  edges.value = []
  clearSvg()
}

const handleQuery = async () => {
  if (!selectedGraphId.value) {
    ElMessage.warning('请选择图')
    return
  }
  // mock查询接口
  try {
    const res = await graphApi.queryGraph(selectedGraphId.value, query.value)
    nodes.value = res.data.nodes
    edges.value = res.data.edges
    drawGraph()
  } catch (e) {
    ElMessage.error('查询失败')
  }
}

// 添加布局切换处理函数
const handleLayoutChange = () => {
  if (nodes.value.length > 0) {
    drawGraph()
  }
}

// 添加节点类型样式配置
const addNodeTypeStyle = () => {
  nodeStyleConfig.value.typeStyles.push({
    type: '',
    color: '#409EFF',
    size: 22
  })
}

// 删除节点类型样式配置
const removeNodeTypeStyle = (index) => {
  nodeStyleConfig.value.typeStyles.splice(index, 1)
}

// 添加关系类型样式配置
const addEdgeTypeStyle = () => {
  edgeStyleConfig.value.typeStyles.push({
    type: '',
    color: '#aaa',
    width: 2
  })
}

// 删除关系类型样式配置
const removeEdgeTypeStyle = (index) => {
  edgeStyleConfig.value.typeStyles.splice(index, 1)
}

// 重置样式配置
const resetStyleConfig = () => {
  nodeStyleConfig.value = {
    defaultSize: 22,
    defaultColor: '#409EFF',
    typeStyles: []
  }
  
  edgeStyleConfig.value = {
    defaultWidth: 2,
    defaultColor: '#aaa',
    typeStyles: []
  }
}

// 应用样式配置
const applyStyleConfig = () => {
  styleDrawerVisible.value = false
  if (nodes.value.length > 0) {
    drawGraph()
  }
}

const clearSvg = () => {
  d3.select(svgRef.value).selectAll('*').remove()
}

const showDetail = (type, data) => {
  detailType.value = type
  detailData.value = data
  detailDrawerVisible.value = true
}

const drawGraph = () => {
  clearSvg()
  const svg = d3.select(svgRef.value)
  if (!nodes.value.length) return
  
  if (layoutType.value === 'force') {
    drawForceLayout(svg)
  } else {
    drawHierarchyLayout(svg)
  }
}

// 获取节点样式
const getNodeStyle = (node) => {
  // 查找是否有针对该类型的具体配置
  const typeStyle = nodeStyleConfig.value.typeStyles.find(style => style.type === node.label)
  if (typeStyle) {
    return {
      size: typeStyle.size,
      color: typeStyle.color
    }
  }
  // 使用默认配置
  return {
    size: nodeStyleConfig.value.defaultSize,
    color: nodeStyleConfig.value.defaultColor
  }
}

// 获取边样式
const getEdgeStyle = (edge) => {
  // 查找是否有针对该类型的具体配置
  const typeStyle = edgeStyleConfig.value.typeStyles.find(style => style.type === edge.label)
  if (typeStyle) {
    return {
      width: typeStyle.width,
      color: typeStyle.color
    }
  }
  // 使用默认配置
  return {
    width: edgeStyleConfig.value.defaultWidth,
    color: edgeStyleConfig.value.defaultColor
  }
}

// 力导向图布局
const drawForceLayout = (svg) => {
  // 力导向布局
  const simulation = d3.forceSimulation(nodes.value)
    .force('link', d3.forceLink(edges.value).id(d => d.id).distance(120))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(width / 2, height / 2))

  // 画边
  const link = svg.append('g')
    .selectAll('line')
    .data(edges.value)
    .join('line')
    .attr('stroke-width', d => getEdgeStyle(d).width)
    .attr('stroke', d => getEdgeStyle(d).color)
    .on('click', (event, d) => showDetail('edge', d))

  // 画点
  const node = svg.append('g')
    .selectAll('circle')
    .data(nodes.value)
    .join('circle')
    .attr('r', d => getNodeStyle(d).size)
    .attr('fill', d => getNodeStyle(d).color)
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .call(drag(simulation))
    .on('click', (event, d) => showDetail('node', d))

  // 点标签
  svg.append('g')
    .selectAll('text')
    .data(nodes.value)
    .join('text')
    .attr('text-anchor', 'middle')
    .attr('dy', 5)
    .attr('font-size', 14)
    .attr('fill', '#fff')
    .text(d => d.label)

  simulation.on('tick', () => {
    link
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y)
    node
      .attr('cx', d => d.x)
      .attr('cy', d => d.y)
    svg.selectAll('text')
      .attr('x', d => d.x)
      .attr('y', d => d.y)
  })
}

// 层次布局
const drawHierarchyLayout = (svg) => {
  // 创建层级结构（简单处理：以第一个节点为根节点）
  if (nodes.value.length === 0) return
  
  // 构建节点映射
  const nodeMap = {}
  nodes.value.forEach(node => {
    nodeMap[node.id] = { ...node, children: [] }
  })
  
  // 构建层级关系
  const rootNodes = []
  const linkedNodes = new Set()
  
  // 标记被链接的节点
  edges.value.forEach(edge => {
    linkedNodes.add(edge.source)
    linkedNodes.add(edge.target)
  })
  
  // 简单的层级分配（实际应用中可以更复杂）
  // 这里我们创建一个简单的树结构
  const rootNode = nodeMap[nodes.value[0].id] || { ...nodes.value[0], children: [] }
  rootNodes.push(rootNode)
  
  // 分配其他节点到层级中
  const levelMap = {}
  nodes.value.forEach((node, index) => {
    const level = Math.floor(index / 5) // 每5个节点一层
    if (!levelMap[level]) levelMap[level] = []
    levelMap[level].push(node)
    node.level = level
  })
  
  // 画边
  const link = svg.append('g')
    .selectAll('line')
    .data(edges.value)
    .join('line')
    .attr('stroke-width', d => getEdgeStyle(d).width)
    .attr('stroke', d => getEdgeStyle(d).color)
    .on('click', (event, d) => showDetail('edge', d))

  // 计算节点位置（层次布局）
  const nodeSpacing = 100
  const levelSpacing = 80
  
  nodes.value.forEach((node, index) => {
    node.x = (index % 5) * nodeSpacing + 100
    node.y = Math.floor(index / 5) * levelSpacing + 100
  })

  // 画点
  const node = svg.append('g')
    .selectAll('circle')
    .data(nodes.value)
    .join('circle')
    .attr('r', d => getNodeStyle(d).size)
    .attr('fill', d => getNodeStyle(d).color)
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .attr('cx', d => d.x)
    .attr('cy', d => d.y)
    .call(dragHierarchy)
    .on('click', (event, d) => showDetail('node', d))

  // 点标签
  svg.append('g')
    .selectAll('text')
    .data(nodes.value)
    .join('text')
    .attr('text-anchor', 'middle')
    .attr('dy', 5)
    .attr('font-size', 14)
    .attr('fill', '#fff')
    .attr('x', d => d.x)
    .attr('y', d => d.y)
    .text(d => d.label)
  
  // 更新连线位置
  link
    .attr('x1', d => {
      const sourceNode = nodes.value.find(n => n.id === (d.source.id || d.source))
      return sourceNode ? sourceNode.x : 0
    })
    .attr('y1', d => {
      const sourceNode = nodes.value.find(n => n.id === (d.source.id || d.source))
      return sourceNode ? sourceNode.y : 0
    })
    .attr('x2', d => {
      const targetNode = nodes.value.find(n => n.id === (d.target.id || d.target))
      return targetNode ? targetNode.x : 0
    })
    .attr('y2', d => {
      const targetNode = nodes.value.find(n => n.id === (d.target.id || d.target))
      return targetNode ? targetNode.y : 0
    })
}

// 层次布局的拖拽处理
function dragHierarchy() {
  function dragstarted(event, d) {
    d.fx = d.x
    d.fy = d.y
  }
  function dragged(event, d) {
    d.fx = event.x
    d.fy = event.y
  }
  function dragended(event, d) {
    d.fx = null
    d.fy = null
  }
  return d3.drag()
    .on('start', dragstarted)
    .on('drag', dragged)
    .on('end', dragended)
}

function drag(simulation) {
  function dragstarted(event, d) {
    if (!event.active) simulation.alphaTarget(0.3).restart()
    d.fx = d.x
    d.fy = d.y
  }
  function dragged(event, d) {
    d.fx = event.x
    d.fy = event.y
  }
  function dragended(event, d) {
    if (!event.active) simulation.alphaTarget(0)
    d.fx = null
    d.fy = null
  }
  return d3.drag()
    .on('start', dragstarted)
    .on('drag', dragged)
    .on('end', dragended)
}

onMounted(() => {
  fetchGraphList()
})
</script>

<style scoped>
.graph-visual-container {
  padding: 20px;
}
.visual-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 10px;
}
.visual-area {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.08);
  padding: 16px;
  min-height: 520px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.type-style-item {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.style-drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid #eee;
  background: #fff;
}
</style>