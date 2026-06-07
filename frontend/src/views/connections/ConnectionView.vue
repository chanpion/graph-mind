<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">连接管理</h2>
    </div>

    <div class="content-card">
      <!-- 搜索和操作栏 -->
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索连接名称或地址"
            style="width: 200px"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="searchType"
            placeholder="数据库类型"
            style="width: 150px"
            clearable
            @change="handleSearch"
          >
            <el-option label="Neo4j" value="NEO4J" />
            <el-option label="Nebula" value="NEBULA" />
            <el-option label="Janus" value="JANUS" />
          </el-select>
        </div>
        <div class="action-area">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增连接
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <!-- 连接列表 -->
      <el-table
        :data="filteredConnections"
        style="width: 100%"
        v-loading="loading"
        stripe
      >
        <el-table-column prop="name" label="连接名称" min-width="140">
          <template #default="{ row }">
            <div class="connection-name">
              <el-icon :color="getStatusColor(row.status)">
                <CircleCheck v-if="row.status === 1" />
                <CircleClose v-else-if="row.status === 0 || row.status === 2" />
                <Loading v-else />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="graphType" label="数据库类型" width="130">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.graphType)">
              {{ getTypeLabel(row.graphType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="hosts" label="主机地址" min-width="160" />

        <el-table-column prop="port" label="端口" width="80" />

        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              type="success"
              :icon="Connection"
              size="small"
              circle
              :loading="testingId === row.id"
              @click="handleTest(row)"
              title="测试"
            />
            <el-button
              type="primary"
              :icon="Edit"
              size="small"
              circle
              @click="handleEdit(row)"
              title="编辑"
            />
            <el-button
              type="danger"
              :icon="Delete"
              size="small"
              circle
              @click="handleDelete(row)"
              title="删除"
            />
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入连接名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="数据库类型" prop="graphType">
          <el-select v-model="form.graphType" placeholder="请选择数据库类型" style="width: 100%" @change="handleTypeChange">
            <el-option label="Neo4j" value="NEO4J" />
            <el-option label="Nebula" value="NEBULA" />
            <el-option label="Janus" value="JANUS" />
          </el-select>
        </el-form-item>

        <el-form-item label="主机地址" prop="hosts">
          <el-input v-model="form.hosts" placeholder="请输入主机地址，多个地址用逗号分隔" />
        </el-form-item>

        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <template v-if="form.graphType === 'JANUS'">
          <el-form-item label="存储后端" prop="storageBackend">
            <el-select v-model="form.storageBackend" placeholder="请选择存储后端">
              <el-option label="Cassandra (CQL)" value="cql" />
              <el-option label="HBase" value="hbase" />
            </el-select>
          </el-form-item>
          <el-form-item label="存储主机">
            <el-input v-model="form.storageHost" placeholder="请输入存储主机地址" />
          </el-form-item>
        </template>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入连接描述" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, CircleCheck, CircleClose, Loading, Edit, Delete, Connection } from '@element-plus/icons-vue'
import { connectionApi } from './api/connection'

const loading = ref(false)
const searchKeyword = ref('')
const searchType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const testingId = ref(null)
const formRef = ref()

const connections = ref([])

const defaultPorts = { NEO4J: 7687, NEBULA: 9669, JANUS: 8182 }

const form = reactive({
  id: null,
  name: '',
  type: 'NEO4J',
  host: '',
  port: 7687,
  username: '',
  password: '',
  description: '',
  storageBackend: 'cql',
  storageHost: ''
})

const rules = computed(() => {
  const isEdit = !!form.id
  return {
    name: [
      { required: true, message: '请输入连接名称', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    graphType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
    hosts: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
    port: [{ required: true, message: '请输入端口号', trigger: 'blur' }]
  }
})

const filteredConnections = computed(() => {
  return connections.value
})

const dialogTitle = computed(() => (form.id ? '编辑连接' : '新增连接'))

const getStatusColor = (status) => {
  const colors = { 0: '#909399', 1: '#67C23A', 2: '#F56C6C' }
  return colors[status] || '#909399'
}

const getStatusTagType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = { 0: '未检测', 1: '正常', 2: '失败' }
  return labels[status] || '未知'
}

const normalizeType = (type) => (type || '').toUpperCase()

const getTypeTagType = (type) => {
  const types = { NEO4J: 'primary', NEBULA: 'success', JANUS: 'warning' }
  return types[normalizeType(type)] || 'info'
}

const getTypeLabel = (type) => {
  const labels = { NEO4J: 'Neo4j', NEBULA: 'Nebula', JANUS: 'Janus' }
  return labels[normalizeType(type)] || type
}

async function fetchConnections() {
  loading.value = true
  try {
    const params = { page: currentPage.value, pageSize: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchType.value) params.graphType = searchType.value
    const res = await connectionApi.list(params)
    const data = Array.isArray(res) ? { records: res, total: res.length } : (res?.data || { records: [], total: 0 })
    connections.value = data.records || data.list || []
    total.value = data.total || connections.value.length
  } catch (error) {
    ElMessage.error('获取连接列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchConnections()
}

function handleRefresh() {
  fetchConnections()
  ElMessage.success('刷新成功')
}

function handleAdd() {
  resetForm()
  form.id = null
  dialogVisible.value = true
}

function handleEdit(row) {
  const editRow = JSON.parse(JSON.stringify(row))
  form.id = editRow.id
  form.name = editRow.name
  form.graphType = normalizeType(editRow.graphType) || 'NEO4J'
  form.hosts = editRow.hosts || ''
  form.port = editRow.port ?? (defaultPorts[normalizeType(editRow.graphType)] || 7687)
  form.description = editRow.description || ''

  form.username = editRow.username || ''

  // 兼容旧数据：如果实体字段为空，从 params 中获取
  let params = {}
  if (!editRow.username && !editRow.password) {
    if (editRow.params) {
      try {
        params = typeof editRow.params === 'string' ? JSON.parse(editRow.params) : editRow.params
      } catch (e) {
        console.error('解析 params 失败:', e)
      }
    }
    if (!form.username) form.username = params.username || ''
    form.password = params.password ? '••••••••' : ''
  } else {
    // 如果有密码，显示 •••••••• 表示有密码但不显示具体内容
    form.password = editRow.password ? '••••••••' : ''
  }

  form.storageBackend = params.storageBackend || 'cql'
  form.storageHost = params.storageHost || ''
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除连接 "${row.name}" 吗？`, '提示', { type: 'warning' })
    await connectionApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchConnections()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

async function handleTest(row) {
  testingId.value = row.id
  try {
    const res = await connectionApi.testConnection(row.id)
    const ok = res?.data === true
    if (ok) {
      ElMessage.success('连接测试通过')
      fetchConnections()
    } else {
      ElMessage.error('连接测试失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    testingId.value = null
  }
}

function handleTypeChange(type) {
  form.port = defaultPorts[type] || 7687
  if (type !== 'JANUS') {
    form.storageBackend = 'cql'
    form.storageHost = ''
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true
    // 将前端表单字段打包到 params JSON 字段中，匹配后端 GraphDatabaseConnection 模型
    const data = { ...form }
    const params = {}
    if (data.username) params.username = data.username
    // 如果密码是 ••••••••，说明用户没有修改密码，不传递密码
    if (data.password && data.password !== '••••••••') params.password = data.password
    if (data.storageBackend) params.storageBackend = data.storageBackend
    if (data.storageHost) params.storageHost = data.storageHost
    data.params = JSON.stringify(params)
    // 确保 type 存为大写
    data.graphType = normalizeType(data.graphType)
    // 清理不存在的字段，避免后端报错
    delete data.username
    delete data.password
    delete data.storageBackend
    delete data.storageHost
    if (form.id) {
      await connectionApi.update(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await connectionApi.create(data)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchConnections()
  } catch (error) {
    if (error?.message) ElMessage.error('提交失败: ' + error.message)
  } finally {
    submitLoading.value = false
  }
}

function handleDialogClose() {
  resetForm()
}

function resetForm() {
  form.id = null
  form.name = ''
  form.graphType = 'NEO4J'
  form.hosts = ''
  form.port = 7687
  form.username = ''
  form.password = ''
  form.description = ''
  form.storageBackend = 'cql'
  form.storageHost = ''
  formRef.value?.clearValidate()
}

function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
  fetchConnections()
}

function handleCurrentChange(val) {
  currentPage.value = val
  fetchConnections()
}

onMounted(fetchConnections)
</script>

<style scoped>
.connection-container {
  padding: 5px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.page-description {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.content-card {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.action-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.connection-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
