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
          <!-- 顶点类型 -->
          <div class="section">
            <div class="section-header">
              <span class="section-icon"><el-icon><Connection /></el-icon></span>
              <h4>顶点类型</h4>
              <span class="section-count">{{ vertexTypes.length }}</span>
            </div>
            <div class="label-tags" v-if="vertexTypes.length">
              <div
                v-for="t in vertexTypes"
                :key="t.id"
                class="label-tag"
                :class="{ active: selectedType === 'vertex' && selectedTypeId === t.id }"
                @click="selectVertexType(t)"
              >
                <span class="tag-text">{{ t.name || t.label }}</span>
              </div>
            </div>
            <el-empty v-else-if="graphLoaded" description="无顶点类型" :image-size="60" />
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
              <h3>{{ selectedType === 'vertex' ? '顶点数据' : '边数据' }}</h3>
              <el-tag :type="selectedType === 'vertex' ? 'primary' : 'success'" size="small" effect="plain">
                {{ selectedType === 'vertex' ? '顶点' : '边' }}
              </el-tag>
              <span class="data-count">共 <strong>{{ total }}</strong> 条</span>
            </div>
            <div class="header-actions">
              <el-input
                v-model="searchText"
                :placeholder="selectedType === 'vertex' ? '搜索顶点...' : '搜索边...'"
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
            <el-table-column v-if="selectedType === 'edge'" prop="startUid" label="起点" width="180" show-overflow-tooltip />
            <el-table-column v-if="selectedType === 'edge'" prop="endUid" label="终点" width="180" show-overflow-tooltip />
            <el-table-column label="属性" min-width="400">
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
          <el-empty description="请从左侧选择顶点或边类型" />
        </div>
      </el-main>
    </el-container>

    <!-- 创建/编辑顶点对话框 -->
    <el-dialog v-model="vertexDialogVisible" :title="vertexDialogTitle" width="600px">
      <el-form :model="vertexForm" label-width="90px">
        <el-form-item label="标签" required>
          <el-select v-model="vertexForm.label" placeholder="选择顶点标签" style="width: 100%" @change="onVertexLabelChange">
            <el-option v-for="t in vertexTypes" :key="t.id" :label="t.name || t.label" :value="t.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="UID" required>
          <el-input v-model="vertexForm.uid" placeholder="输入唯一标识符" :disabled="vertexDialogTitle === '编辑顶点'" />
        </el-form-item>
        <el-form-item label="属性" v-if="currentVertexDef && currentVertexDef.properties?.filter(p => p.code !== 'uid').length">
          <div class="props-form">
            <el-form-item
              v-for="prop in currentVertexDef.properties.filter(p => p.code !== 'uid')"
              :key="prop.id"
              :label="prop.name || prop.code"
              :prop="`props.${prop.code}`"
            >
              <!-- 根据数据类型显示不同的输入控件 -->
              <el-switch
                v-if="isBooleanType(prop)"
                v-model="vertexForm.props[prop.code]"
                active-text="是"
                inactive-text="否"
              />
              <el-input-number
                v-else-if="isNumberType(prop)"
                v-model="vertexForm.props[prop.code]"
                :controls="false"
                :placeholder="`输入${prop.name || prop.code}`"
              />
              <el-input-number
                v-else-if="isDoubleType(prop)"
                v-model="vertexForm.props[prop.code]"
                :controls="false"
                :step="0.1"
                :placeholder="`输入${prop.name || prop.code}`"
              />
              <el-date-picker
                v-else-if="isDateType(prop)"
                v-model="vertexForm.props[prop.code]"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
              />
              <el-date-picker
                v-else-if="isDateTimeType(prop)"
                v-model="vertexForm.props[prop.code]"
                type="datetime"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择日期时间"
              />
              <el-input
                v-else
                v-model="vertexForm.props[prop.code]"
                :placeholder="`输入${prop.name || prop.code}`"
                clearable
              />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="属性" v-else>
          <span style="color: var(--el-text-color-placeholder); font-size: 13px;">该顶点类型暂无自定义属性</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vertexDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVertex">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑边对话框 -->
    <el-dialog v-model="edgeDialogVisible" :title="edgeDialogTitle" width="600px">
      <el-form :model="edgeForm" label-width="100px">
        <el-form-item label="标签" required>
          <el-select v-model="edgeForm.label" placeholder="选择边标签" style="width: 100%" @change="onEdgeLabelChange">
            <el-option v-for="t in edgeTypes" :key="t.id" :label="t.name || t.label" :value="t.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="唯一标识符" required>
          <el-input v-model="edgeForm.uid" placeholder="输入唯一标识符" :disabled="edgeDialogTitle === '编辑边'" />
        </el-form-item>
        <el-form-item label="起点UID" required>
          <el-input v-model="edgeForm.startUid" placeholder="输入起点顶点UID" :disabled="edgeDialogTitle === '编辑边'" />
        </el-form-item>
        <el-form-item label="终点UID" required>
          <el-input v-model="edgeForm.endUid" placeholder="输入终点顶点UID" :disabled="edgeDialogTitle === '编辑边'" />
        </el-form-item>
        <el-form-item label="属性" v-if="currentEdgeDef && currentEdgeDef.properties?.filter(p => p.code !== 'uid').length">
          <div class="props-form">
            <el-form-item
              v-for="prop in currentEdgeDef.properties.filter(p => p.code !== 'uid')"
              :key="prop.id"
              :label="prop.name || prop.code"
            >
              <!-- 根据数据类型显示不同的输入控件 -->
              <el-switch
                v-if="isBooleanType(prop)"
                v-model="edgeForm.props[prop.code]"
                active-text="是"
                inactive-text="否"
              />
              <el-input-number
                v-else-if="isNumberType(prop)"
                v-model="edgeForm.props[prop.code]"
                :controls="false"
                :placeholder="`输入${prop.name || prop.code}`"
              />
              <el-input-number
                v-else-if="isDoubleType(prop)"
                v-model="edgeForm.props[prop.code]"
                :controls="false"
                :step="0.1"
                :placeholder="`输入${prop.name || prop.code}`"
              />
              <el-date-picker
                v-else-if="isDateType(prop)"
                v-model="edgeForm.props[prop.code]"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
              />
              <el-date-picker
                v-else-if="isDateTimeType(prop)"
                v-model="edgeForm.props[prop.code]"
                type="datetime"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择日期时间"
              />
              <el-input
                v-else
                v-model="edgeForm.props[prop.code]"
                :placeholder="`输入${prop.name || prop.code}`"
                clearable
              />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="属性" v-else>
          <span style="color: var(--el-text-color-placeholder); font-size: 13px;">该边类型暂无自定义属性</span>
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

