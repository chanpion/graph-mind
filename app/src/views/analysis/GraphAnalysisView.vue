<template>
  <div class="graph-analysis-container">

    <el-row :gutter="20" class="main-content">
      <!-- 左侧操作区 -->
      <el-col :span="6" class="operation-panel">
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
          <div v-if="analysisResult && !analysisLoading" class="result-section">
            <el-divider>分析结果</el-divider>
            <div class="result-content">
              <template v-if="activeAlgorithmTab === 'kLayerExpand' || analysisForm.algorithm === 'kLayerExpand'">
                <h4>K层展开</h4>
                <p>拓展节点数: {{ analysisResult.expandedNodes }}</p>
                <p>拓展边数: {{ analysisResult.expandedEdges }}</p>
                <p>路径数: {{ analysisResult.pathCount }}</p>
              </template>
              
              <template v-else-if="activeAlgorithmTab === 'pathQuery' || analysisForm.algorithm === 'shortestPath'">
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
      <el-col :span="18" class="visualization-panel">
        <!-- 画布操作栏 -->
        <div class="canvas-toolbar">
          <div class="toolbar-left">
            <span class="graph-type-tag">
              <el-tag size="small" :type="graphTypeTagType" effect="plain">
                {{ graphTypeLabel }}
              </el-tag>
            </span>
            <span class="default-query-hint" v-if="defaultQuery">
              默认查询: <code>{{ defaultQuery }}</code>
            </span>
          </div>
          <div class="toolbar-right">
            <el-tooltip content="放大" placement="top">
              <el-button text size="small" @click="zoomIn">
                <template #icon><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg></template>
              </el-button>
            </el-tooltip>
            <el-tooltip content="缩小" placement="top">
              <el-button text size="small" @click="zoomOut">
                <template #icon><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 13H5v-2h14v2z"/></svg></template>
              </el-button>
            </el-tooltip>
            <el-tooltip content="适应画布" placement="top">
              <el-button text size="small" @click="resetView">
                <template #icon><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M21 15v4c0 .55-.45 1-1 1h-4M21 9V5c0-.55-.45-1-1-1h-4M3 15v4c0 .55.45 1 1 1h4M3 9V5c0-.55.45-1 1-1h4"/></svg></template>
              </el-button>
            </el-tooltip>
            <el-divider direction="vertical" />
            <span class="node-edge-count">
              <span class="count-item"><el-tag size="small" round>点 {{ nodes.length }}</el-tag></span>
              <span class="count-item"><el-tag size="small" round type="warning">边 {{ edges.length }}</el-tag></span>
            </span>
          </div>
        </div>

        <div class="visualization-content">
          <!-- 图可视化区域 -->
          <div ref="graphContainerRef" class="graph-container">
            <svg ref="svgRef" class="graph-svg"></svg>

            <!-- 图例 -->
            <div v-if="nodes.length > 0" class="canvas-legend">
              <div class="legend-title">图例</div>
              <div class="legend-items">
                <div v-for="item in legendItems" :key="item.label" class="legend-item">
                  <span class="legend-color" :style="{ background: item.color }"></span>
                  <span class="legend-label">{{ item.label }}</span>
                  <span class="legend-count">{{ item.count }}</span>
                </div>
              </div>
            </div>

            <!-- 点边统计 -->
            <div v-if="nodes.length > 0" class="canvas-stats">
              <div class="stats-item">
                <span class="stats-dot" style="background: #409EFF"></span>
                <span>点: {{ nodes.length }}</span>
              </div>
              <div class="stats-item">
                <span class="stats-dot" style="background: #E6A23C"></span>
                <span>边: {{ edges.length }}</span>
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
      </el-col>
    </el-row>
    
    <!-- 节点详情抽屉 -->
    <el-drawer
      v-model="nodeDrawerVisible"
      title="节点详情"
      direction="rtl"
      size="40%"
    >
      <div class="drawer-content">
        <el-descriptions
          v-if="selectedNode"
          :column="1"
          border
        >
          <el-descriptions-item label="ID">
            {{ selectedNode.id }}
          </el-descriptions-item>
          <el-descriptions-item label="标签">
            {{ selectedNode.label }}
          </el-descriptions-item>
          <el-descriptions-item label="分组">
            {{ selectedNode.group }}
          </el-descriptions-item>
          <el-descriptions-item label="属性">
            <div v-for="(value, key) in selectedNode.properties" :key="key">
              {{ key }}: {{ value }}
            </div>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无数据" />
      </div>
    </el-drawer>
    
    <!-- 边详情抽屉 -->
    <el-drawer
      v-model="edgeDrawerVisible"
      title="边详情"
      direction="rtl"
      size="40%"
    >
      <div class="drawer-content">
        <el-descriptions
          v-if="selectedEdge"
          :column="1"
          border
        >
          <el-descriptions-item label="源节点">
            {{ selectedEdge.source.id || selectedEdge.source }}
          </el-descriptions-item>
          <el-descriptions-item label="目标节点">
            {{ selectedEdge.target.id || selectedEdge.target }}
          </el-descriptions-item>
          <el-descriptions-item label="值">
            {{ selectedEdge.value }}
          </el-descriptions-item>
          <el-descriptions-item label="属性">
            <div v-for="(value, key) in selectedEdge.properties" :key="key">
              {{ key }}: {{ value }}
            </div>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无数据" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import * as d3 from 'd3'
