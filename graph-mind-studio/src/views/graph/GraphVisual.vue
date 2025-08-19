<template>
  <div class="graph-visual-container">
    <div class="visual-toolbar">
      <el-select v-model="selectedGraphId" placeholder="请选择图" style="width: 200px" @change="handleGraphChange">
        <el-option v-for="g in graphList" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-input v-model="query" placeholder="请输入查询语句" style="width: 400px; margin: 0 12px;" />
      <el-button type="primary" @click="handleQuery" :loading="queryLoading">查询</el-button>
      
      <!-- 添加布局切换控件 -->
      <el-select v-model="layoutType" placeholder="选择布局" style="width: 120px; margin-left: 12px;" @change="handleLayoutChange">
        <el-option label="力导向图" value="force" />
        <el-option label="层次布局" value="hierarchy" />
      </el-select>

      <!-- 添加样式设置按钮 -->
      <el-button type="primary" @click="styleDrawerVisible = true" style="margin-left: 12px;">样式设置</el-button>
      
    </div>

    
    <!-- 右键菜单 -->
    <div v-if="contextMenuVisible" class="context-menu" :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }">
      <ul>
        <li @click="handleExpandNode">展开节点</li>
        <li @click="handleCollapseNode">收起节点</li>
        <li @click="handleDeleteNode">删除节点</li>
        <li @click="handleFindPath">查找路径</li>
      </ul>
    </div>
          
    <!-- 节点/关系详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" :title="detailType==='node' ? '点详情' : '边详情'" direction="rtl" size="350px">
      <div v-if="detailType==='node'">
        <div><b>UID：</b>{{ detailData.uid }}</div>
        <div><b>类型：</b>{{ detailData.label }}</div>
        <div v-if="detailData.properties">
          <b>属性：</b>
          <el-table :data="formatProperties(detailData.properties)" size="small" border style="margin-top: 8px;">
            <el-table-column prop="name" label="属性名" />
            <el-table-column prop="value" label="属性值" />
          </el-table>
        </div>
      </div>
      <div v-else>
        <div><b>UID：</b>{{ detailData.uid }}</div>
        <div><b>类型：</b>{{ detailData.label }}</div>
        <div><b>起点UID：</b>{{ detailData.startUid }}</div>
        <div><b>起点类型：</b>{{ detailData.startLabel }}</div>
        <div><b>终点UID：</b>{{ detailData.endUid }}</div>
        <div><b>终点类型：</b>{{ detailData.endLabel }}</div>
        <div v-if="detailData.properties">
          <b>属性：</b>
          <el-table :data="formatProperties(detailData.properties)" size="small" border style="margin-top: 8px;">
            <el-table-column prop="name" label="属性名" />
            <el-table-column prop="value" label="属性值" />
          </el-table>
        </div>
      </div>
    </el-drawer>

    
    <div class="visual-area">
      <!-- 添加画布loading遮罩 -->
      <div v-if="canvasLoading" class="canvas-loading">
        <el-icon class="is-loading" style="font-size: 36px; color: #409eff;">
          <Loading />
        </el-icon>
        <p style="margin-top: 10px; color: #666;">数据加载中...</p>
      </div>
      
      <svg ref="svgRef" :width="width" :height="height"></svg>
      
      <!-- 添加居中按钮 -->
      <el-button 
        class="center-button"
        circle 
        size="small" 
        @click="centerGraph"
        title="居中显示"
      >
        <el-icon><Aim /></el-icon>
      </el-button>
      
      <!-- 缩放控制按钮，置于画布右下角 -->
      <div class="zoom-controls">
        <el-button 
          circle 
          size="small" 
          @click="zoomIn"
          title="放大"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button 
          circle 
          size="small" 
          @click="zoomOut"
          title="缩小"
        >
          <el-icon><Minus /></el-icon>
        </el-button>
        <el-button 
          circle 
          size="small" 
          @click="resetZoom"
          title="重置"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>
    
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
          </el-form>        </el-tab-pane>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Minus, Refresh, Loading, Aim } from '@element-plus/icons-vue'