// ---- 属性类型判断方法 ----
function isBooleanType(prop) {
  const type = (prop.dataType || prop.type)?.toLowerCase?.() || ''
  return type.includes('bool')
}

function isNumberType(prop) {
  const type = (prop.dataType || prop.type)?.toLowerCase?.() || ''
  return type.includes('int') || type.includes('long') || type.includes('short')
}

function isDoubleType(prop) {
  const type = (prop.dataType || prop.type)?.toLowerCase?.() || ''
  return type.includes('float') || type.includes('double') || type.includes('decimal')
}

function isDateType(prop) {
  const type = (prop.dataType || prop.type)?.toLowerCase?.() || ''
  return type === 'date'
}

function isDateTimeType(prop) {
  const type = (prop.dataType || prop.type)?.toLowerCase?.() || ''
  return type.includes('datetime') || type.includes('timestamp')
}

// ---- 图信息 ----
const graphId = computed(() => graphsStore.currentGraphId)
const vertexTypes = ref([])
const edgeTypes = ref([])
const graphLoaded = ref(false)

// ---- 侧边栏选择 ----
const selectedType = ref('')   // 'vertex' | 'edge'
const selectedTypeId = ref('')
// 保存当前选中类型的图上下文（discovered graph 需要 connectionId + graphCode）
const selectedTypeGraph = ref(null)

