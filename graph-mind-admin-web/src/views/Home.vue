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
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userInfo.avatar">
                  {{ userInfo.nickname?.charAt(0) || 'U' }}
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
              <RouterView/>
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

// 引入 Pinia stores
import { useTabsStore } from '@/stores/tabs'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const tabsStore = useTabsStore()
const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 监听路由变化，自动添加页签（首页处理特殊）
watch(
    () => route.path,
    (newPath) => {
      let title
      if (newPath === '/home') {
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
      
      // 如果没有匹配的子菜单，返回一级菜单的ID
      return String(menu.id)
    }
  }
  
  // 默认返回图库管理菜单ID
  return '200'
})

// 折叠图标计算属性
const collapseIcon = computed(() => isCollapse.value ? Expand : Fold)

// 处理用户操作
const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人中心功能开发中...')
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中...')
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

// 组件挂载时检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
  }
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
