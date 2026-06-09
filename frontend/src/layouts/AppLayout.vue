<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <el-button text @click="toggleSidebar">
          <el-icon :size="20">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </el-button>
        <span class="app-title">Graph Mind</span>
      </div>
      <div class="header-right">
        <div v-if="!hideSelectors" class="connection-selector-wrapper">
          <el-icon class="selector-icon"><Connection /></el-icon>
          <el-select
            v-model="selectedConnectionId"
            placeholder="选择连接"
            size="small"
            clearable
            filterable
            class="header-selector"
            @update:model-value="handleConnectionChange"
          >
            <el-option
              v-for="conn in connections"
              :key="conn.id"
              :label="conn.name || conn.alias"
              :value="conn.id"
            >
              <div class="conn-option">
                <span class="conn-option-name">{{ conn.name || conn.alias }}</span>
                <el-tag v-if="connLabel(conn)" :type="connTagType(conn)" size="small" effect="plain" class="conn-option-tag">
                  {{ connLabel(conn) }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </div>
        <div v-if="!hideSelectors" class="graph-selector-wrapper">
          <el-icon class="selector-icon"><Grid /></el-icon>
          <el-select
            :model-value="currentGraphId"
            :disabled="!selectedConnectionId"
            placeholder="选择图"
            size="small"
            clearable
            filterable
            class="header-selector"
            @update:model-value="handleGraphChange"
          >
            <el-option
              v-for="g in graphs"
              :key="g.id"
              :label="g.code || g.graphCode || g.graphName || g.name"
              :value="g.id"
            >
              <div class="graph-option">
                <span class="graph-option-name">{{ g.code || g.graphCode }}</span>
                <el-tag :type="dbTagType(g)" size="small" effect="plain" class="graph-option-tag">
                  {{ dbLabel(g) }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </div>
        <el-button text circle @click="toggleTheme">
          <el-icon :size="18">
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
        </el-button>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <span class="user-profile">
            <el-avatar :size="28">{{ userInitial }}</el-avatar>
            <span class="username">{{ username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container class="app-body">
      <el-aside :width="sidebarCollapsed ? '64px' : '180px'" class="app-sidebar">
        <SidebarNav />
      </el-aside>
      <el-container class="app-main">
        <div class="app-content">
          <router-view />
        </div>
      </el-container>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/views/auth/stores/useAuthStore'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import { useTheme } from '@/shared/composables/useTheme'
import { storeToRefs } from 'pinia'
import SidebarNav from './components/SidebarNav.vue'
import { Grid, Connection } from '@element-plus/icons-vue'
import { connectionApi } from '@/views/connections/api/connection'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()
const graphsStore = useGraphsStore()
const { currentGraphId, graphs, selectedConnectionId } = storeToRefs(graphsStore)
const { toggleTheme, isDark } = useTheme()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

// 不需要显示连接/图选择器的页面
const hideSelectors = computed(() => {
  const hiddenRoutes = ['Dashboard', 'Connections', 'Graphs', 'User']
  return hiddenRoutes.includes(route.name)
})

// 连接列表
const connections = ref([])

// 数据库类型标签映射
const DB_TAG_MAP = {
  neo4j: { label: 'Neo4j', type: 'primary' },
  nebula: { label: 'Nebula', type: 'success' },
  janus: { label: 'Janus', type: 'warning' }
}

function connLabel(conn) {
  const raw = (conn.type || conn.databaseType || '').toLowerCase()
  return DB_TAG_MAP[raw]?.label || raw.toUpperCase()
}

function connTagType(conn) {
  const raw = (conn.type || conn.databaseType || '').toLowerCase()
  return DB_TAG_MAP[raw]?.type || 'info'
}

function dbLabel(graph) {
  const raw = (graph.graphType || '').toLowerCase()
  return DB_TAG_MAP[raw]?.label || raw.toUpperCase()
}

function dbTagType(graph) {
  const raw = (graph.graphType || '').toLowerCase()
  return DB_TAG_MAP[raw]?.type || 'info'
}

function toggleSidebar() {
  appStore.toggleSidebar()
}

const username = computed(() => authStore.userInfo?.username || 'Admin')
const userInitial = computed(() => username.value.charAt(0).toUpperCase())

function handleUserCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/home/admin/profile')
  } else if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

async function handleConnectionChange(connectionId) {
  graphsStore.setSelectedConnection(connectionId)
  graphsStore.setCurrentGraph(null)
  await graphsStore.fetchGraphsByConnection(connectionId)
}

function handleGraphChange(graphId) {
  const graph = graphs.value.find(g => g.id === graphId)
  if (graph) {
    graphsStore.setCurrentGraph(graph)
  } else {
    graphsStore.setCurrentGraph(null)
  }
}

onMounted(async () => {
  try {
    const res = await connectionApi.list()
    const data = res?.data || res
    connections.value = Array.isArray(data) ? data : data?.records || []
  } catch (err) {
    console.error('加载连接列表失败:', err)
  }
  if (selectedConnectionId.value) {
    await graphsStore.fetchGraphsByConnection(selectedConnectionId.value)
  } else if (graphs.value.length === 0) {
    graphsStore.fetchGraphs()
  }
})
</script>

<style scoped>
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-center {
  flex: 1;
  justify-content: center;
}

.connection-selector-wrapper,
.graph-selector-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.selector-icon {
  font-size: 16px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.header-selector {
  width: 160px;
}

.header-selector :deep(.el-select__wrapper) {
  border-radius: 6px;
  min-height: 30px;
  padding: 0 8px;
}

.header-selector :deep(.el-select__placeholder) {
  font-size: 13px;
}

.conn-option,
.graph-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.conn-option-name,
.graph-option-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}

.conn-option-tag,
.graph-option-tag {
  flex-shrink: 0;
  font-size: 11px;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  margin-left: 4px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.username {
  font-size: 13px;
}

.app-body {
  flex: 1;
  overflow: hidden;
}

.app-sidebar {
  transition: width 0.3s;
  overflow: hidden;
  border-right: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.app-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-content {
  flex: 1;
  overflow: auto;
  padding: 5px;
  background: var(--el-bg-color-page);
}
</style>
