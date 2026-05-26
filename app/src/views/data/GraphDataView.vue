<template>
  <div class="graph-data">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="220px">
        <div class="sidebar-header">
          <div class="sidebar-header-top">
            <h3>图数据</h3>
            <el-button size="small" @click="refreshAll">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="sidebar-content">
          <!-- 节点类型 -->
          <div class="section">
            <div class="section-header">
              <span class="section-icon"><el-icon><Connection /></el-icon></span>
              <h4>节点类型</h4>
              <span class="section-count">{{ nodeTypes.length }}</span>
            </div>
            <div class="label-tags" v-if="nodeTypes.length">
              <div
                v-for="t in nodeTypes"
                :key="t.id"
                class="label-tag"
                :class="{ active: selectedType === 'node' && selectedTypeId === t.id }"
                @click="selectNodeType(t)"
              >
                <span class="tag-text">{{ t.name || t.label }}</span>
              </div>
            </div>
            <el-empty v-else-if="graphLoaded" description="无节点类型" :image-size="60" />
          </div>
          <!-- 边类型 -->
          <div class="section">
            <div class="section-header">
              <span class="section-icon edge-icon"><el-icon><Share /></el-icon></span>
              <h4>边类型</h4>
              <span class="section-count">{{ edgeTypes.length }}</span>
            </div>
            <div class="label-tags" v-if="edgeTypes.length">
              <div
                v-for="t in edgeTypes"
                :key="t.id"
                class="label-tag edge-tag"
                :class="{ active: selectedType === 'edge' && selectedTypeId === t.id }"
                @click="selectEdgeType(t)"
              >
                <span class="tag-text">{{ t.name || t.label }}</span>
              </div>
            </div>
            <el-empty v-else-if="graphLoaded" description="无边类型" :image-size="60" />
          </div>
        </div>
      </el-aside>

      <!-- 主内容 -->
      <el-main>
        <div class="data-content" v-if="selectedTypeId">
          <!-- 数据卡片：统一容器 -->
          <div class="data-card">
          <!-- 操作栏 -->
          <div class="data-header">
            <div class="header-left">
              <h3>{{ selectedType === 'node' ? '节点数据' : '边数据' }}</h3>
              <el-tag :type="selectedType === 'node' ? 'primary' : 'success'" size="small" effect="plain">
                {{ selectedType === 'node' ? '节点' : '边' }}
              </el-tag>
              <span class="data-count">共 <strong>{{ total }}</strong> 条</span>
            </div>
            <div class="header-actions">
              <el-input
                v-model="searchText"
                :placeholder="selectedType === 'node' ? '搜索节点...' : '搜索边...'"
                size="small"
                style="width: 200px"
                clearable
                @input="onSearchInput"
              />
              <el-dropdown @command="exportData">
                <el-button size="small" type="primary">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="csv">导出 CSV</el-dropdown-item>
                    <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button size="small" type="primary" @click="showImportDialog">
                <el-icon><Upload /></el-icon> 导入
              </el-button>
              <el-button type="primary" size="small" @click="showCreateDialog">
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </div>

          <!-- 数据表格 -->
          <div class="table-wrapper">
          <el-table
            :data="tableData"
            stripe
            border
            height="100%"
            v-loading="loading"
          >
            <el-table-column prop="uid" label="UID" width="180" show-overflow-tooltip />
            <el-table-column v-if="selectedType === 'edge'" prop="sourceUid" label="起点" width="150" show-overflow-tooltip />
            <el-table-column v-if="selectedType === 'edge'" prop="targetUid" label="终点" width="150" show-overflow-tooltip />
            <el-table-column label="属性" min-width="280">
              <template #default="{ row }">
                <div class="props-display">
                  <span
                    v-for="(val, key) in row.properties"
                    :key="key"
                    class="prop-item"
                  >
                    <span class="prop-key">{{ key }}</span>
                    <span class="prop-val">{{ val }}</span>
                  </span>
                  <span v-if="!row.properties || Object.keys(row.properties).length === 0" class="no-prop">-</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button type="primary" size="small" :icon="Edit" circle @click="showEditDialog(row)" title="编辑" />
                  <el-button type="danger" size="small" :icon="Delete" circle @click="handleDelete(row)" title="删除" />
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div> <!-- end pagination-wrap -->
          </div> <!-- end data-card -->
        </div>

        <div v-else class="empty-hint">
          <el-empty description="请从左侧选择节点或边类型" />
        </div>
      </el-main>
    </el-container>

    <!-- 创建/编辑节点对话框 -->
    <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="600px">
      <el-form :model="nodeForm" label-width="90px">
        <el-form-item label="标签" required>
          <el-select v-model="nodeForm.label" placeholder="选择节点标签" style="width: 100%">
            <el-option v-for="t in nodeTypes" :key="t.id" :label="t.name || t.label" :value="t.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="属性">
          <div class="props-editor">
            <div class="props-editor-header">
              <span class="props-editor-title">属性列表</span>
              <el-button size="small" text @click="addNodeProp">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
            <div v-for="(_, idx) in nodePropKeys" :key="idx" class="prop-row">
              <el-input v-model="nodePropKeys[idx]" placeholder="属性名" size="small" />
              <el-input v-model="nodePropVals[idx]" placeholder="属性值" size="small" />
              <el-button type="danger" :icon="Delete" size="small" circle @click="removeNodeProp(idx)" />
            </div>
            <div v-if="nodePropKeys.length === 0" class="props-empty">
              <el-empty description="暂无属性" :image-size="40" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveNode">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑边对话框 -->
    <el-dialog v-model="edgeDialogVisible" :title="edgeDialogTitle" width="600px">
      <el-form :model="edgeForm" label-width="100px">
        <el-form-item label="标签" required>
          <el-select v-model="edgeForm.label" placeholder="选择边标签" style="width: 100%">
            <el-option v-for="t in edgeTypes" :key="t.id" :label="t.name || t.label" :value="t.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="起点UID" required>
          <el-input v-model="edgeForm.sourceUid" placeholder="输入起点UID" />
        </el-form-item>
        <el-form-item label="终点UID" required>
          <el-input v-model="edgeForm.targetUid" placeholder="输入终点UID" />
        </el-form-item>
        <el-form-item label="属性">
          <div class="props-editor">
            <div class="props-editor-header">
              <span class="props-editor-title">属性列表</span>
              <el-button size="small" text @click="addEdgeProp">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
            <div v-for="(_, idx) in edgePropKeys" :key="idx" class="prop-row">
              <el-input v-model="edgePropKeys[idx]" placeholder="属性名" size="small" />
              <el-input v-model="edgePropVals[idx]" placeholder="属性值" size="small" />
              <el-button type="danger" :icon="Delete" size="small" circle @click="removeEdgeProp(idx)" />
            </div>
            <div v-if="edgePropKeys.length === 0" class="props-empty">
              <el-empty description="暂无属性" :image-size="40" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdge">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入数据" width="700px">
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="模板下载">
          <el-button size="small" @click="downloadTemplate">
            <el-icon><Download /></el-icon> 下载CSV模板
          </el-button>
        </el-form-item>
        <el-form-item label="CSV文件" required>
          <div class="upload-area">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="onFileChange"
              accept=".csv"
              class="csv-upload"
            >
              <template #trigger>
                <div class="upload-trigger">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <span class="upload-text">{{ selectedFile ? selectedFile.name : '点击选择 CSV 文件' }}</span>
                  <span class="upload-hint" v-if="!selectedFile">支持 .csv 格式，首行为列标题</span>
                </div>
              </template>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="数据预览" v-if="csvPreview.length">
          <el-table :data="csvPreview" border stripe size="small" max-height="250" style="width: 100%">
            <el-table-column v-for="h in csvHeaders" :key="h" :prop="h" :label="h" min-width="100" show-overflow-tooltip />
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedFile" :loading="importing" @click="importData">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Download, Upload, Plus, Edit, Delete, Connection, Share, UploadFilled } from '@element-plus/icons-vue'
import { graphApi } from '@/views/graphs/api/graph'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'

