<template>
  <div class="graph-statistics-container">
    <div class="page-header">
      <h2 class="page-title">图统计信息</h2>
      <p class="page-description">查看图的统计信息，包括节点数、边数等</p>
    </div>

    <el-card class="statistics-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span v-if="currentGraph">当前图：{{ currentGraph.name }}</span>
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
            <el-table :data="vertexLabelStats" style="width: 100%">
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
            <el-table :data="edgeLabelStats" style="width: 100%">
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useGraphStore } from '@/stores/graph'
import { graphApi } from '@/api/graph'

// 使用图存储
const graphStore = useGraphStore()

// 计算属性：当前选中的图
const currentGraph = computed(() => graphStore.currentGraph)

// 响应式数据
const statistics = ref({
  vertexCount: 0,
  edgeCount: 0,
  vertexLabelCount: {},
  edgeLabelCount: {}
})
const loading = ref(false)

// 计算属性
const vertexLabelStats = computed(() => {
  if (!statistics.value.vertexLabelCount) return []
  return Object.entries(statistics.value.vertexLabelCount).map(([label, count]) => ({
    label,
    count
  }))
})

const edgeLabelStats = computed(() => {
  if (!statistics.value.edgeLabelCount) return []
  return Object.entries(statistics.value.edgeLabelCount).map(([label, count]) => ({
    label,
    count
  }))
})

// 获取统计信息
const fetchStatistics = async () => {
  if (!currentGraph.value) return
  
  try {
    loading.value = true
    const response = await graphApi.getGraphSummary(currentGraph.value.id)
    statistics.value = response.data
  } catch (error) {
    console.error('获取统计信息失败:', error)
    ElMessage.error('获取统计信息失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 监听当前图的变化
watch(currentGraph, (newGraph) => {
  if (newGraph) {
    fetchStatistics()
  } else {
    // 清空统计信息
    statistics.value = {
      vertexCount: 0,
      edgeCount: 0,
      vertexLabelCount: {},
      edgeLabelCount: {}
    }
  }
})

// 生命周期钩子
onMounted(() => {
  // 如果已经有选中的图，获取统计信息
  if (currentGraph.value) {
    fetchStatistics()
  }
})
</script>

<style scoped>
.graph-statistics-container {
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
  margin-bottom: 30px;
}

.stat-item {
  text-align: center;
  padding: 20px 0;
  border-radius: 4px;
  background-color: #f5f7fa;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
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
</style>