// 添加缩放相关引用
const zoom = ref(null)
const gRef = ref(null)

// 添加右键菜单相关引用
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const contextMenuData = ref(null)
const contextMenuType = ref('node') // node or edge

// 预定义颜色列表，用于不同类型节点和边的颜色区分
const predefinedColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'
]

const width = 800
const height = 500
const svgRef = ref(null)

const graphList = ref([])
const selectedGraphId = ref()
const query = ref('MATCH (n)-[r]->(m) RETURN n,r,m')
const nodes = ref([])
const edges = ref([])

// 添加查询加载状态
const queryLoading = ref(false)
const canvasLoading = ref(false)

// 添加图类型到查询语句的映射
const graphTypeQueryMap = {
  'neo4j': 'MATCH (n)-[r]->(m) RETURN n,r,m LIMIT 25',
  'nebula': 'MATCH ()-[e]->() RETURN e LIMIT 25',
  'janus': 'g.V().outE().inV().path()'
}

// 存储当前选中图的信息
const selectedGraph = ref(null)
// 存储连接信息
const graphConnections = ref(new Map())

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
const detailType = ref('node') // node or edge
const detailData = ref({})

const fetchGraphList = async () => {
  try {
    const res = await graphApi.getGraphs()
    // 根据API返回的数据结构正确设置graphList
    if (res.data.records) {
      graphList.value = res.data.records
    } else if (res.data.list) {
      graphList.value = res.data.list
    } else {
      graphList.value = res.data
    }
  } catch (e) {
    ElMessage.error('获取图列表失败')
  }
}

const handleGraphChange = async (graphId) => {
  // 查找选中的图信息
  selectedGraph.value = graphList.value.find(graph => graph.id === graphId) || null
  // 根据图的数据库类型设置示例查询语句
  if (selectedGraph.value && selectedGraph.value.connectionId) {
    const graphType = selectedGraph.value.graphType
    query.value = graphTypeQueryMap[selectedGraph.value.graphType] || graphTypeQueryMap.neo4j
  } else {
    // 如果没有选中图，使用默认查询语句
    query.value = 'MATCH (n)-[r]->(m) RETURN n,r,m LIMIT 25'
  }
  
  nodes.value = []
  edges.value = []
  clearSvg()
}

const handleQuery = async () => {
  if (!selectedGraphId.value) {
    ElMessage.warning('请选择图')
    return
  }
  
  // 设置加载状态
  queryLoading.value = true
  canvasLoading.value = true
  
  // mock查询接口
  try {
    const res = await graphApi.queryGraph(selectedGraphId.value, query.value)
    // 处理返回的数据，确保边的source和target指向节点对象而不是ID
    const vertices = res.data.vertices || []
    const edgesData = res.data.edges || []
    
    // 转换节点数据格式以适配可视化
    nodes.value = vertices.map(vertex => ({
      id: vertex.uid, // 使用uid作为节点ID
      uid: vertex.uid,
      label: vertex.label,
      properties: vertex.properties,
      x: 0,
      y: 0
    }))
    
    // 转换边数据格式以适配可视化
    edges.value = edgesData.map(edge => ({
      id: edge.uid,
      uid: edge.uid,
      label: edge.label,
      source: edge.startUid, // 使用startUid作为source
      target: edge.endUid,   // 使用endUid作为target
      startUid: edge.startUid,
      startLabel: edge.startLabel,
      endUid: edge.endUid,
      endLabel: edge.endLabel,
      properties: edge.properties
    }))
    
    // 处理边数据，确保source和target引用节点对象
    processEdgesData()
    
    // 自动为不同类型的节点和边分配颜色
    autoAssignColors()
    
    drawGraph()
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.message || '未知错误'))
    console.error('查询失败:', e)
  } finally {
    // 无论成功或失败都取消加载状态
    queryLoading.value = false
    canvasLoading.value = false
  }
}