const graphsStore = useGraphsStore()

// ---- 图信息 ----
const graphId = computed(() => graphsStore.currentGraphId)
const nodeTypes = ref([])
const edgeTypes = ref([])
const graphLoaded = ref(false)

// ---- 侧边栏选择 ----
const selectedType = ref('')   // 'node' | 'edge'
const selectedTypeId = ref('')

function selectNodeType(t) {
  selectedType.value = 'node'
  selectedTypeId.value = t.id
  searchText.value = ''
  pageNum.value = 1
  nextTick(() => loadData())
}

function selectEdgeType(t) {
  selectedType.value = 'edge'
  selectedTypeId.value = t.id
  searchText.value = ''
  pageNum.value = 1
  nextTick(() => loadData())
}

// ---- 数据表格 ----
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchText = ref('')

/** 原始数据（不含搜索过滤），用于导出 */
const rawData = ref([])

async function loadData() {
  if (!graphId.value || !selectedTypeId.value) return
  loading.value = true
  try {
    let res
    const params = { page: pageNum.value, size: pageSize.value }
    if (selectedType.value === 'node') {
      res = await graphApi.getNodeDataList(graphId.value, selectedTypeId.value, params)
    } else {
      res = await graphApi.getEdgeDataList(graphId.value, selectedTypeId.value, params)
    }
    const items = (() => {
      const d = res?.data
      if (Array.isArray(d)) return d
      if (d?.list) return d.list
      if (d?.records) return d.records
      if (Array.isArray(res)) return res
      return []
    })()
    rawData.value = items

    // 客户端搜索过滤
    if (searchText.value) {
      const kw = searchText.value.toLowerCase()
      tableData.value = items.filter(d =>
        (d.uid || '').toLowerCase().includes(kw) ||
        JSON.stringify(d.properties || {}).toLowerCase().includes(kw) ||
        (d.sourceUid || '').toLowerCase().includes(kw) ||
        (d.targetUid || '').toLowerCase().includes(kw)
      )
    } else {
      tableData.value = items
    }

    // total 优先取后端分页总数
    const rd = res?.data
    total.value = rd?.total ?? rd?.totalCount ?? items.length
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  pageNum.value = 1
  loadData()
}

// ---- 类型加载 ----
async function loadTypes() {
  if (!graphId.value) return
  graphLoaded.value = false
  try {
    const [nodes, edges] = await Promise.all([
      graphApi.getNodeDefs(graphId.value),
      graphApi.getEdgeDefs(graphId.value)
    ])
    nodeTypes.value = Array.isArray(nodes) ? nodes : (nodes?.data || [])
    edgeTypes.value = Array.isArray(edges) ? edges : (edges?.data || [])
    graphLoaded.value = true

    // 自动选中第一个类型，右侧直接显示数据
    if (!selectedTypeId.value) {
      if (nodeTypes.value.length > 0) {
        selectNodeType(nodeTypes.value[0])
      } else if (edgeTypes.value.length > 0) {
        selectEdgeType(edgeTypes.value[0])
      }
    }
  } catch (e) {
    ElMessage.error('加载类型定义失败')
  }
}

// ---- 新增/编辑 节点 ----
const nodeDialogVisible = ref(false)
const nodeDialogTitle = ref('')
const nodeForm = ref({ label: '', properties: {} })
const nodePropKeys = ref([])
const nodePropVals = ref([])

function showCreateDialog() {
  if (selectedType.value === 'node') {
    nodeForm.value = { label: '', properties: {} }
    nodePropKeys.value = []
    nodePropVals.value = []
    nodeDialogTitle.value = '新增节点'
    nodeDialogVisible.value = true
  } else {
    edgeForm.value = { label: '', sourceUid: '', targetUid: '', properties: {} }
    edgePropKeys.value = []
    edgePropVals.value = []
    edgeDialogTitle.value = '新增边'
    edgeDialogVisible.value = true
  }
}

function showEditDialog(row) {
  const props = row.properties || {}
  if (selectedType.value === 'node') {
    nodeForm.value = { uid: row.uid, label: row.label, properties: { ...props } }
    nodePropKeys.value = Object.keys(props)
    nodePropVals.value = Object.values(props)
    nodeDialogTitle.value = '编辑节点'
    nodeDialogVisible.value = true
  } else {
    edgeForm.value = { uid: row.uid, label: row.label, sourceUid: row.sourceUid, targetUid: row.targetUid, properties: { ...props } }
    edgePropKeys.value = Object.keys(props)
    edgePropVals.value = Object.values(props)
    edgeDialogTitle.value = '编辑边'
    edgeDialogVisible.value = true
  }
}

function buildProperties(keys, vals) {
  const p = {}
  keys.forEach((k, i) => { if (k) p[k] = vals[i] })
  return p
}

function addNodeProp() { nodePropKeys.value.push(''); nodePropVals.value.push('') }
function removeNodeProp(i) { nodePropKeys.value.splice(i, 1); nodePropVals.value.splice(i, 1) }

const saving = ref(false)

async function saveNode() {
  saving.value = true
  try {
    const data = {
      label: nodeForm.value.label,
      properties: buildProperties(nodePropKeys.value, nodePropVals.value)
    }
    if (nodeForm.value.uid) {
      await graphApi.updateNodeData(graphId.value, nodeForm.value.uid, data)
    } else {
      await graphApi.addNodeData(graphId.value, selectedTypeId.value, data)
    }
    ElMessage.success(nodeForm.value.uid ? '更新成功' : '新增成功')
    nodeDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 新增/编辑 边 ----
const edgeDialogVisible = ref(false)
const edgeDialogTitle = ref('')
const edgeForm = ref({ label: '', sourceUid: '', targetUid: '', properties: {} })
const edgePropKeys = ref([])
const edgePropVals = ref([])

function addEdgeProp() { edgePropKeys.value.push(''); edgePropVals.value.push('') }
function removeEdgeProp(i) { edgePropKeys.value.splice(i, 1); edgePropVals.value.splice(i, 1) }

async function saveEdge() {
  saving.value = true
  try {
    const data = {
      label: edgeForm.value.label,
      sourceUid: edgeForm.value.sourceUid,
      targetUid: edgeForm.value.targetUid,
      properties: buildProperties(edgePropKeys.value, edgePropVals.value)
    }
    if (edgeForm.value.uid) {
      await graphApi.updateEdgeData(graphId.value, edgeForm.value.uid, data)
    } else {
      await graphApi.addEdgeData(graphId.value, selectedTypeId.value, data)
    }
    ElMessage.success(edgeForm.value.uid ? '更新成功' : '新增成功')
    edgeDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该数据吗？', '提示', { type: 'warning' })
    if (selectedType.value === 'node') {
      await graphApi.deleteNode(graphId.value, row.uid, row.label)
    } else {
      await graphApi.deleteEdge(graphId.value, row.uid, row.label)
    }
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ---- 导出 ----
function exportData(format) {
  const items = rawData.value
  if (!items.length) {
    ElMessage.warning('没有数据可导出')
    return
  }
  if (format === 'csv') {
    exportCsv(items)
  } else {
    exportJson(items)
  }
}

function exportJson(items) {
  const typeLabel = selectedType.value === 'node' ? 'nodes' : 'edges'
  const data = items.map(d => ({
    uid: d.uid,
    label: d.label,
    ...(d.sourceUid ? { sourceUid: d.sourceUid } : {}),
    ...(d.targetUid ? { targetUid: d.targetUid } : {}),
    ...(d.properties || {})
  }))
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `${typeLabel}_${graphId.value}_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('JSON 导出成功')
}

function exportCsv(items) {
  const typeLabel = selectedType.value === 'node' ? 'nodes' : 'edges'
  const fields = baseFields.value
  const headerRow = fields.join(',')
  const dataRows = items.map(d => {
    return fields.map(f => {
      if (f === 'uid') return d.uid || ''
      if (f === 'label') return d.label || ''
      if (f === 'sourceUid') return d.sourceUid || ''
      if (f === 'targetUid') return d.targetUid || ''
      // 属性字段
      const val = d.properties?.[f]
      return val != null ? String(val) : ''
    }).join(',')
  }).join('\n')
  const csvContent = `${headerRow}\n${dataRows}`
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `${typeLabel}_${graphId.value}_${Date.now()}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('CSV 导出成功')
}

// ---- 导入 CSV ----
const importDialogVisible = ref(false)
const importForm = ref({})
const selectedFile = ref(null)
const uploadRef = ref(null)
const csvPreview = ref([])
const csvHeaders = ref([])
const importing = ref(false)

const baseFields = computed(() => {
  const cols = ['uid', 'label']
  if (selectedType.value === 'edge') {
    cols.push('sourceUid', 'targetUid')
  }
  // 从 schema 获取属性列
  const typeDefs = selectedType.value === 'node' ? nodeTypes.value : edgeTypes.value
  const currentType = typeDefs.find(t => t.id === selectedTypeId.value)
  const schemaProps = (currentType?.properties || [])
    .filter(p => !cols.includes(p.code))
    .map(p => p.code)
  return [...cols, ...schemaProps]
})

function showImportDialog() {
  selectedFile.value = null
  csvPreview.value = []
  csvHeaders.value = []
  importDialogVisible.value = true
  nextTick(() => uploadRef.value?.clearFiles())
}

function onFileChange(file) {
  selectedFile.value = file.raw
  previewCsv(file.raw)
}

function previewCsv(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const text = e.target.result
      const lines = text.split('\n').filter(l => l.trim())
      if (lines.length < 2) { ElMessage.warning('CSV文件内容不足'); return }
      const headers = parseCsvLine(lines[0])
      csvHeaders.value = headers
      const previewRows = []
      for (let i = 1; i < Math.min(lines.length, 11); i++) {
        const vals = parseCsvLine(lines[i])
        const row = {}
        headers.forEach((h, j) => { row[h] = vals[j] || '' })
        previewRows.push(row)
      }
      csvPreview.value = previewRows
    } catch (err) {
      ElMessage.error('CSV解析失败')
    }
  }
  reader.readAsText(file)
}

function parseCsvLine(line, delimiter = ',') {
  const result = []; let cur = ''; let inQ = false
  for (let i = 0; i < line.length; i++) {
    const c = line[i]
    if (c === '"') { inQ = !inQ }
    else if (c === delimiter && !inQ) { result.push(cur.trim()); cur = '' }
    else { cur += c }
  }
  result.push(cur.trim())
  return result
}

function downloadTemplate() {
  const typeLabel = selectedType.value === 'node' ? '节点' : '边'
  const fields = baseFields.value
  const headerRow = fields.join(',')
  const typeDefs = selectedType.value === 'node' ? nodeTypes.value : edgeTypes.value
  const currentType = typeDefs.find(t => t.id === selectedTypeId.value)
  const exampleRow = fields.map(f => {
    if (f === 'label') {
      return currentType?.label || ''
    }
    // 从 schema 属性中获取示例值
    const propDef = currentType?.properties?.find(p => p.code === f)
    if (propDef) {
      switch (propDef.type) {
        case 'Int': return '0'
        case 'Float': return '0.0'
        case 'Boolean': return 'true'
        case 'Date': return '2024-01-01'
        default: return `${f}_example`
      }
    }
    return `${f}_example`
  }).join(',')
  const csvContent = `${headerRow}\n${exampleRow}`
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `${typeLabel}_template.csv`
  a.click()
  URL.revokeObjectURL(url)
}

async function importData() {
  if (!selectedFile.value) { ElMessage.error('请选择文件'); return }
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('config', JSON.stringify({
      delimiter: ',',
      hasHeader: true
    }))
    if (selectedType.value === 'node') {
      await graphApi.importNodeData(graphId.value, selectedTypeId.value, formData)
    } else {
      await graphApi.importEdgeData(graphId.value, selectedTypeId.value, formData)
    }
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

// ---- 刷新 ----
function refreshAll() {
  loadTypes()
  if (selectedTypeId.value) loadData()
}

// ---- 监听图切换 ----
watch(() => graphId.value, (newId) => {
  if (newId) {
    selectedType.value = ''
    selectedTypeId.value = ''
    tableData.value = []
    loadTypes()
  }
}, { immediate: true })
</script>

<style scoped>
/* ===== 布局 ===== */
.graph-data {
  height: calc(100vh - 60px);
  background: var(--el-bg-color-page);
}

.graph-data :deep(.el-container) {
  height: 100%;
  gap: 0;
}

/* ===== 侧边栏 ===== */
.graph-data :deep(.el-aside) {
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

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
.graph-selector {
  width: 100%;
}

.sidebar-content {
  overflow-y: auto;
  flex: 1;
  padding: 0;
}

.section {
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.section:last-child {
  border-bottom: none;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.section-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 13px;
}

.section-icon.edge-icon {
  background: #f0f9f0;
  color: #67c23a;
}

.section-header h4 {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.section-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color);
  padding: 0 6px;
  border-radius: 8px;
  line-height: 18px;
  min-width: 18px;
  text-align: center;
}

/* 类型标签 — 标签芯片风格 */
.label-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.label-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color-lighter);
  font-size: 12px;
  min-width: 54px;
  text-align: center;
}

.label-tag .tag-text {
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.label-tag:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  transform: translateY(-1px);
}

.label-tag.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

/* 边标签 */
.edge-tag {
  border-color: #67c23a;
}

.edge-tag:hover {
  border-color: #67c23a;
  background: #f0f9f0;
  transform: translateY(-1px);
}

.edge-tag.active {
  border-color: #67c23a;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
}

/* ===== 主内容 ===== */
.graph-data :deep(.el-main) {
  padding: 0;
  background: var(--el-bg-color-page);
  overflow: auto;
}

.data-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 2px 20px 0;
}

/* 统一数据卡片 */
.data-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}

/* 数据操作栏 — 卡片头部 */
.data-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-left h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.data-count {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.data-count strong {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== 数据表格 ===== */
.table-wrapper {
  flex: 1;
  min-height: 0;
}

.graph-data :deep(.el-table) {
  font-size: 13px;
  border: none;
  border-radius: 0;
}

.graph-data :deep(.el-table th.el-table__cell) {
  background: var(--el-fill-color-light);
  font-weight: 600;
  color: var(--el-text-color-primary);
  padding: 8px 12px;
}

.graph-data :deep(.el-table .el-table__cell) {
  padding: 6px 12px;
}

.graph-data :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: var(--el-fill-color-lighter);
}

.graph-data :deep(.el-table::before) {
  display: none;
}

/* 属性标签 */
.props-display {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.prop-item {
  display: inline-flex;
  align-items: center;
  gap: 0;
  background: transparent;
  border-radius: 4px;
  font-size: 12px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  line-height: 22px;
}
.prop-key {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 500;
  padding: 0 6px;
  border-right: 1px solid var(--el-border-color-lighter);
}
.prop-val {
  color: var(--el-text-color-regular);
  padding: 0 6px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-prop {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

/* 表格操作按钮 */
.table-actions {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.table-actions .el-button {
  transition: all 0.2s ease;
}
.table-actions .el-button.el-button--primary {
  --el-button-bg-color: var(--el-color-primary-light-9);
  --el-button-border-color: transparent;
  --el-button-text-color: var(--el-color-primary);
}
.table-actions .el-button.el-button--primary:hover {
  --el-button-bg-color: var(--el-color-primary);
  --el-button-text-color: #fff;
}
.table-actions .el-button.el-button--danger {
  --el-button-bg-color: var(--el-color-danger-light-9);
  --el-button-border-color: transparent;
  --el-button-text-color: var(--el-color-danger);
}
.table-actions .el-button.el-button--danger:hover {
  --el-button-bg-color: var(--el-color-danger);
  --el-button-text-color: #fff;
}

/* ===== 分页 — 卡片底部 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
  flex-shrink: 0;
}

.data-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}

/* ===== 空状态 ===== */
.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0;
}

/* ===== 属性编辑器（对话框内） ===== */
.props-editor {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.props-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-bottom: 1px solid var(--el-border-color-light);
}

.props-editor-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.prop-row {
  display: grid;
  grid-template-columns: 1fr 1fr 32px;
  gap: 8px;
  align-items: center;
  padding: 6px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background 0.15s;
}
.prop-row:hover {
  background: var(--el-fill-color-lighter);
}
.prop-row:last-child {
  border-bottom: none;
}

.props-empty {
  padding: 12px 0;
}

/* ===== 上传区域 ===== */
.upload-area {
  width: 100%;
}

.csv-upload {
  width: 100%;
}

.csv-upload :deep(.el-upload) {
  width: 100%;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 28px 16px;
  border: 2px dashed var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--el-fill-color-lighter);
}
.upload-trigger:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.upload-icon {
  font-size: 28px;
  color: var(--el-text-color-placeholder);
  transition: color 0.2s;
}
.upload-trigger:hover .upload-icon {
  color: var(--el-color-primary);
}

.upload-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* ===== 对话框微调 ===== */
.graph-data :deep(.el-dialog) {
  border-radius: 10px;
  overflow: hidden;
}

.graph-data :deep(.el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
}

.graph-data :deep(.el-dialog__title) {
  font-size: 15px;
  font-weight: 600;
}

.graph-data :deep(.el-dialog__body) {
  padding: 20px;
}

.graph-data :deep(.el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid var(--el-border-color-light);
}

/* ===== 过渡动画 ===== */
.data-content {
  animation: contentFadeIn 0.2s ease;
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 滚动条美化 */
.sidebar-content::-webkit-scrollbar {
  width: 4px;
}
.sidebar-content::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 2px;
}
.sidebar-content::-webkit-scrollbar-track {
  background: transparent;
}
</style>
