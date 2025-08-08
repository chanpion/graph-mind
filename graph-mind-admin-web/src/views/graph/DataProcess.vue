<template>
  <div class="data-process-container">
    <div class="page-header">
      <h2 class="page-title">图数据加工</h2>
      <p class="page-description">支持从CSV文件导入点边数据，进行数据预处理和图构建</p>
    </div>

    <el-card class="process-card">
      <template #header>
        <div class="card-header">
          <span>数据导入</span>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" simple>
        <el-step title="选择图" />
        <el-step title="上传文件" />
        <el-step title="数据映射" />
        <el-step title="数据预览" />
        <el-step title="执行导入" />
      </el-steps>

      <!-- 步骤1: 选择图 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-form :model="form" label-width="100px" style="max-width: 500px; margin: 20px auto;">
          <el-form-item label="选择图">
            <el-select v-model="form.graphId" placeholder="请选择图" style="width: 100%" @change="handleGraphChange">
              <el-option
                v-for="graph in graphList"
                :key="graph.id"
                :label="graph.name"
                :value="graph.id">
              </el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="数据类型">
            <el-radio-group v-model="form.dataType">
              <el-radio value="node">点数据</el-radio>
              <el-radio value="edge">边数据</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        
        <div class="step-actions">
          <el-button type="primary" @click="nextStep" :disabled="!form.graphId">下一步</el-button>
        </div>
      </div>

      <!-- 步骤2: 上传文件 -->
      <div v-show="currentStep === 1" class="step-content">
        <div class="upload-area">
          <el-upload
            class="upload-demo"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :show-file-list="true"
            accept=".csv"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将CSV文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                请上传CSV格式文件，大小不超过10MB
              </div>
            </template>
          </el-upload>
        </div>
        
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!csvFile">下一步</el-button>
        </div>
      </div>

      <!-- 步骤3: 数据映射 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="mapping-header">
          <h3>数据映射配置</h3>
          <p>将CSV列映射到图数据库中的属性</p>
        </div>
        
        <el-table :data="csvHeaders" style="width: 100%" border>
          <el-table-column prop="column" label="CSV列" width="180" />
          <el-table-column label="映射到">
            <template #default="{ row }">
              <el-select 
                v-model="row.mapping" 
                placeholder="请选择映射字段" 
                style="width: 100%"
                @change="handleMappingChange(row)"
              >
                <el-option 
                  v-for="field in mappingFields" 
                  :key="field.value" 
                  :label="field.label" 
                  :value="field.value" 
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="sample" label="示例数据" />
        </el-table>
        
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!isMappingValid">下一步</el-button>
        </div>
      </div>

      <!-- 步骤4: 数据预览 -->
      <div v-show="currentStep === 3" class="step-content">
        <div class="preview-header">
          <h3>数据预览</h3>
          <p>预览即将导入的数据</p>
        </div>
        
        <el-table :data="previewData" style="width: 100%" border max-height="400">
          <el-table-column 
            v-for="col in csvHeaders" 
            :key="col.column" 
            :prop="col.column" 
            :label="col.column" 
          />
        </el-table>
        
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep">下一步</el-button>
        </div>
      </div>

      <!-- 步骤5: 执行导入 -->
      <div v-show="currentStep === 4" class="step-content">
        <div class="import-summary">
          <h3>导入配置确认</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="目标图">{{ selectedGraph.name }}</el-descriptions-item>
            <el-descriptions-item label="数据类型">{{ form.dataType === 'node' ? '点数据' : '边数据' }}</el-descriptions-item>
