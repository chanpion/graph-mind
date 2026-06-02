<template>
  <div class="graph-analysis-container">

    <el-row :gutter="0" class="main-content analysis-layout">
      <!-- 左侧操作区 -->
      <el-col style="flex: 0 0 300px; max-width: 300px;" class="operation-panel">
        <!-- 侧边栏头部 -->
        <div class="sidebar-header">
          <div class="sidebar-header-top">
            <h3>图分析</h3>
          </div>
        </div>
        <div class="operation-content">
          <!-- 算法分类Tab -->
          <el-tabs v-model="activeAlgorithmTab" class="algorithm-tabs">
            <!-- K层展开Tab -->
            <el-tab-pane label="K层展开" name="kLayerExpand">
              <el-form
                  ref="analysisFormRef"
                  :model="analysisForm"
                  :rules="analysisRules"
                  label-position="top"
              >
                <!-- 目标实体 -->
                <el-form-item label="目标实体" prop="targetEntity">
                  <el-cascader
                    v-model="analysisForm.targetEntity"
                    placeholder="请选择目标实体类型和属性"
                    style="width: 100%"
                    :options="entityOptions"
                    @change="handleEntityChange"
                  />
                </el-form-item>

                <el-form-item  prop="queryValue">
                  <el-input v-model="analysisForm.queryValue" placeholder="请输入查询值"/>
                </el-form-item>

                <!-- 拓展配置 -->
                <el-collapse v-model="expandConfigVisible" accordion>
                  <el-collapse-item title="拓展配置" name="1">
                    <div class="config-item">
                      <el-form-item label="返回最大路径数：" prop="maxPaths">
                        <el-input-number
                            v-model="analysisForm.maxPaths"
                            :min="1"
                            :max="10000"
                            style="width: 100%"
                        />
                      </el-form-item>

                      <el-form-item label="拓展层数：" prop="layers">
                        <el-input-number
                            v-model="analysisForm.layers"
                            :min="1"
                            :max="10"
                            style="width: 100%"
                        />
                      </el-form-item>

                      <el-form-item label="拓展实体：" prop="expandEntities">
                        <el-select
                            v-model="analysisForm.expandEntities"
                            multiple
                            placeholder="选择拓展实体"
                            style="width: 100%"
                        >
                          <el-option label="全部" value="all"/>
                        </el-select>
                      </el-form-item>

                      <el-form-item label="拓展关系：" prop="expandRelations">
                        <el-select
                            v-model="analysisForm.expandRelations"
                            multiple
                            placeholder="选择拓展关系"
                            style="width: 100%"
                        >
                          <el-option label="全部" value="all"/>
                        </el-select>
                      </el-form-item>
                    </div>
                  </el-collapse-item>
                </el-collapse>

                <!-- 执行按钮 -->
                <el-form-item>
                  <el-button
                      type="primary"
                      style="width: 100%"
                      @click="executeAnalysis"
                      :loading="analysisLoading"
                      :disabled="isExecuteButtonDisabled"
                  >
                    执行分析
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 路径查询Tab -->
            <el-tab-pane label="路径查询" name="pathQuery">
              <el-form
                  ref="analysisFormRef"
                  :model="analysisForm"
                  :rules="analysisRules"
                  label-position="top"
              >
                <!-- 起点 -->
                <el-form-item label="起点" prop="sourceEntity">
                  <el-cascader
                    v-model="analysisForm.sourceEntity"
                    placeholder="请选择起点实体类型和属性"
                    style="width: 100%"
                    :options="entityOptions"
                    @change="handleSourceEntityChange"
                  />
                </el-form-item>
                
                <el-form-item label="" prop="sourceValue">
                  <el-input v-model="analysisForm.sourceValue" placeholder="请输入起点值"/>
                </el-form-item>

                <!-- 终点 -->
                <el-form-item label="终点" prop="targetEntity">
                  <el-cascader
                    v-model="analysisForm.targetEntity"
                    placeholder="请选择终点实体类型和属性"
                    style="width: 100%"
                    :options="entityOptions"
                    @change="handleTargetEntityChange"
                  />
                </el-form-item>
                
                <el-form-item label="" prop="targetValue">
                  <el-input v-model="analysisForm.targetValue" placeholder="请输入终点值"/>
                </el-form-item>

                <!-- 路径长度 -->
                <el-form-item label="最大路径长度" prop="maxLength">
                  <el-input-number
                      v-model="analysisForm.maxLength"
                      :min="1"
                      :max="100"
                      placeholder="请输入最大路径长度"
                      style="width: 100%"
                  />
                </el-form-item>

                <!-- 其他相关参数配置 -->
                <!-- 根据需求添加其他参数配置 -->

                <!-- 执行按钮 -->
                <el-form-item>
                  <el-button
                      type="primary"
                      style="width: 100%"
                      @click="executePathQuery"
                      :loading="analysisLoading"
                      :disabled="isPathQueryButtonDisabled"
                  >
                    开始探索
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

          </el-tabs>

          <!-- 分析结果展示 -->
          <div v-if="analysisResult && !analysisLoading && (activeAlgorithmTab === 'pathQuery' || analysisForm.algorithm === 'shortestPath')" class="result-section">
            <el-divider>分析结果</el-divider>
            <div class="result-content">
              <template v-if="activeAlgorithmTab === 'pathQuery' || analysisForm.algorithm === 'shortestPath'">
                <h4>最短路径</h4>
                <p>路径长度: {{ analysisResult.pathLength }}</p>
                <p>路径节点数: {{ analysisResult.nodeCount }}</p>
                <el-button
                    type="primary"
                    size="small"
                    @click="highlightPath"
                    v-if="analysisResult.path && analysisResult.path.length > 0"
                >
                  高亮显示路径
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧图可视化区 -->
      <el-col style="flex: 1;" class="visualization-panel">
        <div class="visualization-content">
          <!-- 图可视化区域 -->
          <div ref="graphContainerRef" class="viz-canvas-container">
            <div class="graph-container">
              <svg ref="svgRef" class="graph-svg"></svg>
              
              <!-- 画布工具栏 -->
              <div class="canvas-toolbar">
                <el-tag size="small" :type="graphTypeTagType" effect="dark" v-if="graphTypeLabel">
                  {{ graphTypeLabel }}
                </el-tag>
                <div class="toolbar-divider"></div>
                <el-button-group size="small">
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
              <div v-if="legendItems.nodes.length > 0 || legendItems.edges.length > 0" class="canvas-legend">
                <div class="legend-title">图例</div>
                <div class="legend-items">
                  <div class="legend-group" v-if="legendItems.nodes.length > 0">
                    <div class="legend-group-title">节点</div>
                    <div v-for="item in legendItems.nodes" :key="'node-'+item.label" class="legend-item">
                      <span class="legend-node-dot" :style="{ background: item.color }"></span>
                      <span class="legend-label">{{ item.label }}</span>
                    </div>
                  </div>
                  <div class="legend-group" v-if="legendItems.edges.length > 0">
                    <div class="legend-group-title">边</div>
                    <div v-for="item in legendItems.edges" :key="'edge-'+item.label" class="legend-item">
                      <span class="legend-edge-line" :style="{ background: item.color, borderColor: item.color }"></span>
                      <span class="legend-label">{{ item.label }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 点边统计 -->
              <div v-if="nodes.length > 0" class="canvas-stats">
                <div class="stat-item">
                  <span class="stat-label">节点</span>
                  <span class="stat-value">{{ nodes.length }}</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-label">边</span>
                  <span class="stat-value">{{ edges.length }}</span>
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

              <!-- 加载提示 -->
              <div v-if="analysisLoading" class="loading-overlay">
                <div class="loading-content">
                  <el-icon class="is-loading" color="#409EFF" size="30">
                    <Loading/>
                  </el-icon>
                  <p>分析中...</p>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-else-if="!analysisLoading && nodes.length === 0" class="empty-state">
                <el-empty description="暂无图数据，请选择分析算法并执行分析"/>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import * as d3 from 'd3'
import {ElMessage} from 'element-plus'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import { Loading, ZoomIn, ZoomOut, FullScreen, Download, Close } from '@element-plus/icons-vue'
import { graphApi } from '@/views/graphs/api/graph'

// 响应式数据
const analysisFormRef = ref(null)
const graphContainerRef = ref(null)
const svgRef = ref(null)
const graphsStore = useGraphsStore()

// 图类型信息
const graphType = computed(() => graphsStore.currentGraph?.graphType || '')
const graphTypeLabel = computed(() => {
  const map = { neo4j: 'Neo4j', nebula: 'Nebula', janus: 'Janus' }
  return map[graphType.value] || graphType.value || '未知'
})
const graphTypeTagType = computed(() => {
  const map = { neo4j: 'success', nebula: 'warning', janus: 'danger' }
  return map[graphType.value] || 'info'
})
const defaultQuery = computed(() => {
  const map = {
    neo4j: 'MATCH p=(n)-[r]->() RETURN p LIMIT 10',
    nebula: 'MATCH p=(v)-[e]->(v2) RETURN p LIMIT 10',
    janus: 'g.V().limit(10)'
  }
  return map[graphType.value] || ''
})

// 图例数据
const legendItems = computed(() => {
  const legendNodes = []
  const legendEdges = []
  const nodeLabels = new Set()
  const edgeLabels = new Set()

  ;(nodes.value || []).forEach(n => {
    const label = n.label || 'Unknown'
    if (!nodeLabels.has(label)) {
      nodeLabels.add(label)
      legendNodes.push({ label, color: getNodeColor(label) })
    }
  })

  ;(edges.value || []).forEach(e => {
    const label = e.label || 'Unknown'
    if (!edgeLabels.has(label)) {
      edgeLabels.add(label)
      legendEdges.push({ label, color: getEdgeColor(label) })
    }
  })

  return { nodes: legendNodes, edges: legendEdges }
})

// 节点颜色映射
const NODE_COLORS = {
  center: '#e6a23c',
  layer1: '#409EFF',
  layer2: '#67c23a',
  path: '#f56c6c',
  normal: '#909399',
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

// 点类型和边类型
const vertexTypes = ref([])
const edgeTypes = ref([])

// 当前激活的算法tab
const activeAlgorithmTab = ref('kLayerExpand')

// 分析表单
const analysisForm = reactive({
  algorithm: '',
  sourceId: '',
  targetId: '',
  iterations: 10,
  dampingFactor: 0.85,
  resolution: 1.0,
  weakly: true,
  targetEntity: [], // 修改为数组以适应级联选择
  entityProperty: '',
  queryValue: '',
  maxPaths: 1000,
  layers: 2,
  expandEntities: ['all'],
  expandRelations: ['all'],
  // 路径查询相关字段
  sourceEntity: [],
  sourceValue: '',
  targetValue: '',
  maxLength: 10
})

// 详情面板相关
const detailDrawerVisible = ref(false)
const selectedElement = ref(null)

// 实体选项（用于级联选择）
const entityOptions = computed(() => {
  return vertexTypes.value.map(entity => ({
    value: entity.label,
    label: entity.label,
    children: entity.properties ? entity.properties.map(prop => ({
      value: prop.code,
      label: prop.name
    })) : []
  }))
})

// 图schema数据
const graphSchema = ref({
  entities: [],
  relations: []
})

// 根据选择的实体类型计算属性列表
const selectedEntityProperties = computed(() => {
  if (!analysisForm.targetEntity) return []

  const entity = graphSchema.value.entities.find(e => e.label === analysisForm.targetEntity)
  return entity ? entity.properties || [] : []
})

// 计算执行按钮是否禁用
const isExecuteButtonDisabled = computed(() => {
  // 如果是K层展开tab，需要检查targetEntity和queryValue
  if (activeAlgorithmTab.value === 'kLayerExpand') {
    return !(analysisForm.targetEntity && 
             analysisForm.targetEntity.length === 2 && 
             analysisForm.queryValue);
  }
  // 其他算法保持原有逻辑
  return !analysisForm.algorithm;
})

// 计算路径查询按钮是否禁用
const isPathQueryButtonDisabled = computed(() => {
  return !(analysisForm.sourceEntity && 
           analysisForm.sourceEntity.length === 2 && 
           analysisForm.sourceValue &&
           analysisForm.targetEntity && 
           analysisForm.targetEntity.length === 2 && 
           analysisForm.targetValue);
})

// 所有支持的算法列表
const algorithms = [
  {value: 'kLayerExpand', label: 'K层展开'},
  {value: 'shortestPath', label: '最短路径'},
  {value: 'pageRank', label: 'PageRank'},
  {value: 'community', label: '社区发现'},
  {value: 'connectedComponents', label: '连通分量'}
]

// 分组算法列表
const kLayerAlgorithms = computed(() => algorithms.filter(algo => algo.value === 'kLayerExpand'))
const pathAlgorithms = computed(() => algorithms.filter(algo => algo.value === 'shortestPath'))
const algorithmAlgorithms = computed(() => algorithms.filter(algo => 
  ['pageRank', 'community', 'connectedComponents'].includes(algo.value)))

// 表单验证规则
const analysisRules = {
  algorithm: [
    {required: true, message: '请选择分析算法', trigger: 'change'}
  ],
  sourceId: [
    {required: true, message: '请输入起点ID', trigger: 'blur'}
  ],
  targetId: [
    {required: true, message: '请输入终点ID', trigger: 'blur'}
  ],
  targetEntity: [
    {required: true, message: '请选择目标实体和属性', trigger: 'change', type: 'array', min: 2}
  ],
  queryValue: [
    {required: true, message: '请输入查询值', trigger: 'blur'}
  ],
  sourceEntity: [
    {required: true, message: '请选择起点实体和属性', trigger: 'change', type: 'array', min: 2}
  ],
  sourceValue: [
    {required: true, message: '请输入起点值', trigger: 'blur'}
  ],
  targetValue: [
    {required: true, message: '请输入终点值', trigger: 'blur'}
  ],
  maxLength: [
    {required: true, message: '请输入最大路径长度', trigger: 'blur'}
  ]
}

// K层展开专用验证规则
const kLayerExpandRules = {
  targetEntity: [
    {required: true, message: '请选择目标实体和属性', trigger: 'change', type: 'array', min: 2}
  ],
  queryValue: [
    {required: true, message: '请输入查询值', trigger: 'blur'}
  ]
}

// 路径查询专用验证规则
const pathQueryRules = {
  sourceEntity: [
    {required: true, message: '请选择起点实体和属性', trigger: 'change', type: 'array', min: 2}
  ],
  sourceValue: [
    {required: true, message: '请输入起点值', trigger: 'blur'}
  ],
  targetEntity: [
    {required: true, message: '请选择终点实体和属性', trigger: 'change', type: 'array', min: 2}
  ],
  targetValue: [
    {required: true, message: '请输入终点值', trigger: 'blur'}
  ],
  maxLength: [
    {required: true, message: '请输入最大路径长度', trigger: 'blur'}
  ]
}

// 分析状态
const analysisLoading = ref(false)
const analysisResult = ref(null)

// 图数据
const nodes = ref([])
const edges = ref([])

// 图统计信息
const graphStats = ref({
  nodes: '待获取',
  edges: '待获取',
  components: '待获取',
  avgDegree: '待获取'
})

// D3相关变量
let simulation = null
let svg = null
let g = null
let zoom = null
let rafId = null // 用于requestAnimationFrame的ID，避免过多DOM更新

// 拓展配置可见性
const expandConfigVisible = ref([])

// 处理算法选择变化
const handleAlgorithmChange = (value) => {
  console.log('选择算法:', value)
  // 可以根据选择的算法重置某些参数
}

// 处理实体类型变化
const handleEntityChange = (value) => {
  console.log('选择实体类型和属性:', value)
}

// 处理路径查询起点实体变化
const handleSourceEntityChange = (value) => {
  console.log('选择起点实体类型和属性:', value)
}

// 处理路径查询终点实体变化
const handleTargetEntityChange = (value) => {
  console.log('选择终点实体类型和属性:', value)
}

// 统一处理实体变化
const handleAnyEntityChange = (value, type) => {
  console.log(`选择${type}实体类型和属性:`, value)
}

// 添加实体方法
const addEntity = () => {
  // 实现添加实体逻辑
  ElMessage.info('添加实体功能待实现')
}

// 路径查询方法
const executePathQuery = async () => {
  if (!analysisFormRef.value) return

  try {
    // 手动验证路径查询需要的字段
    const isValid = analysisForm.sourceEntity && 
                   analysisForm.sourceEntity.length === 2 && 
                   analysisForm.sourceValue &&
                   analysisForm.targetEntity && 
                   analysisForm.targetEntity.length === 2 && 
                   analysisForm.targetValue;
    
    if (!isValid) {
      ElMessage.error('请填写所有必填字段');
      return;
    }
    
    analysisLoading.value = true
    analysisResult.value = null

    try {
      // 调用后端API进行路径查询
      const pathParams = {}
      if (graphsStore.currentGraphId < 0 && graphsStore.currentGraph) {
        pathParams.connectionId = graphsStore.currentGraph.connectionId
        pathParams.graphCode = graphsStore.currentGraph.code
      }
      const apiResponse = await graphApi.findPath(
        graphsStore.currentGraphId,
        analysisForm.sourceValue, // 起点ID
        analysisForm.targetValue, // 终点ID
        analysisForm.maxLength // 最大路径长度
      )

      // 转换API响应数据为图数据
      const transformedData = transformApiResponseToGraphData(apiResponse)
      // 标记路径节点并设置起点/终点高亮
      transformedData.nodes = transformedData.nodes.map(n => ({
        ...n,
        group: 'path'
      }))
      analysisForm.sourceId = analysisForm.sourceValue
      analysisForm.targetId = analysisForm.targetValue
      nodes.value = transformedData.nodes
      edges.value = transformedData.edges

      // 设置分析结果
      analysisResult.value = {
        nodeCount: nodes.value.length,
        edgeCount: edges.value.length,
        algorithm: 'shortestPath'
      }

    } catch (error) {
      console.error('路径查询失败:', error)
      ElMessage.error('路径查询失败: ' + (error.message || '未知错误'))
      analysisLoading.value = false
      return
    }

    // 绘制图形
    await nextTick()
    drawGraph()

    ElMessage.success('路径查询完成')
  } catch (error) {
    console.error('路径查询失败:', error)
    ElMessage.error('路径查询失败: ' + (error.message || '未知错误'))
  } finally {
    analysisLoading.value = false
  }
}

// 获取图schema信息
const fetchGraphSchema = async () => {
  if (!graphsStore.currentGraph) {
    ElMessage.warning('请先选择一个图')
    return
  }

  try {
    const params = {}
    if (graphsStore.currentGraphId < 0 && graphsStore.currentGraph) {
      params.connectionId = graphsStore.currentGraph.connectionId
      params.graphCode = graphsStore.currentGraph.code
    }
    const [nodeRes, edgeRes] = await Promise.all([
      graphApi.getVertexDefs(graphsStore.currentGraphId, params),
      graphApi.getEdgeDefs(graphsStore.currentGraphId, params)
    ])
    const vertexDefs = nodeRes?.data || nodeRes || []
    const edgeDefs = edgeRes?.data || edgeRes || []
    graphSchema.value = {
      entities: Array.isArray(vertexDefs) ? vertexDefs : [],
      relations: Array.isArray(edgeDefs) ? edgeDefs : []
    }
    vertexTypes.value = graphSchema.value.entities
    edgeTypes.value = graphSchema.value.relations

    if (graphSchema.value.entities.length > 0) {
      analysisForm.targetEntity = [graphSchema.value.entities[0].label]
    }
  } catch (error) {
    ElMessage.error('获取图schema失败: ' + (error.message || '未知错误'))
    graphSchema.value = {entities: [], relations: []}
  }
}

// 执行分析
const executeAnalysis = async () => {
  if (!analysisFormRef.value) return

  try {
    // 根据当前激活的tab使用不同的验证规则
    let isValid = false;
    if (activeAlgorithmTab.value === 'kLayerExpand') {
      // 对于K层展开，我们手动验证需要的字段
      isValid = analysisForm.targetEntity && 
                analysisForm.targetEntity.length === 2 && 
                analysisForm.queryValue;
    } else if (activeAlgorithmTab.value === 'pathQuery') {
      // 对于路径查询，我们手动验证需要的字段
      isValid = analysisForm.sourceEntity && 
                analysisForm.sourceEntity.length === 2 && 
                analysisForm.sourceValue &&
                analysisForm.targetEntity && 
                analysisForm.targetEntity.length === 2 && 
                analysisForm.targetValue;
    } else {
      // 对于算法分析，使用表单验证
      await analysisFormRef.value.validate()
      isValid = true;
    }
    
    if (!isValid) {
      ElMessage.error('请填写所有必填字段');
      return;
    }
    
    analysisLoading.value = true
    analysisResult.value = null

    // 根据当前激活的tab设置算法
    let algorithm = analysisForm.algorithm;
    if (activeAlgorithmTab.value === 'kLayerExpand') {
      algorithm = 'kLayerExpand';
    } else if (activeAlgorithmTab.value === 'pathQuery') {
      algorithm = 'shortestPath';
    }

    try {
      // 调用后端API执行分析
      let apiResponse;
      switch (algorithm) {
        case 'kLayerExpand':
          // K层展开：调用 expandNode API
          const targetEntityType = analysisForm.targetEntity[0];
          const targetEntityProp = analysisForm.targetEntity[1];
          const opParams = {}
          if (graphsStore.currentGraphId < 0 && graphsStore.currentGraph) {
            opParams.connectionId = graphsStore.currentGraph.connectionId
            opParams.graphCode = graphsStore.currentGraph.code
          }
          apiResponse = await graphApi.expandNode(
            graphsStore.currentGraphId,
            analysisForm.queryValue, // 使用查询值作为节点ID
            analysisForm.layers
          );
          break

        case 'shortestPath':
          // 最短路径：调用 findPath API
          const fpParams = {}
          if (graphsStore.currentGraphId < 0 && graphsStore.currentGraph) {
            fpParams.connectionId = graphsStore.currentGraph.connectionId
            fpParams.graphCode = graphsStore.currentGraph.code
          }
          apiResponse = await graphApi.findPath(
            graphsStore.currentGraphId,
            analysisForm.sourceValue, // 起点ID
            analysisForm.targetValue, // 终点ID
            analysisForm.maxLength // 最大路径长度
          );
          break

        case 'pageRank':
        case 'community':
        case 'connectedComponents':
          // 这些算法暂时使用查询API模拟
          ElMessage.info(`${algorithm} 算法暂未实现，使用查询模拟`);
          await new Promise(resolve => setTimeout(resolve, 1000));
          apiResponse = { data: { nodes: [], edges: [] } };
          break

        default:
          ElMessage.error('未知算法');
          analysisLoading.value = false;
          return;
      }

      // 转换API响应数据为图数据
      const transformedData = transformApiResponseToGraphData(apiResponse);
      // 标记种子节点
      if (algorithm === 'kLayerExpand') {
        transformedData.nodes = transformedData.nodes.map(n => ({
          ...n,
          group: n.id === analysisForm.queryValue ? 'center' : (n.group || 'normal')
        }));
      } else if (algorithm === 'shortestPath') {
        transformedData.nodes = transformedData.nodes.map(n => ({
          ...n,
          group: 'path'
        }));
      }
      nodes.value = transformedData.nodes;
      edges.value = transformedData.edges;

      // 设置分析结果
      analysisResult.value = {
        nodeCount: nodes.value.length,
        edgeCount: edges.value.length,
        algorithm: algorithm
      };

    } catch (error) {
      console.error('分析失败:', error);
      ElMessage.error('分析失败: ' + (error.message || '未知错误'));
      analysisLoading.value = false;
      return;
    }

    // 绘制图形
    await nextTick()
    drawGraph()

    ElMessage.success('分析完成')
  } catch (error) {
    console.error('分析失败:', error)
    ElMessage.error('分析失败: ' + (error.message || '未知错误'))
  } finally {
    analysisLoading.value = false
  }
}

// 生成示例图数据
const generateSampleGraphData = (nodeCount) => {
  nodes.value = []
  edges.value = []

  // 生成节点
  for (let i = 0; i < nodeCount; i++) {
    nodes.value.push({
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

    edges.value.push({
      source: nodes.value[sourceIndex].id,
      target: nodes.value[targetIndex].id,
      value: Math.random() * 5
    })
  }
}

// 生成K层展开示例数据
const generateKLayerExpandData = () => {
  nodes.value = []
  edges.value = []

  // 生成中心节点
  const centerNode = {
    id: 'center',
    label: '中心节点',
    group: 'center',
    x: 300,
    y: 200
  }
  nodes.value.push(centerNode)

  // 生成第一层节点
  for (let i = 1; i <= 5; i++) {
    nodes.value.push({
      id: `layer1_${i}`,
      label: `第一层节点${i}`,
      group: 'layer1',
      x: 300 + Math.random() * 100 - 50,
      y: 200 + Math.random() * 100 - 50
    })
  }

  // 生成第二层节点
  for (let i = 1; i <= 8; i++) {
    nodes.value.push({
      id: `layer2_${i}`,
      label: `第二层节点${i}`,
      group: 'layer2',
      x: 300 + Math.random() * 150 - 75,
      y: 200 + Math.random() * 150 - 75
    })
  }

  // 生成边
  for (let i = 1; i <= 5; i++) {
    edges.value.push({
      source: 'center',
      target: `layer1_${i}`,
      value: 1.2
    })
  }

  for (let i = 1; i <= 8; i++) {
    edges.value.push({
      source: `layer1_${Math.floor(Math.random() * 5) + 1}`,
      target: `layer2_${i}`,
      value: 0.8
    })
  }
}

// 获取节点半径
const getNodeRadius = (node) => {
  if (node.id === analysisForm.sourceId || node.id === analysisForm.targetId) return 22;
  if (node.group === 'center' || node.group === 'path') return 22;
  return 16;
};

// 绘制图形
const drawGraph = () => {
  if (!svgRef.value || !graphContainerRef.value) return

  // 清除之前的图形
  clearSvg()

  // 设置SVG尺寸
  const container = graphContainerRef.value
  svgRef.value.setAttribute('width', container.clientWidth)
  svgRef.value.setAttribute('height', container.clientHeight)

  // 设置SVG和g元素
  svg = d3.select(svgRef.value)
      .style('cursor', 'grab')
  g = svg.append('g')

  // 设置缩放行为
  zoom = d3.zoom()
      .scaleExtent([0.1, 8])
      .on('zoom', handleZoom)

  svg.call(zoom)

  // 创建力导向模拟
  simulation = d3.forceSimulation(nodes.value)
      .force('link', d3.forceLink(edges.value).id(d => d.id).distance(80))
      .force('charge', d3.forceManyBody().strength(-120))
      .force('center', d3.forceCenter(container.clientWidth / 2, container.clientHeight / 2))
      .force('collision', d3.forceCollide(25))
      .on('tick', ticked)

  // 定义箭头标记
  const defs = g.append("defs");
  
  // 为每条边创建独立的箭头标记，以适应不同颜色和大小
  const isPathScene = activeAlgorithmTab.value === 'pathQuery' || analysisForm.algorithm === 'shortestPath';
  edges.value.forEach((d, i) => {
    const color = isPathScene ? 'var(--el-color-danger)' : 'var(--el-text-color-secondary)';
    
    defs.append("marker")
      .attr("id", `arrow-${i}`)
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 10)
      .attr("refY", 0)
      .attr("markerWidth", 8)
      .attr("markerHeight", 8)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M 0 -5 L 10 0 L 0 5")
      .attr("fill", color);
  });

  const link = g.append('g')
      .attr('class', 'links')
      .selectAll('g')
      .data(edges.value)
      .enter()
      .append('g')
      .attr('class', 'edge-group')
      // 透明宽线（点击区域）
      .append('line')
      .attr('stroke', 'transparent')
      .attr('stroke-width', 14)
      .attr('stroke-linecap', 'round')
      .attr('cursor', 'pointer')
      .on('click', (event, d) => {
        selectedElement.value = {
          type: 'edge',
          id: d.id,
          label: d.label,
          properties: d.properties,
          source: d.source,
          target: d.target
        };
        detailDrawerVisible.value = true;
        event.stopPropagation();
      })

  // 可见边线
  const linkLine = g.select('.links').selectAll('.edge-group')
      .append('line')
      .attr('stroke', isPathScene ? 'var(--el-color-danger)' : 'var(--el-text-color-secondary)')
      .attr('stroke-width', isPathScene ? 3 : 2)
      .attr('stroke-opacity', isPathScene ? 1 : 0.6)
      .attr('stroke-linecap', 'round')
      .attr('pointer-events', 'none')
      .attr("marker-end", (d, i) => `url(#arrow-${i})`)

  // 绘制节点
  const node = g.append('g')
      .attr('class', 'nodes')
      .selectAll('circle')
      .data(nodes.value)
      .enter()
      .append('circle')
      .attr('r', d => {
        if (d.id === analysisForm.sourceId || d.id === analysisForm.targetId) return 22
        if (d.group === 'center' || d.group === 'path') return 22
        return 16
      })
      .attr('fill', d => {
        // sourceId/targetId 优先于 group
        if (d.id === analysisForm.sourceId) return 'var(--el-color-success)'
        if (d.id === analysisForm.targetId) return 'var(--el-color-warning)'
        if (d.group === 'path') return 'var(--el-color-danger)'
        if (d.group === 'center') return 'var(--el-color-warning)'
        if (d.group === 'layer1') return 'var(--el-color-primary)'
        if (d.group === 'layer2') return 'var(--el-color-success)'
        return 'var(--el-color-primary)'
      })
      .attr('stroke', '#fff')
      .attr('stroke-width', 1.5)
      .attr('cursor', 'pointer')
      .call(d3.drag()
          .on('start', dragstarted)
          .on('drag', dragged)
          .on('end', dragended))
      // 添加节点点击事件
      .on('click', (event, d) => {
        selectedElement.value = {
          type: 'node',
          id: d.id,
          label: d.label,
          properties: d.properties
        };
        detailDrawerVisible.value = true;
        event.stopPropagation();
      })

  // 节点标签
  const text = g.append('g')
      .attr('class', 'labels')
      .selectAll('text')
      .data(nodes.value)
      .enter()
      .append('text')
      .text(d => d.id)
      .attr('text-anchor', 'middle')
      .attr('dy', '0.35em')
      .attr('fill', '#fff')
      .attr('font-size', '10px')
      .attr('font-weight', 600)
      .attr('pointer-events', 'none')

  // 力导向模拟tick函数 - 使用requestAnimationFrame批量化DOM更新
  function ticked() {
    // 取消之前Pending的动画帧，避免过多DOM更新
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
    
    rafId = requestAnimationFrame(() => {
      // 批量更新DOM，减少重绘次数
      // 连线偏移到节点边缘，让箭头可见
      link
        .attr('x1', d => {
          const r = getNodeRadius(d.source);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.source.x + (dx / dist) * r;
        })
        .attr('y1', d => {
          const r = getNodeRadius(d.source);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.source.y + (dy / dist) * r;
        })
        .attr('x2', d => {
          const r = getNodeRadius(d.target);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.target.x - (dx / dist) * r;
        })
        .attr('y2', d => {
          const r = getNodeRadius(d.target);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.target.y - (dy / dist) * r;
        })

      linkLine
        .attr('x1', d => {
          const r = getNodeRadius(d.source);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.source.x + (dx / dist) * r;
        })
        .attr('y1', d => {
          const r = getNodeRadius(d.source);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.source.y + (dy / dist) * r;
        })
        .attr('x2', d => {
          const r = getNodeRadius(d.target);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.target.x - (dx / dist) * r;
        })
        .attr('y2', d => {
          const r = getNodeRadius(d.target);
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return d.target.y - (dy / dist) * r;
        })

      node
        .attr('cx', d => d.x)
        .attr('cy', d => d.y)

      text
        .attr('x', d => d.x)
        .attr('y', d => d.y)
        
      // 清空rafId
      rafId = null
    })
  }

  // 拖拽开始
  function dragstarted(event, d) {
    if (!event.active) simulation.alphaTarget(0.3).restart()
    d.fx = d.x
    d.fy = d.y
  }

  // 拖拽中
  function dragged(event, d) {
    d.fx = event.x
    d.fy = event.y
  }

  // 拖拽结束
  function dragended(event, d) {
    if (!event.active) simulation.alphaTarget(0)
    d.fx = null
    d.fy = null
  }

  // 缩放处理函数
  function handleZoom(event) {
    g.attr('transform', event.transform)
  }
}

// 清除SVG内容
const clearSvg = () => {
  // 取消Pending的动画帧
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
  }

  if (simulation) {
    simulation.stop()
    simulation = null
  }

  if (svgRef.value) {
    d3.select(svgRef.value).selectAll('*').remove()
  }
  
  // 重置D3相关引用
  svg = null
  g = null
}

// 高亮显示路径
const highlightPath = () => {
  ElMessage.info('高亮显示路径功能待实现')
}

// 关闭详情面板
const closeDetailPanel = () => {
  detailDrawerVisible.value = false
  selectedElement.value = null
}

// 缩放控制
const zoomIn = () => {
  if (svg && zoom) {
    svg.transition().call(zoom.scaleBy, 1.2)
  }
}

const zoomOut = () => {
  if (svg && zoom) {
    svg.transition().call(zoom.scaleBy, 0.8)
  }
}

const resetView = () => {
  if (svg && zoom) {
    svg.transition().call(zoom.transform, d3.zoomIdentity)
  }
}

// 导出功能
const handleExport = (format) => {
  if (format === 'png') exportAsPNG()
  else if (format === 'svg') exportAsSVG()
  else if (format === 'json') exportAsJSON()
}

const exportAsPNG = () => {
  const svgEl = document.querySelector('.graph-container svg')
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

const exportAsSVG = () => {
  const svgEl = document.querySelector('.graph-container svg')
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

const exportAsJSON = () => {
  const data = { nodes: nodes.value, edges: edges.value }
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

// 组件挂载时的处理
onMounted(() => {
  window.addEventListener('resize', handleResize)

  if (graphsStore.currentGraphId) {
    fetchGraphSchema()
  }

  watch(() => graphsStore.currentGraph, () => {
    fetchGraphSchema()
  })
})

// 获取点类型列表
const fetchNodeTypes = async (graphId) => {
  try {
    const params = {}
    if (graphId < 0 && graphsStore.currentGraph) {
      params.connectionId = graphsStore.currentGraph.connectionId
      params.graphCode = graphsStore.currentGraph.code
    }
    const res = await graphApi.getVertexDefs(graphId, params)
    const data = res?.data || res || []
    vertexTypes.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('获取点类型列表失败:', e)
  }
}

// 获取边类型列表
const fetchEdgeTypes = async (graphId) => {
  try {
    const params = {}
    if (graphId < 0 && graphsStore.currentGraph) {
      params.connectionId = graphsStore.currentGraph.connectionId
      params.graphCode = graphsStore.currentGraph.code
    }
    const res = await graphApi.getEdgeDefs(graphId, params)
    const data = res?.data || res || []
    edgeTypes.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('获取边类型列表失败:', e)
  }
}

// 组件卸载时的处理
onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('resize', handleResize)

  // 停止力导向模拟
  if (simulation) {
    simulation.stop()
  }

  // 清理D3元素
  clearSvg()
})

// 处理窗口大小变化
const handleResize = () => {
  if (simulation && graphContainerRef.value) {
    // 更新SVG尺寸
    if (svgRef.value) {
      svgRef.value.setAttribute('width', graphContainerRef.value.clientWidth)
      svgRef.value.setAttribute('height', graphContainerRef.value.clientHeight)
    }

    simulation.force('center', d3.forceCenter(
        graphContainerRef.value.clientWidth / 2,
        graphContainerRef.value.clientHeight / 2
    ))
    simulation.alpha(0.3).restart()
  }
}

/**
 * 将API返回的数据转换为图数据格式
 * @param {Object} apiResponse - API返回的数据
 * @returns {Object} 转换后的图数据
 */
const transformApiResponseToGraphData = (apiResponse) => {
  const rawData = apiResponse.data || apiResponse || []

  // 优先处理 vertices/edges 格式
  if (rawData.vertices && rawData.edges) {
    return {
      nodes: rawData.vertices.map(v => ({
        id: v.uid || v.id,
        label: v.label,
        properties: v.properties || {},
        group: v.group || 'normal'
      })),
      edges: rawData.edges.map(e => ({
        id: e.uid || e.id,
        source: e.startUid,
        target: e.endUid,
        label: e.label,
        properties: e.properties || {},
        value: e.value || 1
      }))
    }
  }

  // 如果返回的是标准图数据结构
  if (rawData.nodes && rawData.edges) {
    return {
      nodes: rawData.nodes || [],
      edges: rawData.edges || []
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
.graph-analysis-container {
  padding: 0px;
  height: calc(100vh - 60px);
  background: var(--el-bg-color-page);
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  overflow: hidden;
}

.analysis-layout {
  background: var(--el-bg-color-page);
}

.operation-panel {
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* 侧边栏头部 */
.sidebar-header {
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  flex-shrink: 0;
  gap: 10px;
}
.sidebar-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: 0.5px;
}

/* 操作内容区 - 可滚动 */
.operation-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}
.operation-content::-webkit-scrollbar {
  width: 4px;
}
.operation-content::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 2px;
}
.operation-content::-webkit-scrollbar-track {
  background: transparent;
}

.visualization-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  flex-shrink: 0;
}


.visualization-content {
  flex: 1;
  display: flex;
  flex-direction: column;
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
  border-radius: 0;
}

.graph-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.graph-svg {
  width: 100%;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--el-bg-color-overlay);
  opacity: 0.8;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10;
}

.loading-content {
  text-align: center;
}

.empty-state {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.result-section {
  padding: 14px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  margin: 0;
}

.result-section :deep(.el-divider) {
  margin: 0 0 12px 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.result-section :deep(.el-divider__text) {
  padding: 0;
  background: transparent;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.result-content h4 {
  margin: 10px 0 5px 0;
  color: var(--el-text-color-primary);
}

.result-content p {
  margin: 5px 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.config-item {
  padding: 10px;
  background-color: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  margin: 8px 0;
}

:deep(.el-collapse-item__header) {
  font-weight: 600;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.el-collapse-item__wrap) {
  border: none;
}

:deep(.el-collapse-item__content) {
  padding: 10px;
  background-color: var(--el-bg-color);
}

.algorithm-tabs {
  padding: 12px 16px;
}

.algorithm-tabs :deep(.el-tabs__header) {
  margin: 0 0 16px 0;
}

.algorithm-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.algorithm-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  border-radius: 1px;
}

.algorithm-tabs :deep(.el-tabs__item) {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding: 0 12px 8px;
  height: auto;
  line-height: 1.5;
}

.algorithm-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}

.algorithm-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.algorithm-tabs :deep(.el-form-item) {
  margin-bottom: 16px;
}

.algorithm-tabs :deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  padding-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
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
</style>
