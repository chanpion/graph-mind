<template>
  <div class="graph-list-container">
    <div class="page-header">
      <h2 class="page-title">图管理</h2>
      <p class="page-description">管理图实例，查看图的基本信息和关联的数据库连接</p>
    </div>

    <div class="content-card">
      <!-- 搜索和操作栏 -->
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索图名称或编码"
            style="width: 300px"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="action-area">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增图
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <!-- 图列表 -->
      <el-table
        :data="graphs"
        style="width: 100%"
        v-loading="loading"
        stripe
      >
        <el-table-column prop="name" label="图名称" min-width="150" />
        
        <el-table-column prop="code" label="图编码" min-width="120" />
        
        <el-table-column prop="description" label="描述" min-width="200" />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="connectionId" label="关联连接" min-width="120">
          <template #default="{ row }">
            <span>{{ getConnectionName(row.connectionId) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="180" />
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
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
        <el-form-item label="图名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入图名称" />
        </el-form-item>
        
        <el-form-item label="图编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入图编码" />
        </el-form-item>
        
        <el-form-item label="关联连接" prop="connectionId">
          <el-select v-model="form.connectionId" placeholder="请选择关联的数据库连接" style="width: 100%">
            <el-option
              v-for="connection in connections"
              :key="connection.id"
              :label="connection.name"
              :value="connection.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入图描述"
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
  </div>
</template>

<script setup name="GraphList">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Plus, Refresh
} from '@element-plus/icons-vue'
import graphApi from '@/api/graph'
import connectionApi from '@/api/connection'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()

// 图数据
const graphs = ref([])
const connections = ref([])

// 表单数据
const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: 1,
  connectionId: null
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入图名称', trigger: 'blur' },
    { min: 2, max: 50, message: '图名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入图编码', trigger: 'blur' },
    { min: 2, max: 100, message: '图编码长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  connectionId: [
    { required: true, message: '请选择关联的数据库连接', trigger: 'change' }
  ]
}

// 计算属性
const dialogTitle = computed(() => {
  return form.id ? '编辑图' : '新增图'
})

// 方法
const getStatusTagType = (status) => {
  const types = {
    1: 'success',  // 启用
    0: 'danger'    // 禁用
  }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    1: '启用',
    0: '禁用'
  }
  return labels[status] || '未知'
}

const getConnectionName = (connectionId) => {
  const connection = connections.value.find(conn => conn.id === connectionId)
  return connection ? connection.name : '未知连接'
}

// 获取图列表
const fetchGraphs = async () => {
  try {
    loading.value = true
    const response = await graphApi.getGraphs({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value
    })
    graphs.value = response.data.records || response.data.list || []
    total.value = response.data.total
  } catch (error) {
    console.error('获取图列表失败:', error)
    ElMessage.error('获取图列表失败')
  } finally {
    loading.value = false
  }
}

// 获取连接列表
const fetchConnections = async () => {
  try {
    const response = await connectionApi.getConnections({
      page: 1,
      pageSize: 100  // 获取所有连接
    })
    connections.value = response.data.records || response.data.list || []
  } catch (error) {
    console.error('获取连接列表失败:', error)
    ElMessage.error('获取连接列表失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchGraphs()
}

const handleRefresh = () => {
  fetchGraphs()
  ElMessage.success('刷新成功')
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除图 "${row.name}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await graphApi.deleteGraph(row.id)
    ElMessage.success('删除成功')
    fetchGraphs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    if (form.id) {
      // 编辑
      await graphApi.updateGraph(form.id, form)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await graphApi.createGraph(form)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    fetchGraphs()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
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
    code: '',
    description: '',
    status: 1,
    connectionId: null
  })
  formRef.value?.clearValidate()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchGraphs()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchGraphs()
}

// 生命周期
onMounted(() => {
  fetchGraphs()
  fetchConnections()
})
</script>

<style scoped>
.graph-list-container {
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