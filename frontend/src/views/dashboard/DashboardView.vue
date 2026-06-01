<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="tech-gradient">
        <h1 class="welcome-title">欢迎使用图数据库管理系统</h1>
        <p class="welcome-subtitle">统一管理多种图数据库，简化数据操作和可视化分析</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stats-card" v-loading="statsLoading">
        <div class="stats-content">
          <div class="stats-icon connection-icon">
            <el-icon><Connection /></el-icon>
          </div>
          <div class="stats-info">
            <div class="stats-number">{{ stats.connectionCount }}</div>
            <div class="stats-label">活跃连接</div>
          </div>
        </div>
      </div>

      <div class="stats-card" v-loading="statsLoading">
        <div class="stats-content">
          <div class="stats-icon graph-icon">
            <el-icon><DataBoard /></el-icon>
          </div>
          <div class="stats-info">
            <div class="stats-number">{{ stats.graphCount }}</div>
            <div class="stats-label">图数据库</div>
          </div>
        </div>
      </div>

      <div class="stats-card">
        <div class="stats-content">
          <div class="stats-icon query-icon">
            <el-icon><Search /></el-icon>
          </div>
          <div class="stats-info">
            <div class="stats-number">{{ stats.queryCount }}</div>
            <div class="stats-label">查询执行</div>
          </div>
        </div>
      </div>

      <div class="stats-card">
        <div class="stats-content">
          <div class="stats-icon import-icon">
            <el-icon><Upload /></el-icon>
          </div>
          <div class="stats-info">
            <div class="stats-number">{{ stats.importCount }}</div>
            <div class="stats-label">数据导入</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快速操作区域 -->
    <div class="quick-actions-section">
      <div class="quick-actions-grid">
        <div class="quick-actions-panel">
          <h3 class="panel-title">快速操作</h3>
          <div class="actions-list">
            <el-button type="primary" class="action-button" @click="router.push('/home/connections')">
              <el-icon class="action-icon"><Connection /></el-icon>
              管理数据库连接
            </el-button>
            <el-button class="action-button" @click="router.push('/home/graphs')">
              <el-icon class="action-icon"><Search /></el-icon>
              图管理列表
            </el-button>
            <el-button class="action-button" @click="router.push('/home/data')">
              <el-icon class="action-icon"><Upload /></el-icon>
              图数据管理
            </el-button>
            <el-button class="action-button" @click="router.push('/home/visualization')">
              <el-icon class="action-icon"><Share /></el-icon>
              图查询可视化
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 支持的数据库类型 -->
    <div class="supported-dbs-section">
      <h3 class="panel-title">支持的图数据库</h3>
      <div class="dbs-grid">
        <div class="db-item">
          <div class="db-icon neo4j-icon">N</div>
          <div class="db-info">
            <div class="db-name">Neo4j</div>
            <div class="db-description">Cypher 查询语言</div>
          </div>
        </div>

        <div class="db-item">
          <div class="db-icon nebula-icon">N</div>
          <div class="db-info">
            <div class="db-name">Nebula</div>
            <div class="db-description">nGQL 查询语言</div>
          </div>
        </div>

        <div class="db-item">
          <div class="db-icon janus-icon">J</div>
          <div class="db-info">
            <div class="db-name">Janus</div>
            <div class="db-description">Gremlin 查询语言</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Connection, DataBoard, Search, Upload, Share } from '@element-plus/icons-vue'
import { connectionApi } from '@/views/connections/api/connection'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'

const router = useRouter()
const graphsStore = useGraphsStore()

const statsLoading = ref(false)
const stats = reactive({
  connectionCount: 0,
  graphCount: 0,
  queryCount: 0,
  importCount: 0
})

async function loadStats() {
  statsLoading.value = true
  try {
    const connections = await connectionApi.list()
    // 后端返回格式: { code: 200, data: { records: [...], total: ... }, message: '...' }
    const connList = connections?.data?.records || []
    stats.connectionCount = connList.length || 0

    await graphsStore.fetchGraphs()
    stats.graphCount = (graphsStore.graphs || []).length || 0
    
    // 从 localStorage 读取查询和导入历史
    loadQueryAndImportStats()
  } catch (err) {
    console.error('加载统计数据失败:', err)
  } finally {
    statsLoading.value = false
  }
}

function loadQueryAndImportStats() {
  try {
    // 读取查询历史
    const queryHistory = localStorage.getItem('recentQueries')
    const queryList = queryHistory ? JSON.parse(queryHistory) : []
    stats.queryCount = queryList.length
    
    // 读取导入历史
    const importHistory = localStorage.getItem('importHistory')
    const importList = importHistory ? JSON.parse(importHistory) : []
    stats.importCount = importList.length
  } catch {
    stats.queryCount = 0
    stats.importCount = 0
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 5px 24px;
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 欢迎区域 */
.welcome-section {
  flex-shrink: 0;
}

.tech-gradient {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  padding: 32px 40px;
  color: white;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.tech-gradient::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 10px;
  position: relative;
  z-index: 1;
}

.welcome-subtitle {
  font-size: 16px;
  opacity: 0.95;
  margin: 0;
  position: relative;
  z-index: 1;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  flex-shrink: 0;
}

.stats-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 18px 22px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--el-border-color-light);
  position: relative;
  overflow: hidden;
}

.stats-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--el-color-primary), var(--el-color-primary-light-3));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stats-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
}

.stats-card:hover::before {
  opacity: 1;
}

.stats-content {
  display: flex;
  align-items: center;
}

.stats-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: white;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.connection-icon { 
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
}
.graph-icon { 
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); 
}
.query-icon { 
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); 
}
.import-icon { 
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); 
}

.stats-info {
  flex: 1;
}

.stats-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stats-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

/* 快速操作区域 */
.quick-actions-section {
  flex: 1;
  display: flex;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  width: 100%;
}

.quick-actions-panel {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-light);
  height: 100%;
  box-sizing: border-box;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--el-border-color-light);
}

.actions-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.action-button {
  justify-content: flex-start;
  height: 48px;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-icon {
  margin-right: 8px;
  font-size: 16px;
}

/* 支持的数据库 */
.supported-dbs-section {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.dbs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.db-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--el-fill-color-light);
}

.db-item:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  background: var(--el-bg-color);
}

.db-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  color: white;
  font-weight: 700;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.neo4j-icon { background: linear-gradient(135deg, #008CC1 0%, #0073A8 100%); }
.nebula-icon { background: linear-gradient(135deg, #00B4A0 0%, #009B8C 100%); }
.janus-icon { background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%); }

.db-info {
  flex: 1;
}

.db-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  font-size: 15px;
}

.db-description {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dbs-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .dbs-grid {
    grid-template-columns: 1fr;
  }

  .tech-gradient {
    padding: 24px 28px;
  }

  .welcome-title {
    font-size: 24px;
  }

  .welcome-subtitle {
    font-size: 14px;
  }
  
  .actions-list {
    grid-template-columns: 1fr;
  }
}
</style>