// 自动为不同类型的节点和边分配颜色
const autoAssignColors = () => {
  // 为节点类型分配颜色
  const nodeTypeSet = new Set()
  nodes.value.forEach(node => {
    if (node.label) {
      nodeTypeSet.add(node.label)
    }
  })
  
  const nodeTypesArray = Array.from(nodeTypeSet)
  nodeStyleConfig.value.typeStyles = nodeTypesArray.map((type, index) => ({
    type: type,
    color: predefinedColors[index % predefinedColors.length],
    size: nodeStyleConfig.value.defaultSize
  }))
  
  // 为边类型分配颜色
  const edgeTypeSet = new Set()
  edges.value.forEach(edge => {
    if (edge.label) {
      edgeTypeSet.add(edge.label)
    }
  })
  
  const edgeTypesArray = Array.from(edgeTypeSet)
  edgeStyleConfig.value.typeStyles = edgeTypesArray.map((type, index) => ({
    type: type,
    color: predefinedColors[(index + 5) % predefinedColors.length], // 使用不同的颜色组
    width: edgeStyleConfig.value.defaultWidth
  }))
}

// 处理边数据，确保source和target引用节点对象而不是ID
const processEdgesData = () => {
  // 创建节点ID到节点对象的映射
  const nodeMap = {}
  nodes.value.forEach(node => {
    nodeMap[node.id] = node
  })
  
  // 更新边的source和target引用
  edges.value.forEach(edge => {
    // 如果source是字符串ID，则替换为节点对象
    if (typeof edge.source === 'string') {
      edge.source = nodeMap[edge.source] || edge.source
    }
    
    // 如果target是字符串ID，则替换为节点对象
    if (typeof edge.target === 'string') {
      edge.target = nodeMap[edge.target] || edge.target
    }
  })
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

// 显示右键菜单
const showContextMenu = (type, data, event) => {
  event.preventDefault() // 阻止默认右键菜单
  event.stopPropagation() // 阻止事件冒泡
  
  contextMenuType.value = type
  contextMenuData.value = data
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
  
  // 点击其他地方隐藏菜单
  const handleClick = (e) => {
    // 检查点击的元素是否在菜单内部
    const menu = document.querySelector('.context-menu')
    if (menu && !menu.contains(e.target)) {
      contextMenuVisible.value = false
      document.removeEventListener('click', handleClick)
    }
  }
  
  // 按ESC键隐藏菜单
  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      contextMenuVisible.value = false
      document.removeEventListener('keydown', handleKeyDown)
    }
  }
  
  // 延迟添加点击监听，避免立即触发
  setTimeout(() => {
    document.addEventListener('click', handleClick)
    document.addEventListener('keydown', handleKeyDown)
  }, 100)
}

// 菜单操作处理函数
const handleExpandNode = async () => {
  contextMenuVisible.value = false
  if (!selectedGraphId.value) {
    ElMessage.warning('请选择图')
    return
  }
  
  if (!contextMenuData.value || !contextMenuData.value.uid) {
    ElMessage.warning('无法获取节点信息')
    return
  }
  
  try {
    // 显示加载状态
    canvasLoading.value = true
    
    // 调用展开节点API
    const res = await graphApi.expandNode(selectedGraphId.value, contextMenuData.value.uid, 1)
    
    // 处理返回的数据
    const vertices = res.data.vertices || []
    const edgesData = res.data.edges || []
    
    // 合并新数据到现有数据中
    // 添加新节点（避免重复添加）
    const existingNodeIds = new Set(nodes.value.map(node => node.uid))
    vertices.forEach(vertex => {
      if (!existingNodeIds.has(vertex.uid)) {
        nodes.value.push({
          id: vertex.uid,
          uid: vertex.uid,
          label: vertex.label,
          properties: vertex.properties,
          x: 0,
          y: 0
        })
      }
    })
    
    // 添加新边（避免重复添加）
    const existingEdgeIds = new Set(edges.value.map(edge => edge.uid))
    edgesData.forEach(edge => {
      if (!existingEdgeIds.has(edge.uid)) {
        edges.value.push({
          id: edge.uid,
          uid: edge.uid,
          label: edge.label,
          source: edge.startUid,
          target: edge.endUid,
          startUid: edge.startUid,
          startLabel: edge.startLabel,
          endUid: edge.endUid,
          endLabel: edge.endLabel,
          properties: edge.properties
        })
      }
    })
    
    // 处理边数据，确保source和target引用节点对象
    processEdgesData()
    
    // 自动为新类型分配颜色
    autoAssignColors()
    
    // 重新绘制图形
    drawGraph()
    
    ElMessage.success('节点展开成功')
  } catch (e) {
    ElMessage.error('展开节点失败: ' + (e.message || '未知错误'))
    console.error('展开节点失败:', e)
  } finally {
    canvasLoading.value = false
  }
}

