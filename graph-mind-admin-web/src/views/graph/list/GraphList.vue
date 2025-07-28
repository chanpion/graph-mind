<template>
  <div class="graph-list-container">
    <div class="page-header">
      <h2 class="page-title">图管理</h2>
      <p class="page-description">以卡片形式展示所有图，包含图标识、名称、所用连接、点数、边数</p>
    </div>
    <div class="card-list">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="graph in graphs" :key="graph.id">
          <el-card class="graph-card" shadow="hover">
            <div class="card-header">
              <el-avatar :size="48" icon="Share" style="background: #409EFF; color: #fff;" />
              <div class="graph-info">
                <div class="graph-name">{{ graph.name }}</div>
                <div class="graph-id">ID: {{ graph.id }}</div>
              </div>
            </div>
            <div class="card-body">
              <div class="graph-meta">
                <span class="meta-label">连接：</span>
                <el-tag size="small" type="info">{{ graph.connectionName }}</el-tag>
              </div>
              <div class="graph-meta">
                <span class="meta-label">点数：</span>
                <el-tag size="small" type="success">{{ graph.nodeCount }}</el-tag>
                <span class="meta-label">边数：</span>
                <el-tag size="small" type="warning">{{ graph.edgeCount }}</el-tag>
              </div>
              <div class="card-actions">
                <el-button size="small" type="primary" @click="goDetail(graph)">详情</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup name="GraphList">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Share } from '@element-plus/icons-vue'
import graphApi from '@/api/graph'
import { useRouter } from 'vue-router'

const router = useRouter()

const graphs = ref([])

const fetchGraphs = async () => {
  try {
    const res = await graphApi.getGraphList()
    graphs.value = res.data.list
  } catch (e) {
    ElMessage.error('获取图列表失败')
  }
}

const goDetail = (graph) => {
  router.push(`/home/graph/detail/${graph.id}`)
}

onMounted(() => {
  fetchGraphs()
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
.card-list {
  margin-top: 10px;
}
.graph-card {
  margin-bottom: 20px;
  border-radius: 10px;
  overflow: hidden;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}
.graph-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.graph-name {
  font-size: 18px;
  font-weight: 500;
  color: #409EFF;
}
.graph-id {
  font-size: 12px;
  color: #909399;
}
.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.graph-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.meta-label {
  color: #606266;
}
.card-actions {
  margin-top: 10px;
  text-align: right;
}
</style> 