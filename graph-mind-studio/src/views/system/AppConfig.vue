<template>
  <div class="connection-container">
    <div class="page-header">
      <h2 class="page-title">系统配置</h2>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input
              v-model="configKey"
              placeholder="请输入配置键"
              style="width: 200px"
              clearable
              @input="searchConfigs"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
        </div>

        <div class="action-area">
          <el-button type="primary" @click="showAddDialog">
            <el-icon>
              <Plus/>
            </el-icon>
            新增
          </el-button>
          <el-button @click="resetSearch">
            <el-icon>
              <Refresh/>
            </el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <el-table :data="configList" border style="width: 100%">
        <el-table-column prop="configKey" label="配置键" min-width="150" />
        <el-table-column prop="configValue" label="配置值" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button 
              type="primary" 
              circle 
              @click="showEditDialog(scope.row)"
              title="编辑"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button 
              type="danger" 
              circle 
              @click="deleteConfigItem(scope.row)"
              title="删除"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 配置编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="configFormRef"
        :model="currentConfig"
        :rules="configRules"
        label-width="80px"
      >
        <el-form-item label="配置键" prop="configKey">
          <el-input
            v-model="currentConfig.configKey"
            placeholder="请输入配置键"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input
            v-model="currentConfig.configValue"
            type="textarea"
            :rows="4"
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="currentConfig.description"
            type="textarea"
            :rows="2"
            placeholder="请输入配置描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveConfig">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, addConfig, updateConfig, deleteConfig } from '@/api/config.js'
import { Plus, Refresh, Search, Edit, Delete } from "@element-plus/icons-vue"
import { formatDateTime } from '@/utils/commonUtils.js'

const configKey = ref('')

// 搜索表单
const searchForm = reactive({
  configKey: ''
})

// 分页参数
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 配置列表
const configList = ref([])

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

// 当前编辑的配置
const currentConfig = reactive({
  id: null,
  configKey: '',
  configValue: '',
  description: ''
})

// 表单引用
const configFormRef = ref(null)

// 配置表单验证规则
const configRules = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' }
  ],
  configValue: [
    { required: true, message: '请输入配置值', trigger: 'blur' }
  ]
}

// 获取配置列表
const loadConfigList = async () => {
  try {
    const res = await getConfigList({
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      configKey: configKey.value || undefined
    })
    
    if (res.code === 200) {
      configList.value = res.data.records
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.message || '获取配置列表失败')
    }
  } catch (error) {
    console.error('获取配置列表失败:', error)
    ElMessage.error('获取配置列表失败')
  }
}

// 搜索配置
const searchConfigs = () => {
  pagination.currentPage = 1
  loadConfigList()
}

// 重置搜索
const resetSearch = () => {
  configKey.value = ''
  pagination.currentPage = 1
  loadConfigList()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1
  loadConfigList()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  pagination.currentPage = val
  loadConfigList()
}

// 显示新增对话框
const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '新增配置'
  
  // 重置表单
  Object.assign(currentConfig, {
    id: null,
    configKey: '',
    configValue: '',
    description: ''
  })
  
  dialogVisible.value = true
}

// 显示编辑对话框
const showEditDialog = (config) => {
  isEdit.value = true
  dialogTitle.value = '编辑配置'
  
  // 填充表单数据
  Object.assign(currentConfig, config)
  
  dialogVisible.value = true
}

// 保存配置
const saveConfig = async () => {
  try {
    await configFormRef.value.validate()
    
    let res
    if (isEdit.value) {
      // 更新配置
      res = await updateConfig(currentConfig.id, currentConfig)
    } else {
      // 新增配置
      res = await addConfig(currentConfig)
    }
    
    if (res.code === 200) {
      ElMessage.success(res.message || (isEdit.value ? '更新成功' : '新增成功'))
      dialogVisible.value = false
      loadConfigList()
    } else {
      ElMessage.error(res.message || (isEdit.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败')
  }
}

// 删除配置
const deleteConfigItem = (config) => {
  ElMessageBox.confirm(
    `确定要删除配置项 "${config.configKey}" 吗？此操作不可恢复！`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteConfig(config.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadConfigList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除配置失败:', error)
      ElMessage.error('删除配置失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 组件挂载时加载数据
onMounted(() => {
  loadConfigList()
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
  font-weight: bold;
  margin-bottom: 10px;
}

.page-description {
  color: #666;
  margin-bottom: 20px;
}

.content-card {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  text-align: right;
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