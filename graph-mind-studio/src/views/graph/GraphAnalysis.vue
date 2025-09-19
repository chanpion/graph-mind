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

            <!-- 算法分析Tab -->
            <el-tab-pane label="算法分析" name="algorithm">
              <el-form
                  ref="analysisFormRef"
                  :model="analysisForm"
                  :rules="analysisRules"
                  label-position="top"
              >
                <el-form-item label="分析算法" prop="algorithm">
                  <el-select
                      v-model="analysisForm.algorithm"
                      placeholder="请选择分析算法"
                      style="width: 100%"
                      @change="handleAlgorithmChange"
                  >
                    <el-option
                        v-for="algo in algorithmAlgorithms"
                        :key="algo.value"
                        :label="algo.label"
                        :value="algo.value"
                    />
                  </el-select>
                </el-form-item>

                <!-- 算法参数配置 -->
                <div v-if="analysisForm.algorithm">
                  <el-divider>参数配置</el-divider>

                  <!-- PageRank参数 -->
                  <div v-if="analysisForm.algorithm === 'pageRank'">
                    <el-form-item label="迭代次数" prop="iterations">
                      <el-input-number
                          v-model="analysisForm.iterations"
                          :min="1"
                          :max="100"
                          style="width: 100%"
                      />
                    </el-form-item>
                    <el-form-item label="阻尼系数" prop="dampingFactor">
                      <el-slider
                          v-model="analysisForm.dampingFactor"
                          :min="0.1"
                          :max="0.9"
                          :step="0.05"
                          show-input
                      />
                    </el-form-item>
                  </div>

                  <!-- 社区发现参数 -->
                  <div v-else-if="analysisForm.algorithm === 'community'">
                    <el-form-item label="分辨率" prop="resolution">
                      <el-slider
                          v-model="analysisForm.resolution"
                          :min="0.1"
                          :max="2"
                          :step="0.1"
                          show-input
                      />
                    </el-form-item>
                  </div>

                  <!-- 连通分量参数 -->
                  <div v-else-if="analysisForm.algorithm === 'connectedComponents'">
                    <el-form-item label="弱连通分量" prop="weakly">
                      <el-switch v-model="analysisForm.weakly"/>
                    </el-form-item>
                  </div>
                </div>
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
              
              <template v-else-if="analysisForm.algorithm === 'pageRank'">
                <h4>PageRank结果</h4>
                <p>处理节点数: {{ analysisResult.nodeCount }}</p>
                <p>最高得分节点: {{ analysisResult.topNode }}</p>
                <p>最高得分: {{ analysisResult.topScore }}</p>
              </template>
              
              <template v-else-if="analysisForm.algorithm === 'community'">
                <h4>社区发现</h4>
                <p>社区数量: {{ analysisResult.communityCount }}</p>
                <p>最大社区节点数: {{ analysisResult.maxCommunitySize }}</p>
              </template>
              
              <template v-else-if="analysisForm.algorithm === 'connectedComponents'">
                <h4>连通分量</h4>
                <p>连通分量数量: {{ analysisResult.componentCount }}</p>
                <p>最大连通分量大小: {{ analysisResult.maxComponentSize }}</p>
              </template>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧图可视化区 -->
      <el-col :span="18" class="visualization-panel">

        <div class="visualization-content">
          <!-- 图可视化区域 -->
          <div ref="graphContainerRef" class="graph-container">
            <svg ref="svgRef" class="graph-svg"></svg>

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
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, onUnmounted, nextTick, watch, computed} from 'vue'
import * as d3 from 'd3'
import {ElMessage} from 'element-plus'
import {useGraphStore} from '@/stores/graph'
import {Plus} from '@element-plus/icons-vue'
import graphApi from '@/api/graph'

// 响应式数据
const analysisFormRef = ref(null)
const graphContainerRef = ref(null)
const svgRef = ref(null)
const graphStore = useGraphStore()
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
  targetValue: ''
})

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

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1500))

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
  if (!graphStore.currentGraph) {
    ElMessage.warning('请先选择一个图')
    return
  }

  try {
    const res = await graphApi.getPublishedSchema(graphStore.currentGraph.id)
    graphSchema.value = res.data || {entities: [], relations: []}

    // 如果有实体，设置默认选中第一个实体
    if (graphSchema.value.entities && graphSchema.value.entities.length > 0) {
      analysisForm.targetEntity = graphSchema.value.entities[0].label
    }

    console.log('获取到图schema信息:', graphSchema.value)
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

  // 力导向模拟tick函数
  function ticked() {
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
  if (simulation) {
    simulation.stop()
    simulation = null
  }

  if (svgRef.value) {
    d3.select(svgRef.value).selectAll('*').remove()
  }
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
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)

  fetchNodeTypes(graphStore.currentGraph.id)
  fetchEdgeTypes(graphStore.currentGraph.id)
  // 监听图变化
  watch(() => graphStore.currentGraph, () => {
    fetchGraphSchema()
  })
})

// 获取点类型列表
const fetchNodeTypes = async (graphId) => {
  try {
    const res = await graphApi.getNodeDefs(graphId)
    nodeTypes.value = res.data
  } catch (e) {
    console.error('获取点类型列表失败:', e)
    ElMessage.error('获取点类型列表失败: ' + (e.message || '未知错误'))
  }
}

// 获取边类型列表
const fetchEdgeTypes = async (graphId) => {
  try {
    const res = await graphApi.getEdgeDefs(graphId)
    edgeTypes.value = res.data
  } catch (e) {
    console.error('获取边类型列表失败:', e)
    ElMessage.error('获取边类型列表失败: ' + (e.message || '未知错误'))
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
</style>