function selectVertexType(t) {
  selectedType.value = 'vertex'
  selectedTypeId.value = t.id
  // 从 store 的 currentGraph 获取图上下文，discovered graph 需要传 connectionId + graphCode
  selectedTypeGraph.value = graphsStore.currentGraph
  searchText.value = ''
  pageNum.value = 1
  nextTick(() => loadData())
}

function selectEdgeType(t) {
  selectedType.value = 'edge'
  selectedTypeId.value = t.id
  selectedTypeGraph.value = graphsStore.currentGraph
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
    // 对于发现的图（负ID类型），需要传入 connectionId + graphCode
    if (selectedTypeGraph.value && selectedTypeGraph.value.connectionId) {
      params.connectionId = selectedTypeGraph.value.connectionId
      params.graphCode = selectedTypeGraph.value.code
    }
    if (selectedType.value === 'vertex') {
      res = await graphApi.getVertexDataList(graphId.value, selectedTypeId.value, params)
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
        (d.startUid || '').toLowerCase().includes(kw) ||
        (d.endUid || '').toLowerCase().includes(kw)
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
    const params = {}
    if (graphId.value < 0 && graphsStore.currentGraph) {
      params.connectionId = graphsStore.currentGraph.connectionId
      params.graphCode = graphsStore.currentGraph.code
    }
    const [vertexDefsResponse, edges] = await Promise.all([
      graphApi.getVertexDefs(graphId.value, params),
      graphApi.getEdgeDefs(graphId.value, params)
    ])
    vertexTypes.value = Array.isArray(vertexDefsResponse) ? vertexDefsResponse : (vertexDefsResponse?.data || [])
    edgeTypes.value = Array.isArray(edges) ? edges : (edges?.data || [])
    graphLoaded.value = true

    // 自动选中第一个类型，右侧直接显示数据
    if (!selectedTypeId.value) {
      if (vertexTypes.value.length > 0) {
        selectVertexType(vertexTypes.value[0])
      } else if (edgeTypes.value.length > 0) {
        selectEdgeType(edgeTypes.value[0])
      }
    }
  } catch (e) {
    ElMessage.error('加载类型定义失败')
  }
}

// ---- 新增/编辑 顶点 ----
const vertexDialogVisible = ref(false)
const vertexDialogTitle = ref('')
const vertexForm = ref({ label: '', uid: '', props: {} })
const currentVertexDef = ref(null)

function showCreateDialog() {
  if (selectedType.value === 'vertex') {
    vertexForm.value = { label: '', uid: '', props: {} }
    currentVertexDef.value = null
    vertexDialogTitle.value = '新增顶点'
    vertexDialogVisible.value = true
  } else {
    edgeForm.value = { uid: '', label: '', startUid: '', endUid: '', props: {} }
    currentEdgeDef.value = null
    edgeDialogTitle.value = '新增边'
    edgeDialogVisible.value = true
  }
}

function onVertexLabelChange(label) {
  currentVertexDef.value = vertexTypes.value.find(t => t.label === label) || null
  vertexForm.value.props = {}
}

function onEdgeLabelChange(label) {
  currentEdgeDef.value = edgeTypes.value.find(t => t.label === label) || null
  edgeForm.value.props = {}
}