const handleCollapseNode = () => {
  // 收起节点功能
  ElMessage.info('收起节点功能待实现')
  contextMenuVisible.value = false
}

const handleDeleteNode = () => {
  // 删除节点功能
  ElMessageBox.confirm('确认删除该节点吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    if (!selectedGraphId.value) {
      ElMessage.warning('请选择图')
      return
    }
    
    if (!contextMenuData.value || !contextMenuData.value.uid) {
      ElMessage.warning('无法获取节点信息')
      return
    }
    
    try {
      // 调用删除节点API
      await graphApi.deleteNode(selectedGraphId.value, contextMenuData.value.uid, contextMenuData.value.label)
      
      // 从本地数据中移除节点
      const nodeUid = contextMenuData.value.uid
      nodes.value = nodes.value.filter(node => node.uid !== nodeUid)
      
      // 同时移除与该节点相关的边
      edges.value = edges.value.filter(edge => 
        edge.startUid !== nodeUid && edge.endUid !== nodeUid
      )
      
      // 重新绘制图形
      drawGraph()
      
      ElMessage.success('删除成功')
    } catch (e) {
      ElMessage.error('删除失败: ' + (e.message || '未知错误'))
      console.error('删除节点失败:', e)
    }
  }).catch(() => {
    // 用户取消删除
  })
  contextMenuVisible.value = false
}

const handleFindPath = async () => {
  contextMenuVisible.value = false
  if (!selectedGraphId.value) {
    ElMessage.warning('请选择图')
    return
  }
  
  if (!contextMenuData.value || !contextMenuData.value.uid) {
    ElMessage.warning('无法获取节点信息')
    return
  }
  
  // 这里应该弹出一个对话框让用户选择目标节点
  // 为简化实现，我们使用一个示例目标节点
  ElMessageBox.prompt('请输入目标节点UID', '查找路径', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S/,
    inputErrorMessage: '节点UID不能为空'
  }).then(async ({ value }) => {
    try {
      // 显示加载状态
      canvasLoading.value = true
      
      // 调用查找路径API
      const res = await graphApi.findPath(selectedGraphId.value, contextMenuData.value.uid, value, 5)
      
      // 处理返回的数据
      const vertices = res.data.vertices || []
      const edgesData = res.data.edges || []
      
      // 清空现有数据
      nodes.value = []
      edges.value = []
      
      // 添加路径数据
      vertices.forEach(vertex => {
        nodes.value.push({
          id: vertex.uid,
          uid: vertex.uid,
          label: vertex.label,
          properties: vertex.properties,
          x: 0,
          y: 0
        })
      })
      
      edgesData.forEach(edge => {
        edges.value.push({
          id: edge.uid,
          uid: edge.uid,
          label: edge.label,
          source: edge.startUid,
          target: edge.endUid,
          startUid: edge.startUid,
          startLabel: edge.startLabel,
          endUid: edge.endUid,
          endLabel: edge.endLabel,
          properties: edge.properties
        })
      })
      
      // 处理边数据，确保source和target引用节点对象
      processEdgesData()
      
      // 自动为新类型分配颜色
      autoAssignColors()
      
      // 重新绘制图形
      drawGraph()
      
      ElMessage.success('路径查找成功')
    } catch (e) {
      ElMessage.error('查找路径失败: ' + (e.message || '未知错误'))
      console.error('查找路径失败:', e)
    } finally {
      canvasLoading.value = false
    }
  }).catch(() => {
    // 用户取消输入
  })
}