import {ElMessage} from 'element-plus'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import { Loading } from '@element-plus/icons-vue'
import { graphApi } from '@/views/graphs/api/graph'

// 响应式数据
const analysisFormRef = ref(null)
const graphContainerRef = ref(null)
const svgRef = ref(null)
const graphsStore = useGraphsStore()

// 图类型信息
const graphType = computed(() => graphsStore.currentGraph?.graphType || '')
const graphTypeLabel = computed(() => {
  const map = { neo4j: 'Neo4j', nebula: 'Nebula Graph', janusgraph: 'JanusGraph' }
  return map[graphType.value] || graphType.value || '未知'
})
const graphTypeTagType = computed(() => {
  const map = { neo4j: 'success', nebula: 'warning', janusgraph: 'danger' }
  return map[graphType.value] || 'info'
})
const defaultQuery = computed(() => {
  const map = {
    neo4j: 'MATCH (n) RETURN n LIMIT 10',
    nebula: 'FETCH PROP ON * LIMIT 10',
    janusgraph: 'g.V().limit(10)'
  }
  return map[graphType.value] || ''
})

// 图例数据
const legendItems = computed(() => {
  const groups = {}
  nodes.value.forEach(n => {
    const g = n.group || 'default'
    if (!groups[g]) groups[g] = { label: g, count: 0, color: '#409EFF' }
    groups[g].count++
  })
  const colorMap = {
    center: '#e6a23c', layer1: '#409EFF', layer2: '#67c23a',
    path: '#f56c6c', normal: '#909399', default: '#409EFF'
  }
  Object.keys(groups).forEach(k => {
    if (colorMap[k]) groups[k].color = colorMap[k]
  })
  return Object.values(groups)
})

// 点类型和边类型
const nodeTypes = ref([])
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

// 抽屉相关数据
const nodeDrawerVisible = ref(false)
const edgeDrawerVisible = ref(false)
const selectedNode = ref(null)
const selectedEdge = ref(null)

