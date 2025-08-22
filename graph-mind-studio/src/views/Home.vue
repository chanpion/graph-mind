<template>
  <div class="system-container">
    <el-container class="main-wrapper">
      <!-- 左侧导航区域 Sidebar组件 -->
      <Sidebar
          :is-collapse="isCollapse"
          :menuData="menu_list.data"
          :active-menu-index="activeMenuIndex"
      />

      <!-- 右侧内容区 -->
      <el-container>
        <el-header class="operate-header">
          <div class="header-left">
            <el-button link class="collapse-btn" @click="toggleSidebar">
              <el-icon :size="20">
                <component :is="collapseIcon"/>
              </el-icon>
            </el-button>
            <!-- 面包屑组件区域 -->
            <div class="breadcrumb-container">
              <el-breadcrumb separator="/">
                <Breadcrumb/>
              </el-breadcrumb>
            </div>
          </div>
          <div class="toolbar">
            <!-- 图切换下拉框 -->
            <el-select
              v-if="graphOptions.length > 0"
              :model-value="graphStore.currentGraph?.id"
              :options="graphOptions"
              placeholder="请选择图"
              size="small"
              style="width: 200px; margin-right: 16px;"
              @change="handleGraphChange"
            >
              <el-option
                v-for="graph in graphStore.graphList"
                :key="graph.id"
                :label="graph.name"
                :value="graph.id"
              />
            </el-select>
            
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userInfo.avatar">
                  {{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="username">{{ userInfo.nickname || userInfo.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 多页签导航组件 -->
        <Tags />

        <!-- 主要内容区域 -->
        <el-main>
          <div class="content-card">
            <div class="table-container">
              <!-- 直接在首页显示仪表盘内容 -->
              <RouterView v-if="showChildRoutes"/>
              <Dashboard v-else />
            </div>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Fold, Expand, ArrowDown } from '@element-plus/icons-vue'
import { menuData } from '@/utils/commonData'
import { useRoute } from 'vue-router'

import Sidebar from '@/components/layout/Sidebar.vue'
import Breadcrumb from '@/components/layout/Breadcrumb.vue'
import Tags from '@/components/layout/Tags.vue'
import Dashboard from './Index.vue' // 导入仪表盘组件

// 引入 Pinia stores
import { useTabsStore } from '@/stores/tabs'
import { useUserStore } from '@/stores/user'
import { useGraphStore } from '@/stores/graph'
import { graphApi } from '@/api/graph'

const router = useRouter()
const route = useRoute()
const tabsStore = useTabsStore()
const userStore = useUserStore()
const graphStore = useGraphStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 判断是否显示子路由内容
const showChildRoutes = computed(() => {
  return route.path !== '/home' && route.path !== '/home/'
})

// 监听路由变化，自动添加页签（首页处理特殊）
watch(
    () => route.path,
    (newPath) => {
      let title
      if (newPath === '/home' || newPath === '/home/' || newPath === '/home/index') {
        title = '首页'
      } else {
        title = typeof route.meta.title === 'string' ? route.meta.title : '无标题'
      }
      tabsStore.addTab({ title, path: newPath })
    },
    { immediate: true }
)

// 侧边栏状态
const isCollapse = ref(false)
const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

// 菜单数据
const menu_list = menuData

// 计算当前应该激活的菜单项
const activeMenuIndex = computed(() => {
  // 获取当前路由路径
  const currentPath = route.path
  
  // 特殊处理首页
  if (currentPath === '/home' || currentPath === '/home/' || currentPath === '/home/index') {
    return '/home' // 首页菜单路径
  }
  
  // 在菜单数据中查找匹配的菜单项
  for (const menu of menu_list.data) {
    // 检查一级菜单是否匹配
    if (menu.path && currentPath.startsWith(menu.path)) {
      // 检查子菜单是否匹配
      for (const child of menu.children) {
        if (child.path && currentPath === child.path) {
          // 返回子菜单的路径作为激活索引
          return child.path
        }
        
        // 处理带参数的路由（如图详情页）
        if (child.path && child.path.includes('/:')) {
          const basePath = child.path.split('/:')[0]
          if (currentPath.startsWith(basePath)) {
            return child.path
          }
        }
      }
      
      // 如果没有匹配的子菜单，返回一级菜单的路径
      return menu.path
    }
  }
  
  // 默认返回首页菜单路径
  return '/home'
})

// 折叠图标计算属性
const collapseIcon = computed(() => isCollapse.value ? Expand : Fold)

// 处理用户操作
const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/home/admin/profile')
      break
    case 'settings':
      router.push('/home/admin/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        // 清除登录信息
        userStore.logout()
        
        // 清除页签状态
        tabsStore.$reset()
        
        ElMessage.success('已退出登录')
        
        // 跳转到登录页
        router.push('/login')
      } catch {
        // 用户取消退出
      }
      break
  }
}

// 图相关状态
const graphLoading = ref(false)
const graphOptions = computed(() => {
  return graphStore.graphList.map(graph => ({
    value: graph.id,
    label: graph.name
  }))
})

// 获取图列表
const fetchGraphList = async () => {
  try {
    graphLoading.value = true
    const response = await graphApi.getGraphs()
    const graphList = response.data.records || response.data.list || []
    graphStore.setGraphList(graphList)
    
    // 如果当前没有选中图，且图列表不为空，则默认选中第一个
    if (!graphStore.currentGraph && graphList.length > 0) {
      graphStore.setCurrentGraph(graphList[0])
    }
  } catch (error) {
    console.error('获取图列表失败:', error)
    ElMessage.error('获取图列表失败')
  } finally {
    graphLoading.value = false
  }
}

// 处理图切换
const handleGraphChange = (graphId) => {
  const selectedGraph = graphStore.graphList.find(graph => graph.id === graphId)
  if (selectedGraph) {
    graphStore.setCurrentGraph(selectedGraph)
  }
}

// 组件挂载时检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
  }
  // 获取图列表
  fetchGraphList()
})
</script>

<style scoped>
@import '@/assets/home.css';

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.3s;
}

.user-info:hover {
  background-color: rgba(64, 158, 255, 0.1);
}

.username {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>