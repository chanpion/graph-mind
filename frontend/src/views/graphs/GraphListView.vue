<template>
  <div class="graph-management">
    <div class="header">
      <h2 class="page-title">图管理</h2>
      <div class="header-actions">
        <el-select
          v-model="selectedConnectionId"
          placeholder="选择连接"
          style="width: 200px; margin-right: 10px;"
          clearable
          @change="onConnectionChange"
        >
          <el-option label="全部连接" value="">
            <span>全部连接</span>
            <el-tag type="info" size="small" style="margin-left: 8px;">全部</el-tag>
          </el-option>
          <el-option
            v-for="conn in connections"
            :key="conn.id"
            :label="conn.name"
            :value="conn.id"
          >
            <span>{{ conn.name }}</span>
            <el-tag
              :type="conn.status === 1 ? 'success' : 'danger'"
              size="small"
              style="margin-left: 8px;"
            >
              {{ conn.status === 1 ? '正常' : '异常' }}
            </el-tag>
          </el-option>
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索图名称"
          style="width: 180px; margin-right: 10px;"
          clearable
          @input="resetPagination"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="selectedSourceType"
          placeholder="图来源"
          style="width: 150px; margin-right: 10px;"
          clearable
          @change="resetPagination"
        >
          <el-option
            v-for="type in sourceTypes"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          >
            <el-tag :type="type.tagType" size="small">{{ type.label }}</el-tag>
          </el-option>
        </el-select>
        <el-button type="primary" @click="loadGraphs" :disabled="connections.length === 0 && !selectedConnectionId">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          新建图
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="graph-grid">
      <GraphCard
        v-for="graph in displayGraphs"
        :key="graph.id || graph.name"
        :graph="graph"
        @click="openGraph(graph)"
        @open="openGraph"
        @edit="handleEdit"
        @detail="showDetail"
        @browse="browseGraph"
        @delete="deleteGraph"
      />
    </div>

    <div v-if="loadingMore" class="loading-more">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="!hasMore && displayGraphs.length > 0" class="no-more">
      <span>没有更多了</span>
    </div>

    <el-empty
      v-if="!loading && !loadingMore && displayGraphs.length === 0"
      description="暂无图数据，请选择连接后点击刷新"
    />

    <!-- 新建图对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建图" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="连接" required>
          <el-select v-model="createForm.connectionId" placeholder="请选择连接" style="width: 100%;">
            <el-option
              v-for="conn in connections"
              :key="conn.id"
              :label="conn.name"
              :value="conn.id"
            >
              <span>{{ conn.name }}</span>
              <el-tag
                :type="conn.status === 1 ? 'success' : 'danger'"
                size="small"
                style="margin-left: 8px;"
              >
                {{ conn.status === 1 ? '正常' : '异常' }}
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="图标识" required>
          <el-input v-model="createForm.graphName" placeholder="请输入图标识（英文）" />
        </el-form-item>
        <el-form-item label="图名称">
          <el-input v-model="createForm.graphDisplayName" placeholder="请输入图名称（中文）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑图对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑图信息" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="关联连接">
          <el-input :model-value="getConnectionNameById(editForm.connectionId)" disabled />
        </el-form-item>
        <el-form-item label="图标识">
          <el-input :model-value="editForm.graphCode" disabled />
        </el-form-item>
        <el-form-item label="图名称">
          <el-input v-model="editForm.graphName" placeholder="请输入图名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入图描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateEdit" :loading="editing">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="图详情" width="600px">
      <div v-if="currentGraphDetail">
        <el-descriptions title="图信息" border :column="1">
          <el-descriptions-item label="图标识">{{ currentGraphDetail.code || currentGraphDetail.graphCode }}</el-descriptions-item>
          <el-descriptions-item label="图名称">{{ currentGraphDetail.name || currentGraphDetail.graphName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="连接名称">{{ getConnectionNameById(currentGraphDetail.connectionId) }}</el-descriptions-item>
          <el-descriptions-item label="数据库类型">{{ currentGraphDetail.graphType || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="节点数">{{ currentGraphDetail.vertexCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="边数">{{ currentGraphDetail.edgeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentGraphDetail.status === 'NORMAL' || currentGraphDetail.status === 0 ? 'success' : currentGraphDetail.status === 'ABNORMAL' || currentGraphDetail.status === 1 ? 'danger' : 'info'">
              {{ currentGraphDetail.status === 'NORMAL' || currentGraphDetail.status === 0 ? '正常' : currentGraphDetail.status === 'ABNORMAL' || currentGraphDetail.status === 1 ? '异常' : '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ currentGraphDetail.createTime ? new Date(currentGraphDetail.createTime).toLocaleString() : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else>
        <el-empty description="加载中..." />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus, Loading, Search } from '@element-plus/icons-vue'
import GraphCard from './components/GraphCard.vue'
import { connectionApi } from '@/views/connections/api/connection'
import { graphApi } from '@/views/graphs/api/graph'
import { useGraphsStore } from './stores/useGraphsStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const route = useRoute()
const graphsStore = useGraphsStore()
const { graphs, loading } = storeToRefs(graphsStore)

// ====== 本地状态 ======
const connections = ref([])
const selectedConnectionId = ref('')
const selectedSourceType = ref('')
const searchKeyword = ref('')
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const creating = ref(false)
const editing = ref(false)
const currentGraphDetail = ref(null)

// 分页状态
const currentPage = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

// 响应式分页大小
const pageSize = ref(getPageSize())

function getPageSize() {
  const width = window.innerWidth
  if (width >= 1400) return 8
  if (width >= 1024) return 6
  if (width >= 768) return 4
  return 2
}

const sourceTypes = [
  { label: '平台创建', value: 'PLATFORM', tagType: 'success' },
  { label: '图数据库已有', value: 'EXISTING', tagType: 'info' }
]

const createForm = reactive({
  connectionId: '',
  graphName: '',
  graphDisplayName: ''
})

const editForm = reactive({
  graphId: '',
  graphCode: '',
  graphName: '',
  description: '',
  originalGraphName: '',
  connectionId: ''
})

// 根据图来源和名称搜索筛选
const filteredGraphs = computed(() => {
  let list = graphs.value || []
  if (selectedSourceType.value) {
    list = list.filter(g => g.sourceType === selectedSourceType.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(g => (g.name || g.graphName || '').toLowerCase().includes(kw))
  }
  return list
})

// 分页显示
const displayGraphs = computed(() => {
  const end = currentPage.value * pageSize.value
  const filtered = filteredGraphs.value || []
  hasMore.value = end < filtered.length
  return filtered.slice(0, end)
})

// ====== 窗口大小变化重新计算分页 ======
let resizeHandler = null

// ====== 防抖滚动加载 ======
function debounce(fn, delay) {
  let timer = null
  return (...args) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

const handleScroll = debounce(() => {
  const el = document.querySelector('.app-content')
  if (!el) return
  const scrollTop = el.scrollTop
  const scrollHeight = el.scrollHeight
  const clientHeight = el.clientHeight
  if (!loadingMore.value && hasMore.value && scrollHeight - scrollTop - clientHeight < 100) {
    loadMore()
  }
}, 100)

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  setTimeout(() => {
    currentPage.value++
    loadingMore.value = false
  }, 300)
}

function resetPagination() {
  currentPage.value = 1
  hasMore.value = true
  loadingMore.value = false
}

// ====== 数据加载 ======
async function loadConnections() {
  try {
    const res = await connectionApi.list()
    const data = Array.isArray(res) ? res : (res?.data || [])
    connections.value = Array.isArray(data) ? data : data?.records || data?.list || []
  } catch (err) {
    console.error('加载连接列表失败:', err)
  }
}

async function loadGraphs() {
  resetPagination()
  if (selectedConnectionId.value) {
    await graphsStore.fetchGraphsByConnection(selectedConnectionId.value, { sourceType: selectedSourceType.value || undefined })
  } else {
    await graphsStore.fetchGraphs({ sourceType: selectedSourceType.value || undefined })
  }
}

function onConnectionChange() {
  graphsStore.setSelectedConnection(selectedConnectionId.value)
  loadGraphs()
}

// ====== 新建图 ======
function showCreateDialog() {
  createForm.connectionId = selectedConnectionId.value || ''
  createForm.graphName = ''
  createForm.graphDisplayName = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createForm.connectionId) { ElMessage.warning('请选择连接'); return }
  if (!createForm.graphName.trim()) { ElMessage.warning('请输入图标识'); return }
  if (!/^[a-zA-Z0-9_]+$/.test(createForm.graphName)) { ElMessage.warning('图标识只能包含字母、数字和下划线'); return }

  creating.value = true
  try {
    await graphsStore.createGraph({
      connectionId: Number(createForm.connectionId),
      code: createForm.graphName,
      name: createForm.graphDisplayName
    })
    ElMessage.success('图创建成功')
    createDialogVisible.value = false
    await loadGraphs()
  } catch (err) {
    ElMessage.error('创建图失败：' + (err.message || err))
  } finally {
    creating.value = false
  }
}

// ====== 编辑图 ======
function handleEdit(graph) {
  editForm.graphId = graph.id || ''
  editForm.graphCode = graph.code || graph.graphCode || ''
  editForm.graphName = graph.name || graph.graphName || ''
  editForm.description = graph.description || ''
  editForm.originalGraphName = graph.name || graph.graphName || ''
  editForm.connectionId = graph.connectionId || selectedConnectionId.value
  editDialogVisible.value = true
}

async function handleUpdateEdit() {
  const id = editForm.graphId
  if (!id) { ElMessage.error('无法确定图ID'); return }

  editing.value = true
  try {
    await graphsStore.updateGraph(id, {
      name: editForm.graphName,
      description: editForm.description
    })
    ElMessage.success('图信息更新成功')
    editDialogVisible.value = false
    await loadGraphs()
  } catch (err) {
    ElMessage.error('更新图失败')
  } finally {
    editing.value = false
  }
}

// ====== 详情 ======
async function showDetail(graph) {
  const conn = connections.value.find(c => c.id === (graph.connectionId || selectedConnectionId.value))
  const detail = {
    ...graph,
    connectionId: graph.connectionId || selectedConnectionId.value,
    graphType: graph.graphType || conn?.type || '未知',
    vertexCount: 0,
    edgeCount: 0,
    createTime: graph.createTime || graph.createdAt
  }
  // 从 summary API 获取真实的点边数量
  try {
    if (graph.id) {
      const graphCode = detail.code || detail.graphCode
      const params = {
        connectionId: detail.connectionId,
        graphCode: graphCode || undefined
      }
      const res = await graphApi.getGraphSummary(graph.id, params)
      if (res?.data) {
        detail.vertexCount = res.data.vertexCount ?? 0
        detail.edgeCount = res.data.edgeCount ?? 0
      }
    }
  } catch (e) {
    console.warn('获取图统计信息失败:', e)
  }
  currentGraphDetail.value = detail
  detailDialogVisible.value = true
}

function getConnectionNameById(connectionId) {
  const conn = connections.value.find(c => c.id === connectionId)
  return conn ? conn.name : '未知连接'
}

// ====== 删除 ======
async function deleteGraph(graph) {
  const name = graph.name || graph.graphName
  const id = graph.id
  if (!name && !id) {
    ElMessage.error('图信息无效')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除图 "${name || id}" 吗？`, '警告', { type: 'warning' })
    await graphsStore.deleteGraph(id, {
      connectionId: graph.connectionId,
      graphCode: graph.code || graph.graphCode
    })
    ElMessage.success('图删除成功')
    await loadGraphs()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除图失败:', err)
      ElMessage.error('删除图失败')
    }
  }
}

// ====== 导航 ======
function openGraph(graph) {
  const graphId = graph.id || graph.name
  const connId = graph.connectionId || selectedConnectionId.value
  graphsStore.setSelectedConnection(connId)
  graphsStore.setCurrentGraph(graph)
  router.push({
    name: 'DataModeling',
    query: { connectionId: connId, graphName: graph.name || graph.graphName }
  })
}

function browseGraph(graph) {
  const graphId = graph.id || graph.name
  const connId = graph.connectionId || selectedConnectionId.value
  graphsStore.setSelectedConnection(connId)
  graphsStore.setCurrentGraph(graph)
  router.push({ name: 'GraphData', params: { id: graphId } })
}

// ====== 生命周期 ======
async function init() {
  await loadConnections()
  const connIdFromRoute = route.query.connectionId
  if (connIdFromRoute) {
    selectedConnectionId.value = connIdFromRoute
    graphsStore.setSelectedConnection(connIdFromRoute)
  }
  await loadGraphs()
  await nextTick()
  const el = document.querySelector('.app-content')
  if (el) el.addEventListener('scroll', handleScroll)
  resizeHandler = () => { pageSize.value = getPageSize() }
  window.addEventListener('resize', resizeHandler)
}

onMounted(init)

onUnmounted(() => {
  const el = document.querySelector('.app-content')
  if (el) el.removeEventListener('scroll', handleScroll)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>
.graph-management { padding: 5px; }

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions { display: flex; align-items: center; }

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.graph-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.loading-more,
.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.no-more::before,
.no-more::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--el-border-color);
}
.no-more span { padding: 0 20px; }

@media (max-width: 1400px) {
  .graph-grid { grid-template-columns: repeat(3, 1fr); gap: 18px; }
}
@media (max-width: 1024px) {
  .graph-grid { grid-template-columns: repeat(2, 1fr); gap: 16px; }
}
@media (max-width: 768px) {
  .graph-grid { grid-template-columns: 1fr; gap: 16px; }
}
</style>