// 实体选项（用于级联选择）
const entityOptions = computed(() => {
  return nodeTypes.value.map(entity => ({
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

    // 调用实际API进行路径查询，传递maxLength参数
    // TODO: 这里需要根据实际API调整参数传递方式
    // 目前使用模拟数据
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    // 示例：调用更新后的API
    // const res = await graphApi.findPath(
    //   graphsStore.currentGraphId, 
    //   analysisForm.sourceValue, 
    //   analysisForm.targetValue, 
    //   5,  // maxDepth
    //   analysisForm.maxLength  // maxLength
    // )

    // 模拟路径查询结果
    analysisResult.value = {
      pathLength: 4.2,
      nodeCount: 5,
      path: ['1', '3', '5', '7', '9']
    }

    // 模拟路径数据
    nodes.value = [
      {id: '1', label: '起点', group: 'path', x: 100, y: 100},
      {id: '3', label: '节点3', group: 'path', x: 200, y: 150},
      {id: '5', label: '节点5', group: 'path', x: 300, y: 100},
      {id: '7', label: '节点7', group: 'path', x: 400, y: 150},
      {id: '9', label: '终点', group: 'path', x: 500, y: 100},
      {id: '2', label: '普通节点', group: 'normal', x: 150, y: 250},
      {id: '4', label: '普通节点', group: 'normal', x: 350, y: 250},
      {id: '6', label: '普通节点', group: 'normal', x: 250, y: 300}
    ]
    edges.value = [
      {source: '1', target: '3', value: 1.2},
      {source: '3', target: '5', value: 1.5},
      {source: '5', target: '7', value: 0.8},
      {source: '7', target: '9', value: 0.7},
      {source: '1', target: '2', value: 2.1},
      {source: '2', target: '4', value: 1.3},
      {source: '4', target: '6', value: 1.1},
      {source: '6', target: '5', value: 1.9}
    ]

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
    const [nodeRes, edgeRes] = await Promise.all([
      graphApi.getNodeDefs(graphsStore.currentGraphId),
      graphApi.getEdgeDefs(graphsStore.currentGraphId)
    ])
    const nodeDefs = nodeRes?.data || nodeRes || []
    const edgeDefs = edgeRes?.data || edgeRes || []
    graphSchema.value = {
      entities: Array.isArray(nodeDefs) ? nodeDefs : [],
      relations: Array.isArray(edgeDefs) ? edgeDefs : []
    }
    nodeTypes.value = graphSchema.value.entities
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

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1500))

    // 根据当前激活的tab设置算法
    let algorithm = analysisForm.algorithm;
    if (activeAlgorithmTab.value === 'kLayerExpand') {
      algorithm = 'kLayerExpand';
    } else if (activeAlgorithmTab.value === 'pathQuery') {
      algorithm = 'shortestPath';
    }

    // 模拟分析结果
    switch (algorithm) {
      case 'shortestPath':
        analysisResult.value = {
          pathLength: 4.2,
          nodeCount: 5,
          path: ['1', '3', '5', '7', '9']
        }
        // 模拟路径数据
        nodes.value = [
          {id: '1', label: '起点', group: 'path', x: 100, y: 100},
          {id: '3', label: '节点3', group: 'path', x: 200, y: 150},
          {id: '5', label: '节点5', group: 'path', x: 300, y: 100},
          {id: '7', label: '节点7', group: 'path', x: 400, y: 150},
          {id: '9', label: '终点', group: 'path', x: 500, y: 100},
          {id: '2', label: '普通节点', group: 'normal', x: 150, y: 250},
          {id: '4', label: '普通节点', group: 'normal', x: 350, y: 250},
          {id: '6', label: '普通节点', group: 'normal', x: 250, y: 300}
        ]
        edges.value = [
          {source: '1', target: '3', value: 1.2},
          {source: '3', target: '5', value: 1.5},
          {source: '5', target: '7', value: 0.8},
          {source: '7', target: '9', value: 0.7},
          {source: '1', target: '2', value: 2.1},
          {source: '2', target: '4', value: 1.3},
          {source: '4', target: '6', value: 1.1},
          {source: '6', target: '5', value: 1.9}
        ]
        break

      case 'pageRank':
        analysisResult.value = {
          nodeCount: 128,
          topNode: 'Node_42',
          topScore: 0.0875
        }
        // 生成PageRank示例数据
        generateSampleGraphData(20)
        break

      case 'community':
        analysisResult.value = {
          communityCount: 5,
          maxCommunitySize: 32
        }
        // 生成社区发现示例数据
        generateSampleGraphData(25)
        break

      case 'connectedComponents':
        analysisResult.value = {
          componentCount: 3,
          maxComponentSize: 85
        }
        // 生成连通分量示例数据
        generateSampleGraphData(30)
        break

      case 'kLayerExpand':
        analysisResult.value = {
          pathLength: 4.2,
          nodeCount: 5,
          path: ['1', '3', '5', '7', '9'],
          expandedNodes: 23,
          expandedEdges: 45,
          pathCount: 12
        }
        // 生成K层展开示例数据
        generateKLayerExpandData()
        break
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
  g = svg.append('g')

  // 设置缩放行为
  zoom = d3.zoom()
      .scaleExtent([0.1, 10])
      .on('zoom', handleZoom)

  svg.call(zoom)

  // 创建力导向模拟
  simulation = d3.forceSimulation(nodes.value)
      .force('link', d3.forceLink(edges.value).id(d => d.id).distance(100))
      .force('charge', d3.forceManyBody().strength(-300))
      .force('center', d3.forceCenter(container.clientWidth / 2, container.clientHeight / 2))
      .on('tick', ticked)

  // 定义箭头标记
  const defs = g.append("defs");
  
  // 为每条边创建独立的箭头标记，以适应不同颜色和大小
  edges.value.forEach((d, i) => {
    const isPath = d.source.group === 'path' && d.target.group === 'path';
    const color = isPath ? '#f56c6c' : '#999';
    const size = isPath ? 17 : 12; // 节点半径 + 2
    
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
      .attr("fill", color);
  });

  const link = g.append('g')
      .attr('class', 'links')
      .selectAll('line')
      .data(edges.value)
      .enter()
      .append('line')
      .attr('stroke', d => d.source.group === 'path' && d.target.group === 'path' ? '#f56c6c' : '#999')
      .attr('stroke-width', d => d.source.group === 'path' && d.target.group === 'path' ? 3 : 2)
      .attr("marker-end", (d, i) => `url(#arrow-${i})`)
      // 添加边点击事件
      .on('click', (event, d) => {
        selectedEdge.value = d;
        edgeDrawerVisible.value = true;
        event.stopPropagation();
      })

  // 绘制节点
  const node = g.append('g')
      .attr('class', 'nodes')
      .selectAll('circle')
      .data(nodes.value)
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
          .on('start', dragstarted)
          .on('drag', dragged)
          .on('end', dragended)
      )
      // 添加节点点击事件
      .on('click', (event, d) => {
        selectedNode.value = d;
        nodeDrawerVisible.value = true;
        event.stopPropagation();
      })

  // 节点标签
  const text = g.append('g')
      .attr('class', 'labels')
      .selectAll('text')
      .data(nodes.value)
      .enter()
      .append('text')
      .text(d => d.label)
      .attr('text-anchor', 'middle')
      .attr('dy', 25)
      .attr('fill', '#333')
      .attr('font-size', '12px')

  // 力导向模拟tick函数 - 使用requestAnimationFrame批量化DOM更新
  function ticked() {
    // 取消之前Pending的动画帧，避免过多DOM更新
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
    
    rafId = requestAnimationFrame(() => {
      // 批量更新DOM，减少重绘次数
      link
        .attr('x1', d => d.source.x)
        .attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x)
        .attr('y2', d => d.target.y)

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


// 缩放功能
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
    const res = await graphApi.getNodeDefs(graphId)
    const data = res?.data || res || []
    nodeTypes.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('获取点类型列表失败:', e)
  }
}

// 获取边类型列表
const fetchEdgeTypes = async (graphId) => {
  try {
    const res = await graphApi.getEdgeDefs(graphId)
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

</script>

<style scoped>
.graph-analysis-container {
  padding: 0px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  overflow: hidden;
}

.operation-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.visualization-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}


.operation-content,
.visualization-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.graph-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
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
  background: rgba(255, 255, 255, 0.8);
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
  margin-top: 20px;
}

.result-content h4 {
  margin: 10px 0 5px 0;
  color: #303133;
}

.result-content p {
  margin: 5px 0;
  color: #606266;
  font-size: 14px;
}

.config-item {
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin: 10px 0;
}

:deep(.el-collapse-item__header) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-collapse-item__wrap) {
  border: none;
}

:deep(.el-collapse-item__content) {
  padding: 10px;
  background-color: #fff;
}

.algorithm-tabs {
  margin-bottom: 20px;
}

:deep(.algorithm-tabs .el-tabs__content) {
  padding: 10px 0;
}

:deep(.algorithm-tabs .el-form-item) {
  margin-bottom: 18px;
}

.drawer-content {
  padding: 20px;
}

.drawer-content .el-descriptions {
  margin-bottom: 20px;
}

	/* ====== 画布操作栏 ====== */
	.canvas-toolbar {
	  display: flex;
	  align-items: center;
	  justify-content: space-between;
	  padding: 6px 12px;
	  background: var(--el-bg-color);
	  border: 1px solid var(--el-border-color-light);
	  border-bottom: none;
	  border-radius: 4px 4px 0 0;
	  flex-shrink: 0;
	  min-height: 36px;
	}

	.toolbar-left,
	.toolbar-right {
	  display: flex;
	  align-items: center;
	  gap: 8px;
	}

	.graph-type-tag {
	  flex-shrink: 0;
	}

	.default-query-hint {
	  font-size: 12px;
	  color: var(--el-text-color-secondary);
	  white-space: nowrap;
	  overflow: hidden;
	  text-overflow: ellipsis;
	  max-width: 400px;
	}

	.default-query-hint code {
	  background: var(--el-fill-color-light);
	  padding: 1px 6px;
	  border-radius: 3px;
	  font-size: 12px;
	  color: var(--el-color-primary);
	}

	.node-edge-count {
	  display: flex;
	  align-items: center;
	  gap: 6px;
	}

	.count-item {
	  font-size: 12px;
	}

	/* ====== 画布容器 ====== */
	.graph-container {
	  position: absolute;
	  top: 0;
	  left: 0;
	  right: 0;
	  bottom: 0;
	  border: 1px solid var(--el-border-color-light);
	  border-radius: 0 0 4px 4px;
	  background: var(--el-fill-color-light);
	  overflow: hidden;
	}

	/* ====== 图例 ====== */
	.canvas-legend {
	  position: absolute;
	  bottom: 16px;
	  left: 16px;
	  background: var(--el-bg-color-overlay);
	  border: 1px solid var(--el-border-color-light);
	  border-radius: 6px;
	  padding: 10px 14px;
	  min-width: 110px;
	  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
	  z-index: 5;
	}

	.legend-title {
	  font-size: 12px;
	  font-weight: 600;
	  color: var(--el-text-color-primary);
	  margin-bottom: 6px;
	  padding-bottom: 4px;
	  border-bottom: 1px solid var(--el-border-color-lighter);
	}

	.legend-items {
	  display: flex;
	  flex-direction: column;
	  gap: 4px;
	}

	.legend-item {
	  display: flex;
	  align-items: center;
	  gap: 6px;
	  font-size: 12px;
	  color: var(--el-text-color-regular);
	}

	.legend-color {
	  width: 10px;
	  height: 10px;
	  border-radius: 50%;
	  flex-shrink: 0;
	}

	.legend-label {
	  flex: 1;
	}

	.legend-count {
	  color: var(--el-text-color-secondary);
	  font-size: 11px;
	}

	/* ====== 点边统计 ====== */
	.canvas-stats {
	  position: absolute;
	  bottom: 16px;
	  right: 16px;
	  background: var(--el-bg-color-overlay);
	  border: 1px solid var(--el-border-color-light);
	  border-radius: 6px;
	  padding: 8px 14px;
	  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
	  z-index: 5;
	  display: flex;
	  flex-direction: column;
	  gap: 4px;
	}

	.stats-item {
	  display: flex;
	  align-items: center;
	  gap: 6px;
	  font-size: 12px;
	  color: var(--el-text-color-regular);
	}

	.stats-dot {
	  width: 8px;
	  height: 8px;
	  border-radius: 50%;
	  flex-shrink: 0;
	}

</style>