// 添加缩放功能
const zoomIn = () => {
  if (zoom.value && gRef.value) {
    zoom.value.scaleBy(d3.select(svgRef.value).select('g'), 1.2)
  }
}

const zoomOut = () => {
  if (zoom.value && gRef.value) {
    zoom.value.scaleBy(d3.select(svgRef.value).select('g'), 0.8)
  }
}

const resetZoom = () => {
  if (zoom.value && gRef.value) {
    zoom.value.scaleTo(d3.select(svgRef.value).select('g'), 1)
  }
}
  
// 添加居中功能
const centerGraph = () => {
  if (zoom.value && gRef.value && nodes.value.length > 0) {
    // 计算所有节点的中心点
    const xExtent = d3.extent(nodes.value, d => d.x)
    const yExtent = d3.extent(nodes.value, d => d.y)
    const centerX = (xExtent[0] + xExtent[1]) / 2
    const centerY = (yExtent[0] + yExtent[1]) / 2
      
    // 获取SVG的实际尺寸
    const svg = d3.select(svgRef.value)
    const actualWidth = svg.property('clientWidth')
    const actualHeight = svg.property('clientHeight')
      
    // 计算缩放比例
    const scale = Math.min(actualWidth / (xExtent[1] - xExtent[0] + 100), actualHeight / (yExtent[1] - yExtent[0] + 100))
      
    // 设置转换
    zoom.value.transform(
      d3.select(gRef.value),
      d3.zoomIdentity
        .translate(actualWidth / 2 - centerX * scale, actualHeight / 2 - centerY * scale)
        .scale(scale)
    )
  }
}

const drawGraph = () => {
  clearSvg()
  const svg = d3.select(svgRef.value)
  if (!nodes.value.length && !edges.value.length) return
  
  // 创建缩放行为
  zoom.value = d3.zoom()
    .scaleExtent([0.1, 10]) // 设置缩放范围
    .on('zoom', (event) => {
      gRef.value.attr('transform', event.transform)
    })
  
  // 应用缩放行为到svg
  svg.call(zoom.value)
  
  // 创建一个g元素用于包含所有图形元素，便于缩放
  gRef.value = svg.append('g')
  
  if (layoutType.value === 'force') {
    drawForceLayout(gRef.value)
  } else {
    drawHierarchyLayout(gRef.value)
  }
}

