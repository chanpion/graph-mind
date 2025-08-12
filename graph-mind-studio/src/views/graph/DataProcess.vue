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
        <el-step title="选择实体" />
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
        </el-form>
        
        <div class="step-actions">
          <el-button type="primary" @click="nextStep" :disabled="!form.graphId">下一步</el-button>
        </div>
      </div>

      <!-- 步骤2: 选择实体（点或边） -->
      <div v-show="currentStep === 1" class="step-content">
        <el-form :model="form" label-width="100px" style="max-width: 500px; margin: 20px auto;">
          <el-form-item label="数据类型">
            <el-radio-group v-model="form.dataType">
              <el-radio value="node">点数据</el-radio>
              <el-radio value="edge">边数据</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <!-- 点类型选择 -->
          <el-form-item label="点类型" v-if="form.dataType === 'node'">
            <el-select 
              v-model="form.nodeTypeId" 
              placeholder="请选择点类型" 
              style="width: 100%"
              @change="handleNodeTypeChange"
            >
              <el-option
                v-for="nodeType in nodeTypes"
                :key="nodeType.id"
                :label="nodeType.name"
                :value="nodeType.id">
              </el-option>
            </el-select>
          </el-form-item>
          
          <!-- 边类型选择 -->
          <el-form-item label="边类型" v-if="form.dataType === 'edge'">
            <el-select 
              v-model="form.edgeTypeId" 
              placeholder="请选择边类型" 
              style="width: 100%"
              @change="handleEdgeTypeChange"
            >
              <el-option
                v-for="edgeType in edgeTypes"
                :key="edgeType.id"
                :label="edgeType.name"
                :value="edgeType.id">
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
        
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!isEntityTypeSelected">下一步</el-button>
        </div>
      </div>

      <!-- 步骤3: 上传文件 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="upload-area">
          <el-upload
            class="upload-demo"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :show-file-list="true"
            :limit="1"
            :on-exceed="handleFileExceed"
            accept=".csv"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将CSV文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                请上传CSV格式文件，大小不超过10MB，最多只能上传一个文件
              </div>
            </template>
          </el-upload>
        </div>
        
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!csvFile">下一步</el-button>
        </div>
      </div>

      <!-- 步骤4: 数据映射 -->
      <div v-show="currentStep === 3" class="step-content">
        <div class="mapping-header">
          <h3>数据映射配置</h3>
          <p>根据选中的{{ form.dataType === 'node' ? '点' : '边' }}类型设置属性映射</p>
        </div>
        
        <el-table :data="dynamicMappingFields" style="width: 100%" border>
          <el-table-column prop="label" label="字段" width="200" />
          <el-table-column label="CSV列">
            <template #default="{ row }">
              <el-select 
                v-model="row.csvColumn" 
                placeholder="请选择CSV列" 
                style="width: 100%"
                @change="handleMappingChange(row)"
              >
                <el-option label="无" value="" />
                <el-option 
                  v-for="header in csvHeaders" 
                  :key="header.column" 
                  :label="header.column" 
                  :value="header.column" 
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="示例数据" width="200">
            <template #default="{ row }">
              <span v-if="row.csvColumn">
                {{ getSampleData(row.csvColumn) }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!isMappingValid">下一步</el-button>
        </div>
      </div>

      <!-- 步骤5: 数据预览 -->
      <div v-show="currentStep === 4" class="step-content">
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

      <!-- 步骤6: 执行导入 -->
      <div v-show="currentStep === 5" class="step-content">
        <div class="import-summary">
          <h3>导入配置确认</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="目标图">{{ selectedGraph.name }}</el-descriptions-item>
            <el-descriptions-item label="数据类型">{{ form.dataType === 'node' ? '点数据' : '边数据' }}</el-descriptions-item>
            <el-descriptions-item label="实体类型">{{ form.dataType === 'node' ? selectedNodeType.name : selectedEdgeType.name }}</el-descriptions-item>
<!--            <el-descriptions-item label="文件名">{{ csvFile.name }}</el-descriptions-item>-->
            <el-descriptions-item label="文件名"></el-descriptions-item>
            <el-descriptions-item label="数据行数">{{ previewData.length }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="import-result-display" v-if="importStatus !== ''">
          <div v-if="importStatus === 'importing'" class="importing-status">
            <el-progress type="circle" :percentage="importProgress" />
            <p class="status-text">正在导入数据...</p>
          </div>
          
          <div v-else-if="importStatus === 'success' && importResult" class="success-status">
            <el-result icon="success" title="数据导入完成">
              <template #sub-title>
                <div class="result-stats">
                  <el-descriptions :column="1" border>
                    <el-descriptions-item label="总计">{{ importResult.totalCount }}条</el-descriptions-item>
                    <el-descriptions-item label="成功">{{ importResult.successCount }}条</el-descriptions-item>
                    <el-descriptions-item label="失败">{{ importResult.failureCount }}条</el-descriptions-item>
                  </el-descriptions>
                </div>
                
                <div v-if="importResult.failureCount > 0" class="error-details">
                  <h4>错误详情:</h4>
                  <el-table :data="errorMessages" style="width: 100%" size="small" max-height="200">
                    <el-table-column prop="index" label="#" width="50" />
                    <el-table-column prop="message" label="错误信息" />
                  </el-table>
                  <div class="retry-actions" style="margin-top: 15px; text-align: center;">
                    <el-button type="primary" @click="retryImport">重试失败数据</el-button>
                  </div>
                </div>
              </template>
            </el-result>
          </div>
          
          <div v-else-if="importStatus === 'error'" class="error-status">
            <el-result icon="error" title="数据导入失败">
              <template #sub-title>
                <p>导入过程中发生错误，请检查数据格式或联系管理员。</p>
                <div class="retry-actions">
                  <el-button type="primary" @click="retryImport">重试</el-button>
                </div>
              </template>
            </el-result>
          </div>
        </div>
        
        <div class="step-actions">
          <el-button @click="prevStep" :disabled="importStatus === 'importing'">上一步</el-button>
          <el-button 
            type="primary" 
            @click="executeImport" 
            :loading="importStatus === 'importing'"
            :disabled="importStatus === 'success'"
            v-if="importStatus === '' || importStatus === 'error'"
          >
            执行导入
          </el-button>
          <el-button 
            type="primary" 
            @click="resetImport" 
            v-if="importStatus === 'success'"
          >
            继续导入
          </el-button>
          <el-button 
            @click="resetWizard" 
            v-if="importStatus === 'success'"
          >
            重新开始
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
const importStatus = ref('') // '' | 'importing' | 'success' | 'error'
const importResult = ref(null) // 导入结果数据

// 表单数据
const form = ref({
  graphId: '',
  dataType: 'node', // node or edge
  nodeTypeId: '',
  edgeTypeId: ''
})

// 图列表
const graphList = ref([])
const selectedGraph = ref({})

// 点类型和边类型
const nodeTypes = ref([])
const edgeTypes = ref([])
const selectedNodeType = ref({})
const selectedEdgeType = ref({})

// CSV文件相关
const csvFile = ref(null)
const csvData = ref([])
const csvHeaders = ref([])

// 预览数据
const previewData = ref([])

// 映射字段
const nodeMappingFields = ref([
  { value: 'uid', label: 'uid (唯一标识)' },
  { value: 'label', label: '标签(类型)' },
  { value: 'properties', label: '属性(可多个)' }
])

const edgeMappingFields = ref([
  { value: 'uid', label: 'uid (唯一标识)' },
  { value: 'source', label: '起点uid' },
  { value: 'target', label: '终点uid' },
  { value: 'label', label: '标签(类型)' },
  { value: 'properties', label: '属性(可多个)' }
])

// 根据选中的点/边类型动态生成映射字段
const dynamicMappingFields = ref([])

// 添加一个方法来更新映射字段
const updateDynamicMappingFields = () => {
  if (form.value.dataType === 'node' && selectedNodeType.value.properties) {
    // 基础字段
    const fields = [
      { value: 'uid', label: 'uid (唯一标识)', required: false, csvColumn: '' }
    ];
    
    // 添加点类型属性字段
    selectedNodeType.value.properties.filter(prop => prop.code !== 'uid').forEach(prop => {
      fields.push({
        value: `${prop.code}`,
        label: `${prop.name}(${prop.code})`,
        required: prop.required || false,
        csvColumn: ''
      });
    });
    
    dynamicMappingFields.value = fields;
  } else if (form.value.dataType === 'edge' && selectedEdgeType.value.properties) {
    // 基础字段
    const fields = [
      { value: 'uid', label: 'uid (唯一标识)', required: false, csvColumn: '' },
      { value: 'source', label: '起点uid', required: true, csvColumn: '' },
      { value: 'sourceLabel', label: '起点类型', required: true, csvColumn: '' },
      { value: 'target', label: '终点uid', required: true, csvColumn: '' },
      { value: 'targetLabel', label: '终点类型', required: true, csvColumn: '' }
    ];
    
    // 添加边类型属性字段
    selectedEdgeType.value.properties.filter(prop => prop.code !== 'uid').forEach(prop => {
      fields.push({
        value: `${prop.code}`,
        label: `${prop.name}(${prop.code})`,
        required: prop.required || false,
        csvColumn: ''
      });
    });
    
    dynamicMappingFields.value = fields;
  } else {
    // 默认字段
    const defaultFields = form.value.dataType === 'node' 
      ? [
          { value: 'uid', label: 'uid (唯一标识)', required: true, csvColumn: '' },
          { value: 'label', label: '标签(类型)', required: false, csvColumn: '' },
          { value: 'properties', label: '属性(可多个)', required: false, csvColumn: '' }
        ]
      : [
          { value: 'uid', label: 'uid (唯一标识)', required: false, csvColumn: '' },
          { value: 'source', label: '起点uid', required: true, csvColumn: '' },
          { value: 'target', label: '终点uid', required: true, csvColumn: '' },
          { value: 'label', label: '标签(类型)', required: false, csvColumn: '' },
          { value: 'properties', label: '属性(可多个)', required: false, csvColumn: '' }
        ];
    dynamicMappingFields.value = defaultFields;
  }
}

const isMappingValid = computed(() => {
  // 检查必要字段是否已映射
  return dynamicMappingFields.value.filter(field => field.required)
    .every(field => field.csvColumn !== '');
})

// 检查是否选择了实体类型
const isEntityTypeSelected = computed(() => {
  if (form.value.dataType === 'node') {
    return !!form.value.nodeTypeId
  } else {
    return !!form.value.edgeTypeId
  }
})

// 获取图列表
const fetchGraphList = async () => {
  try {
    const res = await graphApi.getGraphs()
    // 根据API返回的数据结构正确设置graphList
    if (res.data.records) {
      graphList.value = res.data.records
    } else if (res.data.list) {
      graphList.value = res.data.list
    } else {
      graphList.value = res.data
    }
  } catch (e) {
    console.error('获取图列表失败:', e)
    ElMessage.error('获取图列表失败: ' + (e.message || '未知错误'))
  }
}

// 获取点类型列表
const fetchNodeTypes = async (graphId) => {
  try {
    const res = await graphApi.getNodeDefs(graphId)
    nodeTypes.value = res.data
  } catch (e) {
    console.error('获取点类型列表失败:', e)
    ElMessage.error('获取点类型列表失败: ' + (e.message || '未知错误'))
  }
}

// 获取边类型列表
const fetchEdgeTypes = async (graphId) => {
  try {
    const res = await graphApi.getEdgeDefs(graphId)
    edgeTypes.value = res.data
  } catch (e) {
    console.error('获取边类型列表失败:', e)
    ElMessage.error('获取边类型列表失败: ' + (e.message || '未知错误'))
  }
}

// 处理图选择变化
const handleGraphChange = (graphId) => {
  selectedGraph.value = graphList.value.find(g => g.id === graphId) || {}
  // 重置实体类型选择
  form.value.nodeTypeId = ''
  form.value.edgeTypeId = ''
  selectedNodeType.value = {}
  selectedEdgeType.value = {}
  
  // 获取点类型和边类型
  if (graphId) {
    fetchNodeTypes(graphId)
    fetchEdgeTypes(graphId)
  }
}

// 处理点类型选择变化
const handleNodeTypeChange = (nodeTypeId) => {
  selectedNodeType.value = nodeTypes.value.find(n => n.id === nodeTypeId) || {}
  // 更新映射字段
  updateDynamicMappingFields();
}

// 处理边类型选择变化
const handleEdgeTypeChange = (edgeTypeId) => {
  selectedEdgeType.value = edgeTypes.value.find(e => e.id === edgeTypeId) || {}
  // 更新映射字段
  updateDynamicMappingFields();
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
  
  if (currentStep.value === 1 && !isEntityTypeSelected.value) {
    ElMessage.warning('请选择实体类型')
    return
  }
  
  if (currentStep.value === 2 && !csvFile.value) {
    ElMessage.warning('请上传CSV文件')
    return
  }
  
  if (currentStep.value < 5) {
    currentStep.value++
    
    // 特殊处理：在进入数据预览步骤时生成预览数据
    if (currentStep.value === 4) {
      generatePreviewData()
    }
  }
}

// 处理文件上传数量超出限制
const handleFileExceed = (files, uploadFiles) => {
  ElMessage.warning('最多只能上传一个文件，请先移除已上传的文件')
}

// 处理文件上传
const handleFileChange = (file, fileList) => {
  // 如果已有文件，先清空之前的文件
  if (csvFile.value) {
    // 清空之前的数据
    csvData.value = []
    csvHeaders.value = []
  }
  
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
      // 更新映射字段
      updateDynamicMappingFields();
      // 自动映射相同名称的字段
      autoMapFields();
    },
    error: (error) => {
      ElMessage.error('CSV文件解析失败: ' + error.message)
    }
  })
}

