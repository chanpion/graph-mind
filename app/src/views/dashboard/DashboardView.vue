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
            <el-button class="action-button" @click="router.push('/home/export')">
              <el-icon class="action-icon"><Upload /></el-icon>
              导入导出数据
            </el-button>
          </div>
        </div>

        <div class="recent-queries-panel">
          <h3 class="panel-title">最近查询</h3>
          <div v-if="recentQueries.length > 0" class="queries-list">
            <div
              v-for="(item, index) in recentQueries.slice(0, 3)"
              :key="index"
              class="query-item"
              @click="router.push('/home/graphs')"
            >
              <div class="query-text">{{ item }}</div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon class="empty-icon"><Search /></el-icon>
            <p>暂无查询记录</p>
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
            <div class="db-name">Nebula Graph</div>
            <div class="db-description">nGQL 查询语言</div>
          </div>
        </div>

        <div class="db-item">
          <div class="db-icon janus-icon">J</div>
          <div class="db-info">
            <div class="db-name">JanusGraph</div>
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
import { Connection, DataBoard, Search, Upload } from '@element-plus/icons-vue'
import { connectionApi } from '@/views/connections/api/connection'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'

const router = useRouter()
const graphsStore = useGraphsStore()

const statsLoading = ref(false)
const stats = reactive({
  connectionCount: 0,
  graphCount: 0,
  queryCount: 0,
  importCount: 12
})

// 最近查询（从 localStorage 读取）
const recentQueries = ref([])

async function loadStats() {
  statsLoading.value = true
  try {
    const connections = await connectionApi.list()
    const connList = Array.isArray(connections) ? connections : (connections?.data || [])
    stats.connectionCount = connList.length || 0

    await graphsStore.fetchGraphs()
    stats.graphCount = (graphsStore.graphs || []).length || 0
  } catch (err) {
    console.error('加载统计数据失败:', err)
  } finally {
    statsLoading.value = false
  }
}

function loadRecentQueries() {
  try {
    const saved = localStorage.getItem('recentQueries')
    recentQueries.value = saved ? JSON.parse(saved) : []
    stats.queryCount = recentQueries.value.length
  } catch {
    recentQueries.value = []
  }
}

onMounted(() => {
  loadStats()
  loadRecentQueries()
})
</script>

<style scoped>
.dashboard-container {
  padding: 16px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 64px);
  height: 100%;
  box-sizing: border-box;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: 20px;
}

.tech-gradient {
  background: linear-gradient(135deg, #409EFF 0%, #3375B9 50%, #2C6AA0 100%);
  border-radius: 10px;
  padding: 20px 24px;
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 6px;
}

.welcome-subtitle {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stats-card {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 8px var(--el-box-shadow-light);
  transition: all 0.3s ease;
  border: 1px solid var(--el-border-color-light);
}

.stats-card:hover {
  box-shadow: 0 4px 16px var(--el-box-shadow);
  transform: translateY(-2px);
}

.stats-content {
  display: flex;
  align-items: center;
}

.stats-icon {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: white;
  font-size: 18px;
}

.connection-icon { background: linear-gradient(135deg, #409EFF 0%, #3375B9 100%); }
.graph-icon { background: linear-gradient(135deg, #67C23A 0%, #5DAF34 100%); }
.query-icon { background: linear-gradient(135deg, #E6A23C 0%, #D19B3A 100%); }
.import-icon { background: linear-gradient(135deg, #F56C6C 0%, #E55C5C 100%); }

.stats-number {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.stats-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

/* 快速操作区域 */
.quick-actions-section {
  margin-bottom: 20px;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.quick-actions-panel,
.recent-queries-panel {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 8px var(--el-box-shadow-light);
  border: 1px solid var(--el-border-color-light);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 14px;
}

.actions-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-button {
  justify-content: flex-start;
  height: 40px;
  font-size: 13px;
}

.action-icon {
  margin-right: 6px;
}

/* 最近查询 */
.queries-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.query-item {
  padding: 10px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.query-item:hover {
  background: var(--el-fill-color);
  transform: translateX(4px);
}

.query-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.empty-state {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 24px 0;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.5;
}

/* 支持的数据库 */
.supported-dbs-section {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 8px var(--el-box-shadow-light);
  border: 1px solid var(--el-border-color-light);
}

.dbs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.db-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  transition: all 0.2s ease;
}

.db-item:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 2px 8px var(--el-box-shadow-light);
}

.db-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  color: white;
  font-weight: 600;
  font-size: 14px;
}

.neo4j-icon { background: linear-gradient(135deg, #008CC1 0%, #0073A8 100%); }
.nebula-icon { background: linear-gradient(135deg, #00B4A0 0%, #009B8C 100%); }
.janus-icon { background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%); }

.db-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 2px;
  font-size: 13px;
}

.db-description {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions-grid {
    grid-template-columns: 1fr;
  }

  .dbs-grid {
    grid-template-columns: 1fr;
  }

  .tech-gradient {
    padding: 16px 20px;
  }

  .welcome-title {
    font-size: 20px;
  }
}
</style>
