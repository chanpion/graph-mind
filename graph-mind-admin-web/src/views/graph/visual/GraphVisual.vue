<template>
  <div class="graph-visual-container">
    <div class="visual-toolbar">
      <el-select v-model="selectedGraphId" placeholder="请选择图" style="width: 200px" @change="handleGraphChange">
        <el-option v-for="g in graphList" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-input v-model="query" placeholder="请输入查询语句" style="width: 400px; margin: 0 12px;" />
      <el-button type="primary" @click="handleQuery">查询</el-button>
    </div>
    <div class="visual-area">
      <svg ref="svgRef" :width="width" :height="height"></svg>
    </div>
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
  </div>
</template>

<script setup name="GraphVisual">
import { ref, onMounted } from 'vue'
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
  // 力导向布局
  const simulation = d3.forceSimulation(nodes.value)
    .force('link', d3.forceLink(edges.value).id(d => d.id).distance(120))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(width / 2, height / 2))

  // 画边
  const link = svg.append('g')
    .attr('stroke', '#aaa')
    .selectAll('line')
    .data(edges.value)
    .join('line')
    .attr('stroke-width', 2)
    .on('click', (event, d) => showDetail('edge', d))

  // 画点
  const node = svg.append('g')
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .selectAll('circle')
    .data(nodes.value)
    .join('circle')
    .attr('r', 22)
    .attr('fill', '#409EFF')
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
</style> 