// 检查某个CSV列是否已经被映射
const isColumnMapped = (column) => {
  return dynamicMappingFields.value.some(field => field.csvColumn === column);
};

// 自动映射相同名称的字段
const autoMapFields = () => {
  // 遍历所有需要映射的字段
  dynamicMappingFields.value.forEach(field => {
    // 查找是否有同名的CSV列
    const matchingHeader = csvHeaders.value.find(header => 
      header.column.toLowerCase() === field.value.toLowerCase() || 
      header.column.toLowerCase() === field.label.toLowerCase()
    );
    
    // 如果找到同名的CSV列且该列还没有被映射，则自动映射
    if (matchingHeader && !isColumnMapped(matchingHeader.column)) {
      field.csvColumn = matchingHeader.column;
    }
  });
}

// 处理映射变化
const handleMappingChange = (row) => {
  // 检查是否有重复映射
  dynamicMappingFields.value.forEach(field => {
    if (field !== row && field.csvColumn === row.csvColumn && field.csvColumn !== '') {
      field.csvColumn = '';
    }
  });
}

// 获取示例数据
const getSampleData = (column) => {
  const header = csvHeaders.value.find(h => h.column === column);
  return header ? header.sample : '';
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
    // 准备导入数据
    const formData = new FormData()
    formData.append('file', csvFile.value)
    
    // 构建映射关系
    const mapping = {};
    dynamicMappingFields.value.forEach(field => {
      if (field.csvColumn) {
        mapping[field.value] = field.csvColumn;
      }
    });
    
    formData.append('headers', JSON.stringify(csvHeaders.value))
    formData.append('mapping', JSON.stringify(mapping))
    formData.append('data', JSON.stringify(csvData.value))
    
    // 调用后端导入接口
    let response
    if (form.value.dataType === 'node') {
      // 导入点数据
      response = await graphApi.importNodeData(
        form.value.graphId, 
        form.value.nodeTypeId, 
        formData
      )
    } else {
      // 导入边数据
      response = await graphApi.importEdgeData(
        form.value.graphId, 
        form.value.edgeTypeId, 
        formData
      )
    }
    
    // 直接设置进度为100%
    importProgress.value = 100
    importStatus.value = 'success'
    
    // 存储导入结果
    if (response && response.data) {
      importResult.value = response.data
      ElMessage.success(
        `数据导入完成！成功: ${response.data.successCount}条, 失败: ${response.data.failureCount}条`
      )
    } else {
      ElMessage.success('数据导入成功')
    }
  } catch (error) {
    importStatus.value = 'error'
    console.error('数据导入失败:', error)
    ElMessage.error('数据导入失败: ' + (error.message || '未知错误'))
  }
}

