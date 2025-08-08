<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">图连接管理</h2>
      <p class="page-description">管理图数据库连接配置，支持 Neo4j、Nebula Graph、JanusGraph</p>
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
            style="width: 150px; margin-left: 10px"
            clearable
            @change="handleSearch"
          >
            <el-option label="Neo4j" value="neo4j" />
            <el-option label="Nebula Graph" value="nebula" />
            <el-option label="JanusGraph" value="janus" />
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
        <el-table-column prop="name" label="连接名称" min-width="150">
          <template #default="{ row }">
            <div class="connection-name">
              <el-icon :color="getStatusColor(row.status)">
                <CircleCheck v-if="row.status === 'connected'" />
                <CircleClose v-else-if="row.status === 'disconnected'" />
                <Loading v-else />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="数据库类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="host" label="主机地址" min-width="150" />
        
        <el-table-column prop="port" label="端口" width="80" />
        
        <el-table-column prop="database" label="数据库名" min-width="120" />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              type="info"
              size="small"
              @click="handleTest(row)"
            >
              测试
            </el-button>
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入连接名称" />
        </el-form-item>
        
        <el-form-item label="数据库类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择数据库类型" style="width: 100%" @change="handleTypeChange">
            <el-option label="Neo4j" value="neo4j">
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span>Neo4j</span>
                <el-tag size="small" type="primary">最流行</el-tag>
              </div>
            </el-option>
            <el-option label="Nebula Graph" value="nebula">
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span>Nebula Graph</span>
                <el-tag size="small" type="success">分布式</el-tag>
              </div>
            </el-option>
            <el-option label="JanusGraph" value="janus">
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span>JanusGraph</span>
                <el-tag size="small" type="warning">多后端</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="form.host" placeholder="请输入主机地址" />
        </el-form-item>
        
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="数据库名" prop="database">
          <el-input v-model="form.database" placeholder="请输入数据库名" />
        </el-form-item>
        
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="连接池大小" prop="poolSize">
          <el-input-number v-model="form.poolSize" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="超时时间(秒)" prop="timeout">
          <el-input-number v-model="form.timeout" :min="1" :max="300" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入连接描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 测试结果对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="连接测试结果"
      width="500px"
    >
      <div v-if="testResult">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="响应时间">
            {{ testResult.responseTime }}ms
          </el-descriptions-item>
          <el-descriptions-item label="数据库版本">
            {{ testResult.version }}
          </el-descriptions-item>
          <el-descriptions-item label="节点数量">
            {{ testResult.nodes.toLocaleString() }}
          </el-descriptions-item>
          <el-descriptions-item label="边数量">
            {{ testResult.edges.toLocaleString() }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="Connection">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Plus, Refresh, CircleCheck, CircleClose, Loading
} from '@element-plus/icons-vue'
import connectionApi from '@/api/connection'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const searchType = ref('') // 添加数据库类型搜索条件
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const testDialogVisible = ref(false)
const testResult = ref(null)
const formRef = ref()

// 连接数据
const connections = ref([])

// 表单数据
const form = reactive({
  id: null,
  name: '',
  type: '',
  host: '',
  port: 7687,
  database: '',
  username: '',
  password: '',
  poolSize: 10,
  timeout: 30,
  description: ''
})

