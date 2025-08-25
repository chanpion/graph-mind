<script setup>
import { ref, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { graphApi } from '@/api/graph'
import { connectionApi } from '@/api/connection'
import { userApi } from '@/api/user'
import { Share, Connection, DataAnalysis, User, Promotion, Upload } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

// 系统信息
const systemInfo = ref({
  version: '1.0.0',
  lastUpdate: '2025-08-01',
  totalGraphs: 0,
  totalConnections: 0,
  totalUsers: 0,
  totalTasks: 24
})

// 快捷操作
const quickActions = ref([
  { 
    id: 1, 
    title: '新建图连接', 
    description: '创建新的图数据库连接', 
    icon: markRaw(Connection),
    path: '/home/graph/connection' 
  },
  { 
    id: 2, 
    title: '创建图实例', 
    description: '创建新的图数据实例', 
    icon: markRaw(Promotion),
    path: '/home/graph/list' 
  },
  { 
    id: 3, 
    title: '图数据处理', 
    description: '处理图数据导入导出', 
    icon: markRaw(Upload),
    path: '/home/graph/process' 
  },
  { 
    id: 4, 
    title: '图可视化分析', 
    description: '进行图数据可视化分析', 
    icon: markRaw(DataAnalysis),
    path: '/home/graph/visual' 
  }
])

// 最近访问的图
const recentGraphs = ref([
  { id: 1, name: '企业知识图谱', type: 'Neo4j', updateTime: '2025-07-28' },
  { id: 2, name: '社交网络分析', type: 'Nebula', updateTime: '2025-07-25' },
  { id: 3, name: '推荐系统图谱', type: 'JanusGraph', updateTime: '2025-07-20' }
])

// 处理快捷操作点击事件
const handleQuickActionClick = (path) => {
  router.push(path)
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    // 获取图实例数量
    const graphResponse = await graphApi.getGraphs()
    systemInfo.value.totalGraphs = graphResponse.data.total || graphResponse.data.records?.length || 0

    // 获取连接数
    const connectionResponse = await connectionApi.getConnections()
    systemInfo.value.totalConnections = connectionResponse.data.total || connectionResponse.data.records?.length || 0

    // 获取用户数
    const userResponse = await userApi.getUsers()
    systemInfo.value.totalUsers = userResponse.data.total || userResponse.data.records?.length || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

onMounted(() => {
  // 获取统计数据
  fetchStatistics()
})
</script>

<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <h2>欢迎使用 Graph Mind Platform</h2>
      <p class="welcome-text">您好，{{ userStore.nickname || userStore.username }}，祝您今天愉快！</p>
    </div>

    <div class="stats-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon bg-blue">
              <el-icon size="24"><Share /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ systemInfo.totalGraphs }}</div>
              <div class="stat-label">图实例</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon bg-green">
              <el-icon size="24"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ systemInfo.totalConnections }}</div>
              <div class="stat-label">连接数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon bg-orange">
              <el-icon size="24"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">24</div>
              <div class="stat-label">分析任务</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon bg-purple">
              <el-icon size="24"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ systemInfo.totalUsers }}</div>
              <div class="stat-label">团队成员</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="content-section">
      <el-row :gutter="20">
        <el-col :span="16">
          <div class="content-card">
            <div class="card-header">
              <h3>快捷操作</h3>
            </div>
            <div class="quick-actions">
              <el-row :gutter="20">
                <el-col 
                  v-for="action in quickActions" 
                  :key="action.id" 
                  :span="12"
                >
                  <div class="action-item" @click="handleQuickActionClick(action.path)">
                    <div class="action-icon">
                      <el-icon size="20"><component :is="action.icon" /></el-icon>
                    </div>
                    <div class="action-info">
                      <h4>{{ action.title }}</h4>
                      <p>{{ action.description }}</p>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="content-card">
            <div class="card-header">
              <h3>系统信息</h3>
            </div>
            <div class="system-info">
              <ul>
                <li>
                  <span class="info-label">平台版本：</span>
                  <span class="info-value">v{{ systemInfo.version }}</span>
                </li>
                <li>
                  <span class="info-label">更新时间：</span>
                  <span class="info-value">{{ systemInfo.lastUpdate }}</span>
                </li>
                <li>
                  <span class="info-label">支持数据库：</span>
                  <span class="info-value">Neo4j, Nebula, JanusGraph, GES</span>
                </li>
                <li>
                  <span class="info-label">技术支持：</span>
                  <span class="info-value">chanpion@gmail.com</span>
                </li>
              </ul>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100%;
}

.welcome-section {
  margin-bottom: 30px;
}

.welcome-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.welcome-text {
  font-size: 16px;
  color: #606266;
}

.stats-section {
  margin-bottom: 30px;
}

.stat-card {
  display: flex;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  color: #fff;
}

.bg-blue {
  background: #409eff;
}

.bg-green {
  background: #67c23a;
}

.bg-orange {
  background: #e6a23c;
}

.bg-purple {
  background: #7a5af5;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 15px;
}

.quick-actions {
  padding: 10px 0;
  flex: 1;
}

.action-item {
  display: flex;
  padding: 15px;
  border-radius: 6px;
  transition: all 0.3s;
  cursor: pointer;
  margin-bottom: 15px;
  border: 1px solid #ebeef5;
}

.action-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px 0 rgba(64, 158, 255, 0.2);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  margin-right: 15px;
  flex-shrink: 0;
}

.action-info h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.action-info p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.system-info ul {
  padding: 0;
  margin: 0;
  list-style: none;
  flex: 1;
}

.system-info li {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  display: flex;
}

.system-info li:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 500;
  color: #606266;
  width: 100px;
}

.info-value {
  flex: 1;
  color: #303133;
}

.chart-placeholder {
  padding: 20px 0;
}

.chart-info {
  margin-bottom: 20px;
}

.chart-info:last-child {
  margin-bottom: 0;
}

.chart-info p {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #606266;
}
</style>