// 格式化属性数据用于表格展示
const formatProperties = (properties) => {
  if (!properties) return []
  return Object.keys(properties).map(key => ({
    name: key,
    value: properties[key]
  }))
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
const drawForceLayout = (container) => {
  // 获取SVG的实际宽度和高度
  const svg = container.node() ? container.node().ownerSVGElement : null
  const actualWidth = svg ? svg.clientWidth : width
  const actualHeight = svg ? svg.clientHeight : height
  
  // 定义箭头标记
  const defs = container.append("defs")
  
  // 为节点间连线定义箭头
  defs.append("marker")
    .attr("id", "arrow")
    .attr("viewBox", "-10 -5 10 10")
    .attr("refX", -15) // 调整箭头位置，使其接触目标节点边缘
    .attr("refY", 0)
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("path")
    .attr("d", "M -10 -5 L 0 0 L -10 5")
    .attr("fill", "#999")

  // 力导向布局
  const simulation = d3.forceSimulation(nodes.value)
    .force('link', d3.forceLink(edges.value).id(d => d.id).distance(120))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(width / 2, height / 2))

  // 画边（仅当有边时才绘制）
  let link = null;
  let linkText = null;
  
  if (edges.value.length > 0) {
    link = container.append('g')
      .selectAll('line')
      .data(edges.value)
      .join('line')
      .attr('stroke-width', d => getEdgeStyle(d).width)
      .attr('stroke', d => getEdgeStyle(d).color)
      .attr('cursor', 'pointer') // 鼠标悬停时显示手型光标
      .attr("marker-end", "url(#arrow)") // 添加箭头
      .on('click', (event, d) => showDetail('edge', d))

    // 画边标签
    linkText = container.append('g')
      .selectAll('text')
      .data(edges.value)
      .join('text')
      .attr('text-anchor', 'middle')
      .attr('dy', -5)
      .attr('font-size', 10)
      .attr('fill', '#666')
      .attr('cursor', 'pointer') // 鼠标悬停时显示手型光标
      .text(d => d.label || '') // 显示边的类型label
  }

  // 画点
  const node = container.append('g')
    .selectAll('circle')
    .data(nodes.value)
    .join('circle')
    .attr('r', d => getNodeStyle(d).size)
    .attr('fill', d => getNodeStyle(d).color)
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .attr('cx', d => d.x)
    .attr('cy', d => d.y)
    .attr('cursor', 'pointer') // 鼠标悬停时显示手型光标
    .call(drag(simulation))
    .on('click', (event, d) => showDetail('node', d)) // 确保节点点击事件正确绑定
    .on('contextmenu', (event, d) => showContextMenu('node', d, event)) // 添加右键菜单事件

  // 点标签
  const text = container.append('g')
    .selectAll('text')
    .data(nodes.value)
    .join('text')
    .attr('text-anchor', 'middle')
    .attr('dy', 5)
    .attr('font-size', 14)
    .attr('fill', '#fff')
    .attr('x', d => d.x)
    .attr('y', d => d.y)
    .attr('cursor', 'pointer') // 鼠标悬停时显示手型光标
    .text(d => d.uid || '') // 显示节点的uid
    .on('click', (event, d) => {
      // 阻止标签点击事件冒泡，避免与节点点击事件冲突
      event.stopPropagation()
      showDetail('node', d)
    })
    
  simulation.on('tick', () => {
    // 只有当有边时才更新连线位置
    if (link && linkText) {
      link
        .attr('x1', d => {
          // 处理source可能是ID字符串的情况
          const source = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
          return source ? source.x : 0
        })
        .attr('y1', d => {
          const source = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
          return source ? source.y : 0
        })
        .attr('x2', d => {
          // 处理target可能是ID字符串的情况
          const target = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
          return target ? target.x : 0
        })
        .attr('y2', d => {
          const target = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
          return target ? target.y : 0
        })
      
      // 更新边标签位置
      linkText
        .attr('x', d => {
          const source = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
          const target = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
          return source && target ? (source.x + target.x) / 2 : 0
        })
        .attr('y', d => {
          const source = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
          const target = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
          return source && target ? (source.y + target.y) / 2 : 0
        })
    }
    
    node
      .attr('cx', d => d.x)
      .attr('cy', d => d.y)
    text
      .attr('x', d => d.x)
      .attr('y', d => d.y)
  })
}

