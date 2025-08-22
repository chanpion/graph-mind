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

      <!-- 图列表卡片 -->
      <div v-loading="loading">
        <el-row :gutter="20">
          <el-col 
            v-for="graph in graphs" 
            :key="graph.id" 
            :xs="24" 
            :sm="12" 
            :md="8" 
            :lg="6" 
            :xl="4"
            class="graph-card-col"
          >
            <el-card class="graph-card" shadow="hover" @click="handleCardClick(graph)">
              <template #header>
                <div class="card-header">
                  <span class="graph-name">{{ graph.name }}</span>
                  <el-tag :type="getStatusTagType(graph.status)" size="small">
                    {{ getStatusLabel(graph.status) }}
                  </el-tag>
                </div>
              </template>
              
              <div class="card-content">
                <div class="graph-info">
                  <div class="info-item">
                    <span class="info-label">图编码:</span>
                    <span class="info-value">{{ graph.code }}</span>
                  </div>
                  
                  <div class="info-item">
                    <span class="info-label">关联连接:</span>
                    <span class="info-value">{{ getConnectionName(graph.connectionId) }}</span>
                  </div>
                  
                  <div class="info-item">
                    <span class="info-label">创建人:</span>
                    <span class="info-value">{{ graph.creator }}</span>
                  </div>
                  
                  <div class="info-item">
                    <span class="info-label">创建时间:</span>
                    <span class="info-value">{{ graph.createTime }}</span>
                  </div>
                  
                  <div class="info-item">
                    <span class="info-label">描述:</span>
<!--                    <span class="info-value description-text">{{ graph.description || '暂无描述' }}</span>-->
                    <span class="info-value">{{ graph.description}}</span>
                  </div>
                </div>
              </div>
              
              <div class="card-actions">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click.stop="handleEdit(graph)"
                >
                  编辑
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click.stop="handleDelete(graph)"
                >
                  删除
                </el-button>
              </div>
            </el-card>
          </el-col>
          
          <!-- 空状态 -->
          <el-col v-if="graphs.length === 0 && !loading" :span="24">
            <div class="empty-state">
              <el-empty description="暂无图数据" />
            </div>
          </el-col>
        </el-row>
      </div>

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

<!--        <el-form-item label="状态" prop="status">-->
<!--          <el-radio-group v-model="form.status">-->
<!--            <el-radio :value="1">启用</el-radio>-->
<!--            <el-radio :value="0">禁用</el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-form-item>-->
<!--        -->
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入图描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="GraphList">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Search, Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { graphApi } from '@/api/graph'
import { connectionApi } from '@/api/connection'

// 引入图store
import { useGraphStore } from '@/stores/graph'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const router = useRouter()
const graphStore = useGraphStore()

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
  connectionId: null,
  creator: ''
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
    1: 'success',  // 已发布
    0: 'info'      // 未发布
  }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    1: '已发布',
    0: '未发布'
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
    connectionId: null,
    creator: ''
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

// 点击卡片跳转到图设计页面
const handleCardClick = (graph) => {
  // 设置全局选中的图
  graphStore.setCurrentGraph(graph)
  router.push({ name: 'GraphDetail', params: { id: graph.id } })
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

.graph-card-col {
  margin-bottom: 20px;
}

.graph-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.3s ease;
}

.graph-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: #409eff;
}

.graph-card :deep(.el-card__header) {
  padding: 12px 15px;
  border-bottom: 1px solid #ebeef5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.graph-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 15px;
}

.card-content {
  flex: 1;
}

.graph-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  display: flex;
  font-size: 13px;
}

.info-label {
  color: #909399;
  width: 70px;
  flex-shrink: 0;
}

.info-value {
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.description-item {
  flex-direction: column;
  gap: 5px;
}

.description-text {
  white-space: normal;
  word-break: break-all;
  line-height: 1.4;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 15px;
  gap: 10px;
}

.card-actions .el-button {
  flex: 1;
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
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
  
  .graph-card-col {
    margin-bottom: 15px;
  }
}

/* 大屏幕适配 */
@media (min-width: 1200px) {
  .graph-card-col:nth-child(6n+1) {
    clear: both;
  }
}
</style>