<!--            <el-descriptions-item label="文件名">{{ csvFile.name }}</el-descriptions-item>-->
            <el-descriptions-item label="文件名"></el-descriptions-item>
            <el-descriptions-item label="数据行数">{{ previewData.length }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="import-progress" v-if="importStatus !== 'idle'">
          <el-progress 
            :percentage="importProgress" 
            :status="importStatus === 'success' ? 'success' : importStatus === 'error' ? 'exception' : undefined"
          />
          <p v-if="importStatus === 'importing'">正在导入数据，请稍候...</p>
          <p v-if="importStatus === 'success'" class="success-text">数据导入成功！</p>
          <p v-if="importStatus === 'error'" class="error-text">数据导入失败，请重试</p>
        </div>
        
        <div class="step-actions">
          <el-button @click="prevStep" :disabled="importStatus === 'importing'">上一步</el-button>
          <el-button 
            type="primary" 
            @click="executeImport" 
            :loading="importStatus === 'importing'"
            :disabled="importStatus === 'success'"
          >
            {{ importStatus === 'success' ? '已完成' : '执行导入' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="DataProcess">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import graphApi from '@/api/graph'
import Papa from 'papaparse'

// 步骤控制
const currentStep = ref(0)
const importProgress = ref(0)
const importStatus = ref('idle') // idle, importing, success, error

// 表单数据
const form = ref({
  graphId: '',
  dataType: 'node' // node or edge
})

// 图列表
const graphList = ref([])
const selectedGraph = ref({})

// CSV文件相关
const csvFile = ref(null)
const csvData = ref([])
const csvHeaders = ref([])

// 预览数据
const previewData = ref([])

// 映射字段
const nodeMappingFields = ref([
  { value: 'id', label: 'ID (唯一标识)' },
  { value: 'label', label: '标签(类型)' },
  { value: 'properties', label: '属性(可多个)' }
])

const edgeMappingFields = ref([
  { value: 'id', label: 'ID (唯一标识)' },
  { value: 'source', label: '起点ID' },
  { value: 'target', label: '终点ID' },
  { value: 'label', label: '标签(类型)' },
  { value: 'properties', label: '属性(可多个)' }
])

// 计算属性
const mappingFields = computed(() => {
  return form.value.dataType === 'node' ? nodeMappingFields.value : edgeMappingFields.value
})

const isMappingValid = computed(() => {
  // 检查必要字段是否已映射
  const requiredFields = form.value.dataType === 'node' 
    ? ['id'] 
    : ['source', 'target']
  
  return requiredFields.every(field => 
    csvHeaders.value.some(header => header.mapping === field)
  )
})

// 获取图列表
const fetchGraphList = async () => {
  try {
    const res = await graphApi.getGraphList()
    graphList.value = res.data.list
  } catch (e) {
    ElMessage.error('获取图列表失败')
  }
}

// 处理图选择变化
const handleGraphChange = (graphId) => {
  selectedGraph.value = graphList.value.find(g => g.id === graphId) || {}
}

// 上一步
const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 下一步
const nextStep = () => {
  // 数据验证
  if (currentStep.value === 0 && !form.value.graphId) {
    ElMessage.warning('请选择目标图')
    return
  }
  
  if (currentStep.value === 1 && !csvFile.value) {
    ElMessage.warning('请上传CSV文件')
    return
  }
  
  if (currentStep.value < 4) {
    currentStep.value++
    
    // 特殊处理：在进入数据预览步骤时生成预览数据
    if (currentStep.value === 3) {
      generatePreviewData()
    }
  }
}

// 处理文件上传
const handleFileChange = (file) => {
  if (file.raw.type !== 'text/csv' && !file.raw.name.endsWith('.csv')) {
    ElMessage.error('请上传CSV格式文件')
    return
  }
  
  if (file.raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return
  }
  
  csvFile.value = file.raw
  parseCSV(file.raw)
}

// 解析CSV文件
const parseCSV = (file) => {
  Papa.parse(file, {
    header: true,
    skipEmptyLines: true,
    complete: (results) => {
      csvData.value = results.data
      // 初始化表头映射
      if (results.meta.fields && results.meta.fields.length > 0) {
        csvHeaders.value = results.meta.fields.map(field => ({
          column: field,
          mapping: '',
          sample: results.data.length > 0 ? results.data[0][field] : ''
        }))
      }
    },
    error: (error) => {
      ElMessage.error('CSV文件解析失败: ' + error.message)
    }
  })
}

// 处理映射变化
const handleMappingChange = (row) => {
  // 如果选择的是属性字段，允许多个列映射到属性
  // 其他字段只能单列映射
  if (row.mapping !== 'properties') {
    csvHeaders.value.forEach(header => {
      if (header !== row && header.mapping === row.mapping) {
        header.mapping = ''
      }
    })
  }
}

// 生成预览数据
const generatePreviewData = () => {
  // 只显示前10行数据作为预览
  previewData.value = csvData.value.slice(0, 10)
}

// 执行导入
const executeImport = async () => {
  importStatus.value = 'importing'
  importProgress.value = 0
  
  try {
    // 模拟导入过程
    const total = 100
    const interval = setInterval(() => {
      importProgress.value += 10
      if (importProgress.value >= 100) {
        clearInterval(interval)
        importStatus.value = 'success'
        ElMessage.success('数据导入成功')
      }
    }, 300)
  } catch (error) {
    importStatus.value = 'error'
    ElMessage.error('数据导入失败: ' + error.message)
  }
}

// 初始化
onMounted(() => {
  fetchGraphList()
})
</script>

<style scoped>
.data-process-container {
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

.process-card {
  border-radius: 8px;
}

.card-header {
  font-size: 18px;
  font-weight: 500;
}

.step-content {
  margin: 30px 0;
  min-height: 300px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.upload-area {
  max-width: 500px;
  margin: 0 auto;
}

.mapping-header, .preview-header, .import-summary {
  text-align: center;
  margin-bottom: 20px;
}

.mapping-header h3, .preview-header h3, .import-summary h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.mapping-header p, .preview-header p, .import-summary p {
  color: #909399;
  margin: 0;
}

.import-progress {
  margin: 30px 0;
  text-align: center;
}

.success-text {
  color: #67c23a;
}

.error-text {
  color: #f56c6c;
}
</style>