// 层次布局
const drawHierarchyLayout = (container) => {
  // 获取SVG的实际宽度和高度
  const svg = container.node() ? container.node().ownerSVGElement : null
  const actualWidth = svg ? svg.clientWidth : width
  const actualHeight = svg ? svg.clientHeight : height
  
  // 创建层级结构（简单处理：以第一个节点为根节点）
  if (nodes.value.length === 0 && edges.value.length === 0) return
  
  // 定义箭头标记
  const defs = container.append("defs")
  
  // 为节点间连线定义箭头
  defs.append("marker")
    .attr("id", "arrow-hierarchy")
    .attr("viewBox", "-10 -5 10 10")
    .attr("refX", -15) // 调整箭头位置，使其接触目标节点边缘
    .attr("refY", 0)
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("path")
    .attr("d", "M -10 -5 L 0 0 L -10 5")
    .attr("fill", "#999")
  
  // 计算节点位置（层次布局）
  const nodeSpacing = 100
  const levelSpacing = 80
  
  nodes.value.forEach((node, index) => {
    node.x = (index % 5) * nodeSpacing + 100
    node.y = Math.floor(index / 5) * levelSpacing + 100
  })

  // 画边（仅当有边时才绘制）
  let link = null;
  let linkText = null;
  
  if (edges.value.length > 0) {
    link = container.append('g')
      .selectAll('line')
      .data(edges.value)
      .join('line')
      .attr('stroke-width', d => getEdgeStyle(d).width)
      .attr('stroke', d => getEdgeStyle(d).color)
      .attr('cursor', 'pointer') // 鼠标悬停时显示手型光标
      .attr("marker-end", "url(#arrow-hierarchy)") // 添加箭头
      .on('click', (event, d) => showDetail('edge', d))

    // 画边标签
    linkText = container.append('g')
      .selectAll('text')
      .data(edges.value)
      .join('text')
      .attr('text-anchor', 'middle')
      .attr('dy', -5)
      .attr('font-size', 10)
      .attr('fill', '#666')
      .text(d => d.label || '') // 显示边的类型label
  }

  // 画点
  const node = container.append('g')
    .selectAll('circle')
    .data(nodes.value)
    .join('circle')
    .attr('r', d => getNodeStyle(d).size)
    .attr('fill', d => getNodeStyle(d).color)
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .attr('cx', d => d.x)
    .attr('cy', d => d.y)
    .attr('cursor', 'pointer') // 鼠标悬停时显示手型光标
    .call(dragHierarchy)
    .on('click', (event, d) => showDetail('node', d)) // 确保节点点击事件正确绑定
    .on('contextmenu', (event, d) => showContextMenu('node', d, event)) // 添加右键菜单事件

  // 点标签
  const text = container.append('g')
    .selectAll('text')
    .data(nodes.value)
    .join('text')
    .attr('text-anchor', 'middle')
    .attr('dy', 5)
    .attr('font-size', 14)
    .attr('fill', '#fff')
    .attr('x', d => d.x)
    .attr('y', d => d.y)
    .text(d => d.uid || '') // 显示节点的uid
    .on('click', (event, d) => {
      // 阻止标签点击事件冒泡，避免与节点点击事件冲突
      event.stopPropagation()
      showDetail('node', d)
    })
  
  // 只有当有边时才更新连线位置
  if (link && linkText) {
    // 更新连线位置
    link
      .attr('x1', d => {
        const sourceNode = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
        return sourceNode ? sourceNode.x : 0
      })
      .attr('y1', d => {
        const sourceNode = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
        return sourceNode ? sourceNode.y : 0
      })
      .attr('x2', d => {
        const targetNode = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
        return targetNode ? targetNode.x : 0
      })
      .attr('y2', d => {
        const targetNode = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
        return targetNode ? targetNode.y : 0
      })
    
    // 更新边标签位置
    linkText
      .attr('x', d => {
        const sourceNode = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
        const targetNode = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
        return sourceNode && targetNode ? (sourceNode.x + targetNode.x) / 2 : 0
      })
      .attr('y', d => {
        const sourceNode = typeof d.source === 'object' ? d.source : nodes.value.find(n => n.id === d.source)
        const targetNode = typeof d.target === 'object' ? d.target : nodes.value.find(n => n.id === d.target)
        return sourceNode && targetNode ? (sourceNode.y + targetNode.y) / 2 : 0
      })
  }
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

// 添加hasInitialized响应式变量
const hasInitialized = ref(false)
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
  position: relative;
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

/* 右键菜单样式 */
.context-menu {
  position: fixed; /* 改为fixed确保相对于视窗定位 */
  background: #fff;
  border: 1px solid #eee;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  padding: 8px 0;
  z-index: 9999; /* 提高z-index确保在最上层 */
  font-size: 14px;
}

.context-menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.context-menu li {
  padding: 8px 16px;
  cursor: pointer;
}

.context-menu li:hover {
  background: #f5f7fa;
}

/* 缩放控制按钮样式 */
.zoom-controls {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 10;
}

.zoom-controls .el-button {
  margin: 0;
}

/* 画布loading遮罩样式 */
.canvas-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 100;
  border-radius: 8px;
}

/* SVG容器样式 */
.graph-svg-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}
</style>