function showEditDialog(row) {
  const props = row.properties || {}
  // 过滤掉 null、undefined 值和 uid（uid是唯一标识，不应作为属性）
  const filteredProps = {}
  Object.entries(props).forEach(([k, v]) => {
    if (v != null && v !== '' && k !== 'uid') filteredProps[k] = v
  })
  if (selectedType.value === 'vertex') {
    const def = vertexTypes.value.find(t => t.label === row.label)
    currentVertexDef.value = def || null
    vertexForm.value = {
      uid: row.uid,
      label: row.label,
      props: filteredProps
    }
    vertexDialogTitle.value = '编辑顶点'
    vertexDialogVisible.value = true
  } else {
    const def = edgeTypes.value.find(t => t.label === row.label)
    currentEdgeDef.value = def || null
    edgeForm.value = {
      uid: row.uid,
      label: row.label,
      startUid: row.startUid,
      endUid: row.endUid,
      props: filteredProps
    }
    edgeDialogTitle.value = '编辑边'
    edgeDialogVisible.value = true
  }
}

const saving = ref(false)

/** 对于发现的图（graphId < 0），写操作需额外传 connectionId + graphCode */
function getExtraParams() {
  const extra = {}
  if (graphId.value < 0 && graphsStore.currentGraph) {
    extra.connectionId = graphsStore.currentGraph.connectionId
    extra.graphCode = graphsStore.currentGraph.code
  }
  return extra
}