const defaultForm = {
  id: null,
  name: '',
  type: '',
  host: '',
  port: 7687,
  database: '',
  username: '',
  password: '',
  poolSize: 10,
  timeout: 30,
  description: ''
}

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入连接名称', trigger: 'blur' },
    { min: 2, max: 50, message: '连接名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择数据库类型', trigger: 'change' }
  ],
  host: [
    { required: true, message: '请输入主机地址', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '请输入端口号', trigger: 'blur' }
  ],
  database: [
    { required: true, message: '请输入数据库名', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 计算属性
const filteredConnections = computed(() => {
  let result = connections.value
  
  // 根据连接名称搜索
  if (searchKeyword.value) {
    result = result.filter(item =>
      item.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      item.host.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  // 根据数据库类型搜索
  if (searchType.value) {
    result = result.filter(item => item.type === searchType.value)
  }
  
  return result
})

const dialogTitle = computed(() => {
  return form.id ? '编辑连接' : '新增连接'
})

// 方法
const getStatusColor = (status) => {
  const colors = {
    1: '#67C23A', // connected
    0: '#F56C6C', // disconnected
    2: '#E6A23C'  // connecting
  }
  return colors[status] || '#909399'
}

const getStatusTagType = (status) => {
  const types = {
    1: 'success',  // connected
    0: 'danger',   // disconnected
    2: 'warning'   // connecting
  }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    1: '已连接',
    0: '未连接',
    2: '连接中'
  }
  return labels[status] || '未知'
}

const getTypeTagType = (type) => {
  const types = {
    neo4j: 'primary',
    nebula: 'success',
    janus: 'warning'
  }
  return types[type] || 'info'
}

const getTypeLabel = (type) => {
  const labels = {
    neo4j: 'Neo4j',
    nebula: 'Nebula Graph',
    janus: 'JanusGraph'
  }
  return labels[type] || type
}

// 获取连接列表
const fetchConnections = async () => {
  try {
    loading.value = true
    const response = await connectionApi.getConnections({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      type: searchType.value // 添加类型参数
    })
    connections.value = response.data.records || response.data.list || []
    total.value = response.data.total
  } catch (error) {
    console.error('获取连接列表失败:', error)
    ElMessage.error('获取连接列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchConnections()
}

const handleRefresh = () => {
  fetchConnections()
  ElMessage.success('刷新成功')
}

const handleAdd = () => {
  resetForm()
  // 确保form.id为null，表示新增模式
  form.id = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除连接 "${row.name}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await connectionApi.deleteConnection(row.id)
    ElMessage.success('删除成功')
    fetchConnections()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleConnect = async (row) => {
  try {
    loading.value = true
    row.status = 2 // connecting
    
    const response = await connectionApi.connectDatabase(row.id)
    row.status = response.data.status === 'connected' ? 1 : 0
    row.lastConnectTime = response.data.lastConnectTime
    ElMessage.success('连接成功')
    fetchConnections() // 刷新列表以确保状态同步
  } catch (error) {
    row.status = 0 // disconnected
    console.error('连接失败:', error)
    ElMessage.error('连接失败')
  } finally {
    loading.value = false
  }
}

const handleDisconnect = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要断开连接 "${row.name}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await connectionApi.disconnectDatabase(row.id)
    row.status = 0 // disconnected
    ElMessage.success('断开连接成功')
    fetchConnections() // 刷新列表以确保状态同步
  } catch (error) {
    if (error !== 'cancel') {
      console.error('断开连接失败:', error)
      ElMessage.error('断开连接失败')
    }
  }
}

const handleTest = async (row) => {
  try {
    const response = await connectionApi.testConnection(row.id)
    testResult.value = response.data
    testDialogVisible.value = true
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error('测试连接失败')
  }
}

const handleTypeChange = (type) => {
  if (type) {
    // 根据数据库类型设置默认端口和数据库名
    switch (type) {
      case 'neo4j':
        form.port = 7687
        form.database = 'neo4j'
        break
      case 'nebula':
        form.port = 9669
        form.database = 'nebula'
        break
      case 'janus':
        form.port = 8182
        form.database = 'janusgraph'
        break
      default:
        form.port = 7687
        form.database = ''
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    if (form.id) {
      // 编辑
      console.log('正在更新连接:', form)
      await connectionApi.updateConnection(form.id, form)
      ElMessage.success('更新成功')
    } else {
      // 新增
      console.log('正在创建连接:', form)
      await connectionApi.createConnection(form)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    fetchConnections()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败: ' + (error.message || '未知错误'))
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  resetForm()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    type: '',
    host: '',
    port: 7687,
    database: '',
    username: '',
    password: '',
    poolSize: 10,
    timeout: 30,
    description: ''
  })
  formRef.value?.clearValidate()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchConnections()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchConnections()
}

// 生命周期
onMounted(() => {
  fetchConnections()
})
</script>

<style scoped>
.connection-container {
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

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .search-area {
    justify-content: center;
  }
  
  .action-area {
    justify-content: center;
  }
}
</style>