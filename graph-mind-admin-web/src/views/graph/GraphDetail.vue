<template>
  <div class="graph-detail-container">
    <div class="page-header">
      <h2 class="page-title">图详情</h2>
      <p class="page-description">图ID：{{ graphId }}，展示点定义、边定义并支持增删改查</p>
    </div>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="点定义" name="nodes">
        <div class="toolbar">
          <el-button type="primary" @click="handleAddNode">新增点定义</el-button>
        </div>
        <el-table :data="nodeDefs" style="width: 100%" row-key="id">
          <el-table-column prop="code" label="标识" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="handleEditNode(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteNode(row)">删除</el-button>
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.properties" size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="code" label="属性标识" />
                <el-table-column prop="name" label="属性名称" />
                <el-table-column prop="type" label="数据类型" />
                <el-table-column prop="status" label="状态">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                      {{ row.status === 'active' ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
        <!-- 点定义弹窗 -->
        <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="600px">
          <el-form :model="nodeForm" label-width="80px">
            <el-form-item label="标识">
              <el-input v-model="nodeForm.code" placeholder="请输入标识" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="nodeForm.name" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="nodeForm.description" placeholder="请输入描述" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="nodeForm.status" style="width: 120px">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="属性列表">
              <div v-for="(prop, idx) in nodeForm.properties" :key="idx" style="margin-bottom: 8px;">
                <el-row :gutter="10" style="align-items: center;">
                  <el-col :span="5">
                    <el-input v-model="prop.code" placeholder="属性标识" />
                  </el-col>
                  <el-col :span="5">
                    <el-input v-model="prop.name" placeholder="属性名称" />
                  </el-col>
                  <el-col :span="5">
                    <el-select v-model="prop.type" placeholder="数据类型">
                      <el-option v-for="item in propertyTypes" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-col>
                  <el-col :span="5">
                    <el-select v-model="prop.status" placeholder="状态">
                      <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-col>
                  <el-col :span="4">
                    <el-button size="small" type="danger" @click="handleRemoveNodeProperty(idx)" v-if="nodeForm.properties.length > 1">
                      删除
                    </el-button>
                  </el-col>
                </el-row>
              </div>
              <el-button size="small" @click="handleAddNodeProperty">添加属性</el-button>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="nodeDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleNodeDialogOk">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
      <el-tab-pane label="边定义" name="edges">
        <div class="toolbar">
          <el-button type="primary" @click="handleAddEdge">新增边定义</el-button>
        </div>
        <el-table :data="edgeDefs" style="width: 100%" row-key="id">
          <el-table-column prop="code" label="标识" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="from" label="起点类型" />
          <el-table-column prop="to" label="终点类型" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="handleEditEdge(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteEdge(row)">删除</el-button>
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.properties" size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="code" label="属性标识" />
                <el-table-column prop="name" label="属性名称" />
                <el-table-column prop="type" label="数据类型" />
                <el-table-column prop="status" label="状态">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                      {{ row.status === 'active' ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
        <!-- 边定义弹窗 -->
        <el-dialog v-model="edgeDialogVisible" :title="edgeDialogTitle" width="650px">
          <el-form :model="edgeForm" label-width="80px">
            <el-form-item label="标识">
              <el-input v-model="edgeForm.code" placeholder="请输入标识" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="edgeForm.name" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="起点类型">
              <el-select v-model="edgeForm.from" placeholder="请选择起点类型">
                <el-option
                    v-for="node in nodeDefs"
                    :key="node.id"
                    :label="node.name"
                    :value="node.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="终点类型">
              <el-select v-model="edgeForm.to" placeholder="请选择终点类型">
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
              <el-select v-model="edgeForm.status" style="width: 120px">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="属性列表">
              <div v-for="(prop, idx) in edgeForm.properties" :key="idx" style="margin-bottom: 8px;">
                <el-row :gutter="10" style="align-items: center;">
                  <el-col :span="5">
                    <el-input v-model="prop.code" placeholder="属性标识" />
                  </el-col>
                  <el-col :span="5">
                    <el-input v-model="prop.name" placeholder="属性名称" />
                  </el-col>
                  <el-col :span="5">
                    <el-select v-model="prop.type" placeholder="数据类型">
                      <el-option v-for="item in propertyTypes" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-col>
                  <el-col :span="5">
                    <el-select v-model="prop.status" placeholder="状态">
                      <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-col>
                  <el-col :span="4">
                    <el-button size="small" type="danger" @click="handleRemoveEdgeProperty(idx)" v-if="edgeForm.properties.length > 1">
                      删除
                    </el-button>
                  </el-col>
                </el-row>
              </div>
              <el-button size="small" @click="handleAddEdgeProperty">添加属性</el-button>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="edgeDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleEdgeDialogOk">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="GraphDetail">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { graphApi } from '@/api/graph'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const graphId = route.params.id
const activeTab = ref('nodes')

const nodeDefs = ref([])
const edgeDefs = ref([])

const nodeDialogVisible = ref(false)
const nodeDialogTitle = ref('')
const nodeForm = ref({ id: '', code: '', name: '', description: '', status: 'active', properties: [] })
const editingNodeId = ref(null)

const edgeDialogVisible = ref(false)
const edgeDialogTitle = ref('')
const edgeForm = ref({ id: '', code: '', name: '', from: '', to: '', description: '', status: 'active', properties: [] })
const editingEdgeId = ref(null)

const propertyTypes = [
  { label: '字符串', value: 'string' },
  { label: '整数', value: 'int' },
  { label: '日期', value: 'date' },
  { label: '浮点数', value: 'float' }
]

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' }
]

const fetchNodeDefs = async () => {
  try {
    const res = await graphApi.getNodeDefs(graphId)
    nodeDefs.value = res.data
  } catch (e) {
    ElMessage.error('获取点定义失败')
  }
}

const fetchEdgeDefs = async () => {
  try {
    const res = await graphApi.getEdgeDefs(graphId)
    edgeDefs.value = res.data
  } catch (e) {
    ElMessage.error('获取边定义失败')
  }
}

// 处理标签页切换
const handleTabChange = (tabName) => {
  if (tabName === 'nodes') {
    fetchNodeDefs()
  } else if (tabName === 'edges') {
    fetchEdgeDefs()
  }
}

onMounted(() => {
  fetchNodeDefs()
})

// 点定义操作
const handleAddNode = () => {
  nodeDialogTitle.value = '新增点定义'
  nodeForm.value = { id: '', code: '', name: '', description: '', status: 'active', properties: [] }
  editingNodeId.value = null
  nodeDialogVisible.value = true
}
const handleEditNode = (row) => {
  nodeDialogTitle.value = '编辑点定义'
  nodeForm.value = JSON.parse(JSON.stringify(row))
  editingNodeId.value = row.id
  nodeDialogVisible.value = true
}
const handleDeleteNode = (row) => {
  ElMessageBox.confirm(`确定要删除点类型"${row.name}"吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await graphApi.deleteNodeDef(graphId, row.id)
      ElMessage.success('删除成功')
      fetchNodeDefs()
    })
    .catch(() => {})
}

const handleNodeDialogOk = async () => {
  if (!nodeForm.value.name) {
    ElMessage.error('请输入点类型名称')
    return
  }
  
  try {
    if (editingNodeId.value) {
      console.log('更新点定义:', graphId, editingNodeId.value, nodeForm.value)
      await graphApi.updateNodeDef(graphId, editingNodeId.value, nodeForm.value)
      ElMessage.success('编辑成功')
    } else {
      console.log('新增点定义:', graphId, nodeForm.value)
      await graphApi.addNodeDef(graphId, nodeForm.value)
      ElMessage.success('新增成功')
    }
    nodeDialogVisible.value = false
    fetchNodeDefs()
  } catch (error) {
    console.error('保存点定义失败:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  }
}

const handleAddNodeProperty = () => {
  nodeForm.value.properties.push({ id: '', code: '', name: '', type: 'string', status: 'active' })
}
const handleRemoveNodeProperty = (idx) => {
  nodeForm.value.properties.splice(idx, 1)
}

// 边定义操作
const handleAddEdge = () => {
  edgeDialogTitle.value = '新增边定义'
  edgeForm.value = { id: '', code: '', name: '', from: '', to: '', description: '', status: 'active', properties: [] }
  editingEdgeId.value = null
  edgeDialogVisible.value = true
}
const handleEditEdge = (row) => {
  edgeDialogTitle.value = '编辑边定义'
  edgeForm.value = JSON.parse(JSON.stringify(row))
  editingEdgeId.value = row.id
  edgeDialogVisible.value = true
}
const handleDeleteEdge = (row) => {
  ElMessageBox.confirm(`确定要删除边类型"${row.name}"吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await graphApi.deleteEdgeDef(graphId, row.id)
      ElMessage.success('删除成功')
      fetchEdgeDefs()
    })
    .catch(() => {})
}

const handleEdgeDialogOk = async () => {
  if (!edgeForm.value.name) {
    ElMessage.error('请输入边类型名称')
    return
  }
  if (!edgeForm.value.from || !edgeForm.value.to) {
    ElMessage.error('请选择起点类型和终点类型')
    return
  }
  
  try {
    // 修复错误的判断条件
    if (editingEdgeId.value) {
      console.log('更新边定义:', graphId, editingEdgeId.value, edgeForm.value)
      await graphApi.updateEdgeDef(graphId, editingEdgeId.value, edgeForm.value)
      ElMessage.success('编辑成功')
    } else {
      console.log('新增边定义:', graphId, edgeForm.value)
      await graphApi.addEdgeDef(graphId, edgeForm.value)
      ElMessage.success('新增成功')
    }
    edgeDialogVisible.value = false
    fetchEdgeDefs()
  } catch (error) {
    console.error('保存边定义失败:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  }
}

const handleAddEdgeProperty = () => {
  edgeForm.value.properties.push({ id: '', code: '', name: '', type: 'string', status: 'active' })
}
const handleRemoveEdgeProperty = (idx) => {
  edgeForm.value.properties.splice(idx, 1)
}
</script>

<style scoped>
.graph-detail-container {
  padding: 20px;
}
.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}
.page-description {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
.toolbar {
  margin-bottom: 10px;
}
</style>