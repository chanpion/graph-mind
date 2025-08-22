<template>
  <div class="graph-data-container">
    <div class="page-header">
      <h2 class="page-title">图数据管理</h2>
      <p class="page-description">管理图中的点边数据，支持增删改查操作</p>
    </div>

    <el-card class="data-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-select v-model="selectedEntityType" placeholder="请选择实体类型" @change="handleEntityTypeChange" 
                       :disabled="!selectedGraphId" style="width: 200px">
              <el-option label="点数据" value="node"></el-option>
              <el-option label="边数据" value="edge"></el-option>
            </el-select>
            
            <el-select v-model="selectedEntityTypeId" placeholder="请选择实体类型" @change="fetchEntityData"
                       :disabled="!selectedEntityType || entityTypes.length === 0">
              <el-option
                v-for="entityType in entityTypes"
                :key="entityType.id"
                :label="entityType.name"
                :value="entityType.id">
              </el-option>
            </el-select>
          </div>
          
          <div class="header-right">
            <el-button type="primary" @click="handleAdd" :disabled="!selectedEntityTypeId">新增</el-button>
            <el-button @click="handleRefresh">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 点数据表格 -->
      <div v-if="selectedEntityType === 'node' && selectedEntityTypeId">
        <el-table :data="entityData" style="width: 100%" v-loading="loading">
          <el-table-column prop="uid" label="唯一标识" width="180"></el-table-column>
          <el-table-column 
            v-for="prop in currentEntityTypeProperties" 
            :key="prop.code"
            :prop="prop.code"
            :label="prop.name"
            :formatter="propertyFormatter">
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" :icon="Edit" size="small" circle @click="handleEdit(row)" title="编辑" />
              <el-button type="danger" :icon="Delete" size="small" circle @click="handleDelete(row)" title="删除" />
            </template>
          </el-table-column>
        </el-table>
        
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          style="margin-top: 20px; text-align: right;">
        </el-pagination>
      </div>
      
      <!-- 边数据表格 -->
      <div v-else-if="selectedEntityType === 'edge' && selectedEntityTypeId">
        <el-table :data="entityData" style="width: 100%" v-loading="loading">
          <el-table-column prop="uid" label="唯一标识" width="180"></el-table-column>
          <el-table-column prop="startUid" label="起点ID" width="180"></el-table-column>
          <el-table-column prop="endUid" label="终点ID" width="180"></el-table-column>
          <el-table-column 
            v-for="prop in currentEntityTypeProperties" 
            :key="prop.code"
            :prop="prop.code"
            :label="prop.name"
            :formatter="propertyFormatter">
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" :icon="Edit" size="small" circle @click="handleEdit(row)" title="编辑" />
              <el-button type="danger" :icon="Delete" size="small" circle @click="handleDelete(row)" title="删除" />
            </template>
          </el-table-column>
        </el-table>
        
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          style="margin-top: 20px; text-align: right;">
        </el-pagination>
      </div>
      
      <div v-else class="empty-placeholder">
        <el-empty description="请选择实体类型以查看数据"></el-empty>
      </div>
    </el-card>
    
    <!-- 数据编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" label-width="100px" ref="formRef">
        <el-form-item label="唯一标识" prop="uid">
          <el-input v-model="formData.uid" :disabled="!!editMode"></el-input>
        </el-form-item>
        
        <!-- 起点和终点（仅边数据） -->
        <el-form-item label="起点ID" prop="startUid" v-if="selectedEntityType === 'edge'">
          <el-input v-model="formData.startUid"></el-input>
        </el-form-item>
        <el-form-item label="终点ID" prop="endUid" v-if="selectedEntityType === 'edge'">
          <el-input v-model="formData.endUid"></el-input>
        </el-form-item>
        
        <!-- 动态属性字段 -->
        <el-form-item 
          v-for="prop in currentEntityTypeProperties" 
          :key="prop.code"
          :label="prop.name"
          :prop="prop.code">
          <el-input v-model="formData[prop.code]"></el-input>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveData" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete } from '@element-plus/icons-vue'
import { graphApi } from '@/api/graph'

// 引入图store
import { useGraphStore } from '@/stores/graph'

const graphStore = useGraphStore()

// 图相关数据
const graphList = ref([])
const selectedGraphId = computed({
  get: () => graphStore.currentGraph?.id || '',
  set: (value) => {
    // 当图选择变化时，重置相关状态
    const graph = graphList.value.find(g => g.id === value)
    if (graph) {
      graphStore.setCurrentGraph(graph)
      selectedEntityType.value = '' // 重置实体类型
      selectedEntityTypeId.value = '' // 重置实体类型ID
      entityTypes.value = [] // 清空实体类型定义
      entityData.value = [] // 清空当前数据
    }
  }
})
const selectedEntityType = ref('') // 'node' 或 'edge'
const selectedEntityTypeId = ref('')
const entityTypes = ref([]) // 当前选中实体类型下的所有类型定义

// 数据表格相关
const entityData = ref([])
const loading = ref(false)
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 编辑对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formData = ref({})
const formRef = ref(null)
const editMode = ref(false) // 编辑模式标识
const submitLoading = ref(false) // 提交按钮加载状态

// 当前选中实体类型的属性列表
const currentEntityTypeProperties = computed(() => {
  if (!selectedEntityTypeId.value) return []
  
  const entityType = entityTypes.value.find(type => type.id === selectedEntityTypeId.value)
  return entityType?.properties?.filter(prop => prop.code !== 'uid') || []
})

