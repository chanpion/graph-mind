<template>
  <div class="data-modeling">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">图建模</h2>
      </div>
      <div class="page-header-right">
        <el-radio-group v-model="viewMode" size="small" class="view-mode-toggle">
          <el-radio-button value="list">列表视图</el-radio-button>
          <el-radio-button value="graph">图析视图</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="content-card">
      <div class="list-toolbar">
        <el-button type="primary" :icon="Plus" size="small" @click="handleAdd">新增</el-button>
        <el-button size="small" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button size="small" @click="handleImportClick">
          <el-icon><Upload /></el-icon>
          导入
        </el-button>
        <input ref="fileInputRef" type="file" accept=".json" style="display:none" @change="handleFileChange" />
      </div>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="schema-tabs">
        <el-tab-pane label="点定义" name="nodes">
          <el-table :data="nodeDefs" style="width: 100%" row-key="id" v-loading="loading">
          <el-table-column prop="label" label="标签" min-width="120" />
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="description" label="描述" min-width="180" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '已发布' : '未发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" :icon="Edit" size="small" circle @click="handleEditNode(row)" title="编辑" />
              <el-button type="danger" :icon="Delete" size="small" circle @click="handleDeleteNode(row)" title="删除" />
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.properties" size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="code" label="属性标识" min-width="120" />
                <el-table-column prop="name" label="属性名称" min-width="120" />
                <el-table-column prop="type" label="数据类型" min-width="120" />
                <el-table-column prop="indexed" label="索引" width="70" align="center">
                  <template #default="{ row: prop }">
                    <el-tag :type="prop.indexed ? 'success' : 'info'">{{ prop.indexed ? '是' : '否' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row: prop }">
                    <el-tag :type="prop.status === 1 ? 'success' : 'info'">{{ prop.status === 1 ? '已发布' : '未发布' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
        </el-tab-pane>
        <el-tab-pane label="边定义" name="edges">
          <el-table :data="edgeDefs" style="width: 100%" row-key="id" v-loading="loading">
          <el-table-column prop="label" label="标签" min-width="120" />
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column label="起点类型" min-width="120">
            <template #default="{ row }">{{ getNodeNameById(row.from) }}</template>
          </el-table-column>
          <el-table-column label="终点类型" min-width="120">
            <template #default="{ row }">{{ getNodeNameById(row.to) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '未发布' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" :icon="Edit" size="small" circle @click="handleEditEdge(row)" title="编辑" />
              <el-button type="danger" :icon="Delete" size="small" circle @click="handleDeleteEdge(row)" title="删除" />
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.properties" size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="code" label="属性标识" min-width="120" />
                <el-table-column prop="name" label="属性名称" min-width="120" />
                <el-table-column prop="type" label="数据类型" min-width="120" />
                <el-table-column label="索引" width="70" align="center">
                  <template #default="{ row: prop }">
                    <el-tag :type="prop.indexed ? 'success' : 'info'">{{ prop.indexed ? '是' : '否' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row: prop }">
                    <el-tag :type="prop.status === 1 ? 'success' : 'info'">{{ prop.status === 1 ? '已发布' : '未发布' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

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
          <el-tag :type="nodeForm.status === 1 ? 'success' : 'info'">{{ nodeForm.status === 1 ? '已发布' : '未发布' }}</el-tag>
        </el-form-item>
        <el-form-item label="属性">
          <el-table :data="nodeForm.properties" style="width: 100%">
            <el-table-column label="属性标识" min-width="120">
              <template #default="{ row, $index }">
                <el-input v-model="row.code" placeholder="属性标识" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
            <el-table-column label="属性名称" min-width="120">
              <template #default="{ row, $index }">
                <el-input v-model="row.name" placeholder="属性名称" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
            <el-table-column label="数据类型" min-width="120">
              <template #default="{ row, $index }">
                <el-select v-model="row.type" placeholder="请选择" :disabled="row.code === 'uid'">
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
                <el-checkbox v-model="row.indexed" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row, $index }">
                <el-button type="danger" size="small" :icon="Delete" circle @click="removeNodeProperty($index)" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 10px;">
            <el-button type="primary" size="small" @click="addNodeProperty">新增属性</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="nodeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveNode" :loading="saving">确定</el-button>
        </div>
      </template>
    </el-dialog>

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
            <el-option v-for="node in nodeDefs" :key="node.id" :label="node.name" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="终点类型">
          <el-select v-model="edgeForm.to" placeholder="请选择终点类型" style="width: 100%">
            <el-option v-for="node in nodeDefs" :key="node.id" :label="node.name" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="edgeForm.description" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="edgeForm.status === 1 ? 'success' : 'info'">{{ edgeForm.status === 1 ? '已发布' : '未发布' }}</el-tag>
        </el-form-item>
        <el-form-item label="属性">
          <el-table :data="edgeForm.properties" style="width: 100%">
            <el-table-column label="属性标识" min-width="120">
              <template #default="{ row, $index }">
                <el-input v-model="row.code" placeholder="属性标识" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
            <el-table-column label="属性名称" min-width="120">
              <template #default="{ row, $index }">
                <el-input v-model="row.name" placeholder="属性名称" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
            <el-table-column label="数据类型" min-width="120">
              <template #default="{ row, $index }">
                <el-select v-model="row.type" placeholder="请选择" :disabled="row.code === 'uid'">
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
                <el-checkbox v-model="row.indexed" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row, $index }">
                <el-button type="danger" size="small" :icon="Delete" circle @click="removeEdgeProperty($index)" :disabled="row.code === 'uid'" />
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 10px;">
            <el-button type="primary" size="small" @click="addEdgeProperty">新增属性</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="edgeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdge" :loading="saving">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入确认弹窗 -->
    <el-dialog v-model="importDialogVisible" title="导入确认" width="600px">
      <div class="import-summary">
        <p>检测到以下定义，是否导入？</p>
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            点定义 {{ importData.nodes?.length || 0 }} 个 · 边定义 {{ importData.edges?.length || 0 }} 个
          </template>
        </el-alert>
        <div class="import-preview" v-if="importData.nodes?.length">
          <div class="preview-title">点定义预览：</div>
          <div v-for="n in importData.nodes.slice(0, 5)" :key="n.id || n.label" class="preview-item">
            <el-tag size="small" type="success">{{ n.label }}</el-tag> {{ n.name }}
          </div>
          <div v-if="importData.nodes.length > 5" class="preview-more">
            ... 还有 {{ importData.nodes.length - 5 }} 个
          </div>
        </div>
        <div class="import-preview" v-if="importData.edges?.length">
          <div class="preview-title">边定义预览：</div>
          <div v-for="e in importData.edges.slice(0, 5)" :key="e.id || e.label" class="preview-item">
            <el-tag size="small" type="warning">{{ e.label }}</el-tag> {{ e.name }}
          </div>
          <div v-if="importData.edges.length > 5" class="preview-more">
            ... 还有 {{ importData.edges.length - 5 }} 个
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleImportClean" :loading="importing">仅导入新增</el-button>
        <el-button type="primary" @click="handleImportMerge" :loading="importing">合并导入</el-button>
      </template>
    </el-dialog>

    <!-- 图析视图 -->
    <div v-if="viewMode === 'graph'" class="graph-view-wrapper">
      <GraphModelingView :node-defs="nodeDefs" :edge-defs="edgeDefs" />
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Plus, Download, Upload } from '@element-plus/icons-vue'
import { graphApi } from '@/views/graphs/api/graph'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import GraphModelingView from './components/GraphModelingView.vue'

const graphsStore = useGraphsStore()

const currentGraphId = computed(() => graphsStore.currentGraphId)

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const importing = ref(false)
const fileInputRef = ref(null)
const importDialogVisible = ref(false)
const importData = ref({ nodes: [], edges: [] })
const activeTab = ref('nodes')
const viewMode = ref('list')

const nodeDefs = ref([])
const edgeDefs = ref([])

const nodeDialogVisible = ref(false)
const nodeDialogTitle = ref('')
const nodeForm = ref({
  label: '',
  name: '',
  description: '',
  status: 0,
  properties: []
})

const edgeDialogVisible = ref(false)
const edgeDialogTitle = ref('')
const edgeForm = ref({
  label: '',
  name: '',
  from: '',
  to: '',
  description: '',
  status: 0,
  properties: []
})

async function autoPublish() {
  if (!currentGraphId.value) return
  try {
    await graphApi.publishSchema(currentGraphId.value)
  } catch (e) {
    // auto-publish 静默处理
  }
}

async function fetchNodeDefs() {
  if (!currentGraphId.value) return
  loading.value = true
  try {
    const res = await graphApi.getNodeDefs(currentGraphId.value)
    nodeDefs.value = res?.data || res || []
  } catch (e) {
    ElMessage.error('获取点定义失败')
  } finally {
    loading.value = false
  }
}

async function fetchEdgeDefs() {
  if (!currentGraphId.value) return
  loading.value = true
  try {
    const res = await graphApi.getEdgeDefs(currentGraphId.value)
    edgeDefs.value = res?.data || res || []
  } catch (e) {
    ElMessage.error('获取边定义失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab) {
  if (tab === 'edges') {
    fetchEdgeDefs()
  } else {
    fetchNodeDefs()
  }
}

function getNodeNameById(id) {
  const node = nodeDefs.value.find(n => n.id == id)
  return node ? node.name : '未知节点'
}

function handleAdd() {
  if (activeTab.value === 'nodes') {
    handleAddNode()
  } else {
    handleAddEdge()
  }
}

function handleAddNode() {
  nodeForm.value = {
    label: '',
    name: '',
    description: '',
    status: 0,
    properties: [{ code: 'uid', name: '唯一标识', type: 'String', status: 0, indexed: true }]
  }
  nodeDialogTitle.value = '新增点定义'
  nodeDialogVisible.value = true
}

function handleEditNode(row) {
  nodeForm.value = {
    ...row,
    properties: row.properties ? [...row.properties] : []
  }
  nodeDialogTitle.value = '编辑点定义'
  nodeDialogVisible.value = true
}

async function handleDeleteNode(row) {
  try {
    await ElMessageBox.confirm('确定要删除该点定义吗？', '提示', { type: 'warning' })
    await graphApi.deleteNodeDef(currentGraphId.value, row.id)
    ElMessage.success('删除成功')
    await fetchNodeDefs()
    await autoPublish()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function saveNode() {
  saving.value = true
  try {
    const data = { ...nodeForm.value }
    if (nodeForm.value.id) {
      await graphApi.updateNodeDef(currentGraphId.value, nodeForm.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await graphApi.addNodeDef(currentGraphId.value, data)
      ElMessage.success('新增成功')
    }
    nodeDialogVisible.value = false
    await fetchNodeDefs()
    await autoPublish()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function addNodeProperty() {
  nodeForm.value.properties.push({ code: '', name: '', type: 'String', status: 0, indexed: false })
}

function removeNodeProperty(index) {
  nodeForm.value.properties.splice(index, 1)
}

function handleAddEdge() {
  edgeForm.value = {
    label: '',
    name: '',
    from: '',
    to: '',
    description: '',
    status: 0,
    properties: [{ code: 'uid', name: '唯一标识', type: 'String', status: 0, indexed: true }]
  }
  edgeDialogTitle.value = '新增边定义'
  edgeDialogVisible.value = true
}

function handleEditEdge(row) {
  edgeForm.value = {
    ...row,
    properties: row.properties ? [...row.properties] : []
  }
  edgeDialogTitle.value = '编辑边定义'
  edgeDialogVisible.value = true
}

async function handleDeleteEdge(row) {
  try {
    await ElMessageBox.confirm('确定要删除该边定义吗？', '提示', { type: 'warning' })
    await graphApi.deleteEdgeDef(currentGraphId.value, row.id)
    ElMessage.success('删除成功')
    await fetchEdgeDefs()
    await autoPublish()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function saveEdge() {
  saving.value = true
  try {
    const data = { ...edgeForm.value }
    if (edgeForm.value.id) {
      await graphApi.updateEdgeDef(currentGraphId.value, edgeForm.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await graphApi.addEdgeDef(currentGraphId.value, data)
      ElMessage.success('新增成功')
    }
    edgeDialogVisible.value = false
    await fetchEdgeDefs()
    await autoPublish()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function addEdgeProperty() {
  edgeForm.value.properties.push({ code: '', name: '', type: 'String', status: 0, indexed: false })
}

function removeEdgeProperty(index) {
  edgeForm.value.properties.splice(index, 1)
}

// ==================== 导入导出 ====================
function handleImportClick() {
  fileInputRef.value?.click()
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    if (!data.nodes && !data.edges) {
      ElMessage.error('文件格式错误，缺少 nodes 或 edges 字段')
      return
    }
    importData.value = data
    importDialogVisible.value = true
  } catch (e) {
    ElMessage.error('解析 JSON 文件失败')
  }
  event.target.value = ''
}

async function handleImportMerge() {
  importing.value = true
  try {
    await graphApi.importSchema(currentGraphId.value, {
      mode: 'merge',
      nodes: importData.value.nodes,
      edges: importData.value.edges
    })
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    await fetchNodeDefs()
    await fetchEdgeDefs()
    await autoPublish()
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

async function handleImportClean() {
  importing.value = true
  try {
    await graphApi.importSchema(currentGraphId.value, {
      mode: 'replace',
      nodes: importData.value.nodes,
      edges: importData.value.edges
    })
    ElMessage.success('导入成功（已覆盖）')
    importDialogVisible.value = false
    await fetchNodeDefs()
    await fetchEdgeDefs()
    await autoPublish()
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

async function handleExport() {
  if (!currentGraphId.value) {
    ElMessage.warning('请先选择图')
    return
  }
  exporting.value = true
  try {
    const res = await graphApi.exportSchema(currentGraphId.value)
    const data = res?.data || res
    const exportObj = {
      version: data.version || '1.0',
      exportedAt: new Date().toISOString(),
      graphId: currentGraphId.value,
      graphCode: data.graphCode,
      nodes: data.nodes || [],
      edges: data.edges || []
    }
    const blob = new Blob([JSON.stringify(exportObj, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `schema-${currentGraphId.value}-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

watch(() => graphsStore.currentGraphId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    nodeDefs.value = []
    edgeDefs.value = []
    fetchNodeDefs()
    if (viewMode.value === 'graph') {
      fetchEdgeDefs()
    }
  }
})

watch(viewMode, (mode) => {
  if (mode === 'graph') {
    fetchEdgeDefs()
  }
}, { immediate: true })

onMounted(async () => {
  if (graphsStore.graphs.length === 0) {
    await graphsStore.fetchGraphs()
  }
  if (currentGraphId.value) {
    fetchNodeDefs()
  }
})
</script>

<style scoped>
.data-modeling {
  padding: 5px;
  position: relative;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header-left {
  flex: 1;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  margin: 0 0 8px;
}

.graph-name-tag {
  font-size: 16px;
  font-weight: normal;
  color: var(--el-text-color-secondary);
  margin-left: 8px;
}

.page-description {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.tabs-with-action {
  display: flex;
  flex-direction: column;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-mode-toggle {
  margin-right: 4px;
}

.graph-view-wrapper {
  height: 500px;
}

.content-card {
  border-radius: 8px;
  padding: 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.list-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  justify-content: flex-end;
}

.schema-tabs {
  margin-top: 12px;
}

.import-summary { display: flex; flex-direction: column; gap: 12px; }
.import-preview { background: var(--el-fill-color-light); border-radius: 6px; padding: 10px 14px; }
.preview-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 6px; }
.preview-item { display: flex; align-items: center; gap: 6px; font-size: 13px; margin-bottom: 4px; }
.preview-more { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
</style>