async function saveVertex() {
  if (!vertexForm.value.label) { ElMessage.warning('请选择标签'); return }
  if (!vertexForm.value.uid) { ElMessage.warning('请输入UID'); return }
  saving.value = true
  try {
    const properties = {}
    Object.entries(vertexForm.value.props).forEach(([k, v]) => {
      if (v !== '' && v != null) properties[k] = v
    })
    const data = { uid: vertexForm.value.uid, label: vertexForm.value.label, properties }
    const extra = getExtraParams()
    if (vertexDialogTitle.value === '编辑顶点') {
      await graphApi.updateVertexData(graphId.value, vertexForm.value.uid, data, extra)
    } else {
      await graphApi.addVertexData(graphId.value, selectedTypeId.value, data, extra)
    }
    ElMessage.success(vertexDialogTitle.value === '编辑顶点' ? '更新成功' : '新增成功')
    vertexDialogVisible.value = false
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
const edgeForm = ref({ uid: '', label: '', startUid: '', endUid: '', props: {} })
const currentEdgeDef = ref(null)

async function saveEdge() {
  if (!edgeForm.value.label) { ElMessage.warning('请选择标签'); return }
  if (!edgeForm.value.uid) { ElMessage.warning('请输入唯一标识符'); return }
  if (!edgeForm.value.startUid) { ElMessage.warning('请输入起点UID'); return }
  if (!edgeForm.value.endUid) { ElMessage.warning('请输入终点UID'); return }
  saving.value = true
  try {
    const properties = {}
    Object.entries(edgeForm.value.props).forEach(([k, v]) => {
      if (v !== '' && v != null) properties[k] = v
    })
    const data = {
      uid: edgeForm.value.uid,
      label: edgeForm.value.label,
      startUid: edgeForm.value.startUid,
      endUid: edgeForm.value.endUid,
      properties
    }
    const extra = getExtraParams()
    if (edgeDialogTitle.value === '编辑边') {
      await graphApi.updateEdgeData(graphId.value, edgeForm.value.uid, data, extra)
    } else {
      await graphApi.addEdgeData(graphId.value, selectedTypeId.value, data, extra)
    }
    ElMessage.success(edgeDialogTitle.value === '编辑边' ? '更新成功' : '新增成功')
    edgeDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该数据吗？', '提示', { type: 'warning' })
    const extra = getExtraParams()
    if (selectedType.value === 'vertex') {
      await graphApi.deleteVertexData(graphId.value, row.uid, row.label, extra)
    } else {
      await graphApi.deleteEdge(graphId.value, row.uid, row.label, extra)
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
  const typeLabel = selectedType.value === 'vertex' ? 'vertices' : 'edges'
  const data = items.map(d => ({
    uid: d.uid,
    label: d.label,
    ...(d.startUid ? { startUid: d.startUid } : {}),
    ...(d.endUid ? { endUid: d.endUid } : {}),
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
  const typeLabel = selectedType.value === 'vertex' ? 'vertices' : 'edges'
  const fields = baseFields.value
  const headerRow = fields.join(',')
  const dataRows = items.map(d => {
    return fields.map(f => {
      if (f === 'uid') return d.uid || ''
      if (f === 'label') return d.label || ''
      if (f === 'startUid') return d.startUid || ''
      if (f === 'endUid') return d.endUid || ''
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
    cols.push('startUid', 'endUid')
  }
  // 从 schema 获取属性列
  const typeDefs = selectedType.value === 'vertex' ? vertexTypes.value : edgeTypes.value
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
      const buffer = e.target.result
      // 先尝试 UTF-8 解码，如果出现乱码（替换字符）则改用 GBK
      let text = new TextDecoder('utf-8', { fatal: false }).decode(buffer)
      if (text.includes('\uFFFD')) {
        text = new TextDecoder('gbk', { fatal: false }).decode(buffer)
      }
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
  reader.readAsArrayBuffer(file)
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
  const graphCode = graphsStore.currentGraph?.code || graphId.value
  const typePrefix = selectedType.value === 'vertex' ? 'vertex' : 'edge'
  const typeDefs = selectedType.value === 'vertex' ? vertexTypes.value : edgeTypes.value
  const currentType = typeDefs.find(t => t.id === selectedTypeId.value)
  const typeLabel = currentType?.label || 'unknown'
  const fields = baseFields.value
  const headerRow = fields.join(',')
  const exampleRow = fields.map(f => {
    if (f === 'label') {
      return currentType?.label || ''
    }
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
  a.href = url; a.download = `data_${graphCode}_${typePrefix}_${typeLabel}_template.csv`
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
    
    // 对于发现的图（graphId < 0），使用 connectionId + graphCode + label
    const opts = {}
    if (graphId.value < 0 && graphsStore.currentGraph) {
      opts.connectionId = graphsStore.currentGraph.connectionId
      opts.graphCode = graphsStore.currentGraph.code
      // 获取当前选中类型的 label
      if (selectedType.value === 'vertex') {
        const vt = vertexTypes.value.find(t => t.id === selectedTypeId.value)
        if (vt) opts.label = vt.label
      } else {
        const et = edgeTypes.value.find(t => t.id === selectedTypeId.value)
        if (et) opts.label = et.label
      }
    }
    
    let res
    if (selectedType.value === 'vertex') {
      res = await graphApi.importVertexData(graphId.value, selectedTypeId.value, formData, opts)
    } else {
      res = await graphApi.importEdgeData(graphId.value, selectedTypeId.value, formData, opts)
    }
    const result = res?.data
    if (result) {
      const msg = `导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条`
      if (result.failureCount > 0 && result.successCount === 0) {
        ElMessage.error(msg)
      } else if (result.failureCount > 0) {
        ElMessage.warning(msg)
      } else {
        ElMessage.success(msg)
      }
    } else {
      ElMessage.success('导入成功')
    }
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
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
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
  border-color: var(--el-color-success);
}

.edge-tag:hover {
  border-color: var(--el-color-success);
  background: var(--el-color-success-light-9);
  transform: translateY(-1px);
}

.edge-tag.active {
  border-color: var(--el-color-success);
  background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-light-3));
  color: #fff;
}

/* ===== 主内容 ===== */
.graph-data :deep(.el-main) {
  padding: 0;
  background: var(--el-bg-color-page);
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.data-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0 5px 5px 5px;
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

/* ===== 空状态 ===== */
.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 0;
}

/* ===== 属性表单（对话框内，根据 schema 动态生成） ===== */
.props-form .el-form-item {
  margin-bottom: 12px;
}
.props-form .el-form-item__label {
  justify-content: flex-start;
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