// 属性值格式化
const propertyFormatter = (row, column, cellValue) => {
  if (cellValue === null || cellValue === undefined) {
    return ''
  }
  return String(cellValue)
}

// 获取图列表
const fetchGraphs = async () => {
  try {
    const res = await graphApi.getGraphs()
    graphList.value = res.data.records || res.data
  } catch (err) {
    ElMessage.error('获取图列表失败')
  }
}

// 图选择变化
const handleGraphChange = () => {
  selectedEntityType.value = ''
  selectedEntityTypeId.value = ''
  entityTypes.value = []
  entityData.value = []
}

// 实体类型选择变化
const handleEntityTypeChange = async () => {
  if (!selectedGraphId.value || !selectedEntityType.value) return
  
  selectedEntityTypeId.value = ''
  entityData.value = []
  
  try {
    if (selectedEntityType.value === 'node') {
      const res = await graphApi.getNodeDefs(selectedGraphId.value)
      entityTypes.value = res.data || []
    } else if (selectedEntityType.value === 'edge') {
      const res = await graphApi.getEdgeDefs(selectedGraphId.value)
      entityTypes.value = res.data || []
    }
  } catch (err) {
    ElMessage.error('获取实体类型失败')
  }
}

// 获取实体数据
const fetchEntityData = async () => {
  if (!selectedGraphId.value || !selectedEntityType.value || !selectedEntityTypeId.value) return
  
  loading.value = true
  try {
    let res
    const params = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize
    }
    
    if (selectedEntityType.value === 'node') {
      res = await graphApi.getNodeDataList(selectedGraphId.value, selectedEntityTypeId.value, params)
    } else {
      res = await graphApi.getEdgeDataList(selectedGraphId.value, selectedEntityTypeId.value, params)
    }
    
    entityData.value = res.data || []
    // TODO: 实际应从后端获取总数
    pagination.value.total = entityData.value.length
  } catch (err) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 新增数据
const handleAdd = () => {
  editMode.value = false
  dialogTitle.value = selectedEntityType.value === 'node' ? '新增点数据' : '新增边数据'
  formData.value = {
    uid: '',
    startUid: '', // 仅边数据使用
    endUid: ''    // 仅边数据使用
  }
  
  // 初始化属性字段
  currentEntityTypeProperties.value.forEach(prop => {
    formData.value[prop.code] = ''
  })
  
  dialogVisible.value = true
}

// 编辑数据
const handleEdit = (row) => {
  editMode.value = true
  dialogTitle.value = selectedEntityType.value === 'node' ? '编辑点数据' : '编辑边数据'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除数据
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该数据吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      if (selectedEntityType.value === 'node') {
        await graphApi.deleteNode(selectedGraphId.value, row.uid, getEntityLabel())
      } else {
        await graphApi.deleteEdge(selectedGraphId.value, row.uid, getEntityLabel())
      }
      ElMessage.success('删除成功')
      fetchEntityData()
    } catch (err) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 获取当前实体标签
const getEntityLabel = () => {
  const entityType = entityTypes.value.find(type => type.id === selectedEntityTypeId.value)
  return entityType ? entityType.label : ''
}

// 保存数据
const saveData = async () => {
  if (!formRef.value) return
  
  submitLoading.value = true
  try {
    let res
    if (!editMode.value) {
      // 新增数据
      if (selectedEntityType.value === 'node') {
        res = await graphApi.addNodeData(selectedGraphId.value, selectedEntityTypeId.value, formData.value)
      } else {
        res = await graphApi.addEdgeData(selectedGraphId.value, selectedEntityTypeId.value, formData.value)
      }
    } else {
      // 更新数据
      if (selectedEntityType.value === 'node') {
        res = await graphApi.updateNodeData(selectedGraphId.value, formData.value.uid, formData.value)
      } else {
        res = await graphApi.updateEdgeData(selectedGraphId.value, formData.value.uid, formData.value)
      }
    }
    
    if (res.code === 200) {
      ElMessage.success(editMode.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      fetchEntityData()
    } else {
      ElMessage.error(editMode.value ? '更新失败: ' + res.message : '新增失败: ' + res.message)
    }
  } catch (err) {
    ElMessage.error(editMode.value ? '更新失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

// 刷新数据
const handleRefresh = () => {
  fetchEntityData()
}

// 分页相关
const handleSizeChange = (val) => {
  pagination.value.pageSize = val
  fetchEntityData()
}

const handleCurrentChange = (val) => {
  pagination.value.currentPage = val
  fetchEntityData()
}

// 监听全局选中图的变化
watch(() => graphStore.currentGraph, (newGraph) => {
  if (newGraph) {
    // 更新选中的图ID
    // 清空之前选中的实体类型
    selectedEntityType.value = ''
    selectedEntityTypeId.value = ''
    entityData.value = []
  }
}, { immediate: true })

onMounted(() => {
  fetchGraphs()
})
</script>

<style scoped>
.graph-data-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.page-description {
  color: #666;
  font-size: 14px;
}

.data-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  gap: 15px;
}

.header-right {
  display: flex;
  gap: 10px;
}

.step-content {
  margin: 30px 0;
}

.step-actions {
  text-align: center;
  margin-top: 30px;
}

.empty-placeholder {
  padding: 40px 0;
  text-align: center;
}
</style>