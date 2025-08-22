<template>
  <div class="graph-detail-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">图设计</h2>
        <p class="page-description">图ID：{{ graphId }}，展示点定义、边定义并支持增删改查</p>
      </div>
      <div class="publish-toolbar" v-if="nodeDefs.length > 0 || edgeDefs.length > 0">
        <el-button 
          type="success" 
          @click="handlePublishSchema" 
          :disabled="isGraphPublished || publishLoading"
          :loading="publishLoading"
        >
          {{ isGraphPublished ? '已发布' : '发布' }}
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="点定义" name="nodes">
        <div class="toolbar">
          <el-button type="primary" @click="handleAddNode">新增点定义</el-button>
        </div>
        <el-table :data="nodeDefs" style="width: 100%" row-key="id">
          <el-table-column prop="label" label="标签" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '已发布' : '未发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                :icon="Edit" 
                size="small" 
                circle
                @click="handleEditNode(row)"
                title="编辑"
              />
              <el-button 
                type="danger" 
                :icon="Delete" 
                size="small" 
                circle
                @click="handleDeleteNode(row)"
                title="删除"
              />
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.properties" size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="code" label="属性标识" min-width="120" />
                <el-table-column prop="name" label="属性名称" min-width="120" />
                <el-table-column prop="type" label="数据类型" min-width="120" />
                <el-table-column prop="indexed" label="索引" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.indexed ? 'success' : 'info'">
                      {{ row.indexed ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                      {{ row.status === 1 ? '已发布' : '未发布' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
        <!-- 点定义弹窗 -->
        <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="800px">
          <el-form :model="nodeForm" label-width="80px">
            <el-form-item label="标签">
              <el-input v-model="nodeForm.label" placeholder="请输入标签" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="nodeForm.name" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="nodeForm.description" placeholder="请输入描述" />
            </el-form-item>
            <el-form-item label="状态">
              <el-tag :type="nodeForm.status === 1 ? 'success' : 'info'">
                {{ nodeForm.status === 1 ? '已发布' : '未发布' }}
              </el-tag>
            </el-form-item>
            <el-form-item label="属性">
              <el-table :data="nodeForm.properties" style="width: 100%">
                <el-table-column label="属性标识" min-width="120">
                  <template #default="{ row, $index }">
                    <el-input 
                      v-model="row.code" 
                      placeholder="属性标识" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="属性名称" min-width="120">
                  <template #default="{ row, $index }">
                    <el-input 
                      v-model="row.name" 
                      placeholder="属性名称" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="数据类型" min-width="120">
                  <template #default="{ row, $index }">
                    <el-select 
                      v-model="row.type" 
                      placeholder="请选择" 
                      :disabled="row.code === 'uid'"
                    >
                      <el-option label="字符串" value="String" />
                      <el-option label="整数" value="Int" />
                      <el-option label="浮点数" value="Float" />
                      <el-option label="布尔值" value="Boolean" />
                      <el-option label="日期" value="Date" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="索引" width="70" align="center">
                  <template #default="{ row, $index }">
                    <el-checkbox 
                      v-model="row.indexed" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                      {{ row.status === 1 ? '已发布' : '未发布' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ row, $index }">
                    <el-button 
                      type="danger" 
                      size="small" 
                      :icon="Delete" 
                      circle
                      @click="removeNodeProperty($index)" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
              </el-table>
              <div style="margin-top: 10px;">
                <el-button type="primary" size="small" @click="addNodeProperty">新增属性</el-button>
              </div>
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="nodeDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="saveNode">确定</el-button>
            </span>
          </template>
        </el-dialog>
      </el-tab-pane>
      <el-tab-pane label="边定义" name="edges">
        <div class="toolbar">
          <el-button type="primary" @click="handleAddEdge">新增边定义</el-button>
        </div>
        <el-table :data="edgeDefs" style="width: 100%" row-key="id">
          <el-table-column prop="label" label="标签" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="from" label="起点类型">
            <template #default="{ row }">
              {{ getNodeNameById(row.from) }}
            </template>
          </el-table-column>
          <el-table-column prop="to" label="终点类型">
            <template #default="{ row }">
              {{ getNodeNameById(row.to) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '已发布' : '未发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                :icon="Edit" 
                size="small" 
                circle
                @click="handleEditEdge(row)"
                title="编辑"
              />
              <el-button 
                type="danger" 
                :icon="Delete" 
                size="small" 
                circle
                @click="handleDeleteEdge(row)"
                title="删除"
              />
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.properties" size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="code" label="属性标识" />
                <el-table-column prop="name" label="属性名称" />
                <el-table-column prop="type" label="数据类型" />
                <el-table-column label="索引" width="70">
                  <template #default="{ row, $index }">
                    <el-checkbox 
                      v-model="row.indexed" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                      {{ row.status === 1 ? '已发布' : '未发布' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row, $index }">
                    <el-button 
                      type="danger" 
                      size="small" 
                      :icon="Delete" 
                      circle
                      @click="removeEdgeProperty($index)" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
        <!-- 边定义弹窗 -->
        <el-dialog v-model="edgeDialogVisible" :title="edgeDialogTitle" width="800px">
          <el-form :model="edgeForm" label-width="80px">
            <el-form-item label="标签">
              <el-input v-model="edgeForm.label" placeholder="请输入标签" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="edgeForm.name" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="起点类型">
              <el-select v-model="edgeForm.from" placeholder="请选择起点类型" style="width: 100%">
                <el-option
                  v-for="node in nodeDefs"
                  :key="node.id"
                  :label="node.name"
                  :value="node.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="终点类型">
              <el-select v-model="edgeForm.to" placeholder="请选择终点类型" style="width: 100%">
                <el-option
                  v-for="node in nodeDefs"
                  :key="node.id"
                  :label="node.name"
                  :value="node.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="edgeForm.description" placeholder="请输入描述" />
            </el-form-item>
            <el-form-item label="状态">
              <el-tag :type="edgeForm.status === 1 ? 'success' : 'info'">
                {{ edgeForm.status === 1 ? '已发布' : '未发布' }}
              </el-tag>
            </el-form-item>
            <el-form-item label="属性">
              <el-table :data="edgeForm.properties" style="width: 100%">
                <el-table-column label="属性标识">
                  <template #default="{ row, $index }">
                    <el-input 
                      v-model="row.code" 
                      placeholder="属性标识" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="属性名称">
                  <template #default="{ row, $index }">
                    <el-input 
                      v-model="row.name" 
                      placeholder="属性名称" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="数据类型">
                  <template #default="{ row, $index }">
                    <el-select 
                      v-model="row.type" 
                      placeholder="请选择" 
                      :disabled="row.code === 'uid'"
                    >
                      <el-option label="字符串" value="String" />
                      <el-option label="整数" value="Int" />
                      <el-option label="布尔值" value="Boolean" />
                      <el-option label="日期" value="Date" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="索引" width="70">
                  <template #default="{ row, $index }">
                    <el-checkbox 
                      v-model="row.indexed" 
                      :disabled="row.code === 'uid'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="状态">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                      {{ row.status === 1 ? '已发布' : '未发布' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row, $index }">
                    <el-button type="danger" size="small" @click="removeEdgeProperty($index)" :disabled="row.code === 'uid'">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="margin-top: 10px;">
                <el-button type="primary" size="small" @click="addEdgeProperty">新增属性</el-button>
              </div>
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="edgeDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="saveEdge">确定</el-button>
            </span>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
    
<!--    &lt;!&ndash; 发布Schema按钮 &ndash;&gt;-->
<!--    <div class="publish-toolbar" v-if="nodeDefs.length > 0 || edgeDefs.length > 0">-->
<!--      <el-button type="success" @click="handlePublishSchema">发布Schema</el-button>-->
<!--    </div>-->
    
    <!-- 发布结果对话框 -->
    <el-dialog v-model="publishResultVisible" title="发布结果" width="500px">
      <el-result 
        :icon="publishResult.success ? 'success' : 'error'" 
        :title="publishResult.success ? '发布成功' : '发布失败'"
        :sub-title="publishResult.message"
      >
        <template #extra v-if="publishResult.details">
          <div style="text-align: left; max-height: 300px; overflow-y: auto;">
            <p><strong>总计:</strong> {{ publishResult.details.totalCount }}</p>
            <p><strong>成功:</strong> {{ publishResult.details.successCount }}</p>
            <p><strong>失败:</strong> {{ publishResult.details.failureCount }}</p>
            <div v-if="publishResult.details.errorMessages && publishResult.details.errorMessages.length > 0">
              <p><strong>错误信息:</strong></p>
              <el-table :data="publishResult.details.errorMessages" size="small">
                <el-table-column prop="message" label="错误详情" />
              </el-table>
            </div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button @click="publishResultVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import { graphApi } from '@/api/graph'

// 引入图store
import { useGraphStore } from '@/stores/graph'

const route = useRoute()
const router = useRouter()
const graphStore = useGraphStore()

// 获取图ID（优先使用全局选中的图，其次使用路由参数）
const graphId = computed(() => {
  return graphStore.currentGraph?.id || route.params.id
})

const graphName = computed(() => {
  return graphStore.currentGraph?.name || ''
})

// 页面加载状态
const loading = ref(false)
const graphInfo = ref({})

// 标签页相关
const activeTab = ref('nodes')

// 点定义相关
const nodeDefs = ref([])
const nodeDialogVisible = ref(false)
const nodeDialogTitle = ref('')
const nodeForm = ref({
  code: '',
  name: '',
  description: '',
  status: 1,
  properties: []
})

// 边定义相关
const edgeDefs = ref([])
const edgeDialogVisible = ref(false)
const edgeDialogTitle = ref('')
const edgeForm = ref({
  code: '',
  name: '',
  from: '',
  to: '',
  description: '',
  status: 'active',
  properties: []
})

// 发布结果相关
const publishResultVisible = ref(false)
const publishResult = ref({
  success: false,
  message: '',
  details: null
})

// 图是否已发布的状态
const isGraphPublished = ref(false)

// 发布按钮加载状态
const publishLoading = ref(false)

// 计算属性：检查所有节点和边是否都已发布
const checkIfGraphPublished = computed(() => {
  // 如果没有节点和边定义，则认为未发布
  if (nodeDefs.value.length === 0 && edgeDefs.value.length === 0) {
    return false
  }
  
  // 检查所有节点是否已发布
  const allNodesPublished = nodeDefs.value.every(node => node.status === 1)
  
  // 检查所有边是否已发布
  const allEdgesPublished = edgeDefs.value.every(edge => edge.status === 1)
  
  // 只有当所有节点和边都已发布时，才认为图已发布
  return allNodesPublished && allEdgesPublished
})

// 获取图详情
const fetchGraphDetail = async () => {
  try {
    loading.value = true
    // 使用计算后的graphId
    const response = await graphApi.getGraph(graphId.value)
    graphInfo.value = response.data
    
    // 如果全局没有选中图，则设置当前图
    if (!graphStore.currentGraph && response.data) {
      graphStore.setCurrentGraph(response.data)
    }
  } catch (error) {
    console.error('获取图详情失败:', error)
    ElMessage.error('获取图详情失败')
  } finally {
    loading.value = false
  }
}

// 获取点定义列表
const fetchNodeDefs = async () => {
  if (!graphId.value) return
  
  try {
    console.log("graphId", graphId.value)
    const res = await graphApi.getNodeDefs(graphId.value)
    nodeDefs.value = res.data
    // 更新图发布状态
    isGraphPublished.value = checkIfGraphPublished.value
  } catch (e) {
    ElMessage.error('获取点定义列表失败')
  }
}

// 获取边定义列表
const fetchEdgeDefs = async () => {
  if (!graphId.value) return
  
  try {
    const res = await graphApi.getEdgeDefs(graphId.value)
    edgeDefs.value = res.data
    // 更新图发布状态
    isGraphPublished.value = checkIfGraphPublished.value
  } catch (e) {
    ElMessage.error('获取边定义列表失败')
  }
}

// 根据ID获取节点名称
const getNodeNameById = (id) => {
  const node = nodeDefs.value.find(node => node.id == id)
  return node ? node.name : '未知节点'
}

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'nodes') {
    fetchNodeDefs()
  } else if (tab === 'edges') {
    fetchEdgeDefs()
  }
}

// 新增点定义
const handleAddNode = () => {
  nodeForm.value = {
    code: '',
    name: '',
    description: '',
    status: 0,
    properties: [
      {
        code: 'uid',
        name: '唯一标识',
        type: 'String',
        desc: '节点唯一标识符',
        status: 0,
        indexed: true
      }
    ]
  }
  nodeDialogTitle.value = '新增点定义'
  nodeDialogVisible.value = true
}

// 编辑点定义
const handleEditNode = (row) => {
  nodeForm.value = {
    ...row,
    status: 0, // 编辑时状态默认为未发布
    properties: row.properties ? [...row.properties] : []
  }
  nodeDialogTitle.value = '编辑点定义'
  nodeDialogVisible.value = true
}

// 删除点定义
const handleDeleteNode = (row) => {
  ElMessageBox.confirm('确定要删除该点定义吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await graphApi.deleteNodeDef(graphId.value, row.id)
      ElMessage.success('删除成功')
      fetchNodeDefs()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 保存点定义
const saveNode = async () => {
  try {
    // 不再过滤uid属性，让后端处理
    const nodeData = { ...nodeForm.value }
    
    if (nodeForm.value.id) {
      // 更新
      await graphApi.updateNodeDef(graphId.value, nodeForm.value.id, nodeData)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await graphApi.addNodeDef(graphId.value, nodeData)
      ElMessage.success('新增成功')
    }
    nodeDialogVisible.value = false
    fetchNodeDefs()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// 新增点属性
const addNodeProperty = () => {
  nodeForm.value.properties.push({
    code: '',
    name: '',
    type: 'String',
    status: 0
  })
}

// 删除点属性
const removeNodeProperty = (index) => {
  nodeForm.value.properties.splice(index, 1)
}

// 新增边定义
const handleAddEdge = () => {
  edgeForm.value = {
    code: '',
    name: '',
    from: '',
    to: '',
    description: '',
    status: 0, // 状态值保持为整型，新增默认未发布
    properties: [
      {
        code: 'uid',
        name: '唯一标识',
        type: 'String',
        desc: '边唯一标识符',
        status: 0, // 属性状态值保持为整型，新增默认未发布
        indexed: true
      }
    ]
  }
  edgeDialogTitle.value = '新增边定义'
  edgeDialogVisible.value = true
}

// 编辑边定义
const handleEditEdge = (row) => {
  edgeForm.value = {
    ...row,
    properties: row.properties ? [...row.properties] : []
  }
  edgeDialogTitle.value = '编辑边定义'
  edgeDialogVisible.value = true
}

// 删除边定义
const handleDeleteEdge = (row) => {
  ElMessageBox.confirm('确定要删除该边定义吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await graphApi.deleteEdgeDef(graphId.value, row.id)
      ElMessage.success('删除成功')
      fetchEdgeDefs()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 保存边定义
const saveEdge = async () => {
  try {
    // 不再过滤uid属性，让后端处理
    const edgeData = { ...edgeForm.value }
    
    if (edgeForm.value.id) {
      // 更新
      await graphApi.updateEdgeDef(graphId.value, edgeForm.value.id, edgeData)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await graphApi.addEdgeDef(graphId.value, edgeData)
      ElMessage.success('新增成功')
    }
    edgeDialogVisible.value = false
    fetchEdgeDefs()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// 新增边属性
const addEdgeProperty = () => {
  edgeForm.value.properties.push({
    code: '',
    name: '',
    type: 'String',
    status: 0
  })
}

// 删除边属性
const removeEdgeProperty = (index) => {
  edgeForm.value.properties.splice(index, 1)
}

// 发布Schema到图数据库
const handlePublishSchema = async () => {
  // 设置加载状态
  publishLoading.value = true
  try {
    const res = await graphApi.publishSchema(graphId.value)
    publishResult.value = {
      success: res.code === 200,
      message: res.code === 200 ? 'Schema发布成功' : 'Schema发布完成，但存在错误',
      details: res.data
    }
    publishResultVisible.value = true
    
    // 发布成功后更新状态
    if (res.code === 200) {
      isGraphPublished.value = true
      // 重新获取数据以更新状态显示
      fetchNodeDefs()
      fetchEdgeDefs()
    }
  } catch (e) {
    ElMessage.error('发布失败: ' + (e.message || '未知错误'))
  } finally {
    // 无论成功或失败都取消加载状态
    publishLoading.value = false
  }
}

// 监听全局图切换
watch(() => graphStore.currentGraph, (newGraph, oldGraph) => {
  if (newGraph && newGraph.id !== (oldGraph?.id)) {
    // 图发生切换，重新获取点边定义
    fetchNodeDefs()
    // 如果当前在边定义标签页，也获取边定义
    if (activeTab.value === 'edges') {
      fetchEdgeDefs()
    }
  }
})

onMounted(() => {
  if (graphId.value) {
    fetchNodeDefs()
  }
})
</script>

<style scoped>
.graph-detail-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  position: relative;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.page-description {
  color: #666;
  font-size: 14px;
}

/* 优化图信息展示区域样式 */
.graph-info-card {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
}

.info-label {
  font-weight: bold;
  color: #606266;
  font-size: 14px;
  margin-right: 10px;
  white-space: nowrap;
}

.info-value {
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.publish-toolbar {
  position: absolute;
  top: 120px;
  right: 20px;
  z-index: 1000;
}
</style>
