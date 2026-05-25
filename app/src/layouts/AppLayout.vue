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
        <el-select
          :model-value="currentGraphId"
          placeholder="选择图"
          size="small"
          clearable
          filterable
          class="global-graph-selector"
          @update:model-value="handleGraphChange"
        >
          <el-option
            v-for="g in graphs"
            :key="g.id"
            :label="g.graphName || g.name"
            :value="g.id"
          />
        </el-select>
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
      <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="app-sidebar">
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
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/views/auth/stores/useAuthStore'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'
import { useTheme } from '@/shared/composables/useTheme'
import { storeToRefs } from 'pinia'
import SidebarNav from './components/SidebarNav.vue'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const graphsStore = useGraphsStore()
const { currentGraphId, graphs } = storeToRefs(graphsStore)
const { toggleTheme, isDark } = useTheme()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

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

function handleGraphChange(graphId) {
  const graph = graphs.value.find(g => g.id === graphId)
  if (graph) {
    graphsStore.setCurrentGraph(graph)
  }
}

onMounted(() => {
  if (graphs.value.length === 0) {
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

.global-graph-selector {
  width: 280px;
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
  padding: 16px;
  background: var(--el-bg-color-page);
}
</style>