// 重试导入
const retryImport = async () => {
  importStatus.value = 'importing'
  importProgress.value = 0
  
  try {
    // 准备导入数据 - 仅导入失败的数据
    const formData = new FormData()
    formData.append('file', csvFile.value)
    
    // 如果有失败的数据，则仅重试失败的数据
    let retryData = csvData.value
    if (importResult.value && importResult.value.failureCount > 0) {
      // 这里可以根据实际需求进行调整，比如只重试失败的记录
      // 当前实现是重试所有数据
      console.log('重试导入，失败记录数：', importResult.value.failureCount)
    }
    
    // 构建映射关系
    const mapping = {};
    dynamicMappingFields.value.forEach(field => {
      if (field.csvColumn) {
        mapping[field.value] = field.csvColumn;
      }
    });
    
    formData.append('headers', JSON.stringify(csvHeaders.value))
    formData.append('mapping', JSON.stringify(mapping))
    formData.append('data', JSON.stringify(retryData))
    
    // 调用后端导入接口
    let response
    if (form.value.dataType === 'node') {
      // 导入点数据
      response = await graphApi.importNodeData(
        form.value.graphId, 
        form.value.nodeTypeId, 
        formData
      )
    } else {
      // 导入边数据
      response = await graphApi.importEdgeData(
        form.value.graphId, 
        form.value.edgeTypeId, 
        formData
      )
    }
    
    // 直接设置进度为100%
    importProgress.value = 100
    importStatus.value = 'success'
    
    // 存储导入结果
    if (response && response.data) {
      importResult.value = response.data
      ElMessage.success(
        `数据导入完成！成功: ${response.data.successCount}条, 失败: ${response.data.failureCount}条`
      )
    } else {
      ElMessage.success('数据导入成功')
    }
  } catch (error) {
    importStatus.value = 'error'
    console.error('数据导入失败:', error)
    ElMessage.error('数据导入失败: ' + (error.message || '未知错误'))
  }
}

