<template>
  <div class="graph-statistics-container">
    <div class="page-header">
      <h2 class="page-title">图统计信息</h2>
    </div>

    <el-card class="statistics-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span v-if="currentGraph">当前图：{{ currentGraph.graphName || currentGraph.name }}</span>
            <span v-else>未选择图</span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="fetchStatistics" :disabled="!currentGraph" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="!currentGraph" class="empty-placeholder">
          <el-empty description="请先在图管理中选择一个图" />
        </div>

        <div v-else>
          <!-- 基本统计信息 -->
          <el-row :gutter="20" class="basic-stats">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ statistics.vertexCount || 0 }}</div>
                <div class="stat-label">节点总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ statistics.edgeCount || 0 }}</div>
                <div class="stat-label">边总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ Object.keys(statistics.vertexLabelCount || {}).length }}</div>
                <div class="stat-label">节点标签数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ Object.keys(statistics.edgeLabelCount || {}).length }}</div>
                <div class="stat-label">边类型数</div>
              </div>
            </el-col>
          </el-row>

          <!-- 节点标签统计 -->
          <el-card class="detail-card" shadow="never">
            <template #header>
              <div class="detail-header">
                <span>节点标签统计</span>
              </div>
            </template>
            <el-table :data="vertexLabelStats" class="full-width-table">
              <el-table-column prop="label" label="标签名称" />
              <el-table-column prop="count" label="节点数" />
            </el-table>
          </el-card>

          <!-- 边类型统计 -->
          <el-card class="detail-card" shadow="never">
            <template #header>
              <div class="detail-header">
                <span>边类型统计</span>
              </div>
            </template>
            <el-table :data="edgeLabelStats" class="full-width-table">
              <el-table-column prop="label" label="边类型" />
              <el-table-column prop="count" label="边数" />
            </el-table>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import { graphApi } from '@/views/graphs/api/graph'

const graphsStore = useGraphsStore()

const graphId = ref(graphsStore.currentGraphId || '')
const statistics = reactive({
  vertexCount: 0,
  edgeCount: 0,
  vertexLabelCount: {},
  edgeLabelCount: {}
})
const loading = ref(false)

const currentGraph = computed(() => graphsStore.currentGraph)

const vertexLabelStats = computed(() => {
  if (!statistics.vertexLabelCount) return []
  return Object.entries(statistics.vertexLabelCount).map(([label, count]) => ({
    label,
    count
  }))
})

const edgeLabelStats = computed(() => {
  if (!statistics.edgeLabelCount) return []
  return Object.entries(statistics.edgeLabelCount).map(([label, count]) => ({
    label,
    count
  }))
})

async function fetchStatistics() {
  if (!currentGraph.value) return

  try {
    loading.value = true
    const response = await graphApi.getGraphSummary(graphsStore.currentGraphId)
    const data = response?.data || response
    Object.assign(statistics, data)
  } catch (error) {
    console.error('获取统计信息失败:', error)
    ElMessage.error('获取统计信息失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function loadGraphSummary(id) {
  try {
    loading.value = true
    const response = await graphApi.getGraphSummary(id)
    const data = response?.data || response
    Object.assign(statistics, data)
  } catch (error) {
    console.error('加载图统计信息失败:', error)
    ElNotification.error({
      title: '加载图统计信息失败',
      message: error.message || '未知错误'
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const effectiveId = graphsStore.currentGraphId
  if (effectiveId) {
    graphId.value = effectiveId
    loadGraphSummary(effectiveId)
  } else if (graphsStore.currentGraph) {
    graphId.value = graphsStore.currentGraphId
    loadGraphSummary(graphId.value)
  } else {
    ElMessage.warning('未选择图，请先选择一个图')
  }
})

watch(() => graphsStore.currentGraph, (newGraph) => {
  if (newGraph && newGraph.id !== graphId.value) {
    graphId.value = newGraph.id
    loadGraphSummary(graphId.value)
  }
})
</script>

<style scoped>
.graph-statistics-container {
  padding: 5px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
}

.page-description {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.statistics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.basic-stats {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px 0;
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-color-primary);
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.detail-card {
  margin-bottom: 20px;
}

.detail-header {
  font-weight: bold;
  font-size: 16px;
}

.empty-placeholder {
  text-align: center;
  padding: 40px 0;
}

.full-width-table {
  width: 100%;
}
</style>