// 错误信息列表（用于表格展示）
const errorMessages = computed(() => {
  if (importResult.value && importResult.value.errorMessages) {
    return importResult.value.errorMessages.map((message, index) => ({
      index: index + 1,
      message: message
    }))
  }
  return []
})

// 重置导入状态
const resetImport = () => {
  importStatus.value = ''
  importProgress.value = 0
  importResult.value = null
  currentStep.value = 0
}

// 重置整个向导
const resetWizard = () => {
  // 重置表单
  form.value = {
    graphId: '',
    dataType: 'node',
    nodeTypeId: '',
    edgeTypeId: ''
  }
  
  // 重置文件
  csvFile.value = null
  csvHeaders.value = []
  csvData.value = []
  
  // 重置节点和边类型列表
  nodeTypes.value = []
  edgeTypes.value = []
  
  // 重置导入状态
  importStatus.value = ''
  importProgress.value = 0
  importResult.value = null
  
  // 回到第一步
  currentStep.value = 0
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

.step-content {
  min-height: 400px;
  padding: 20px 0;
}

.step-actions {
  margin-top: 30px;
  text-align: right;
}

.import-result-display {
  margin: 30px 0;
  min-height: 200px;
}

.form-item {
  margin-bottom: 22px;
}

.csv-preview {
  margin-top: 20px;
}

.preview-table {
  margin-top: 15px;
}

.mapping-table {
  margin-top: 15px;
}

.preview-actions {
  margin-top: 20px;
  text-align: center;
}


.result-card {
  border: none;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.status-text {
  margin-top: 20px;
  text-align: center;
  font-size: 16px;
  color: #606266;
}

.result-stats {
  margin: 20px 0;
}

.error-details {
  margin-top: 20px;
}

.error-details h4 {
  margin-bottom: 10px;
  color: #303133;
}

.result-actions {
  margin-top: 30px;
  text-align: center;
}

.result-actions .el-button {
  margin: 0 10px;
}

.retry-actions {
  margin-top: 20px;
}

.retry-actions .el-button {
  margin: 0 5px;
}
</style>