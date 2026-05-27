<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    collapse-transition
  >
    <el-menu-item index="/home/dashboard" @click="go('/home/dashboard')">
      <el-icon><House /></el-icon>
      <template #title>首页</template>
    </el-menu-item>
    <el-menu-item index="/home/connections" @click="go('/home/connections')">
      <el-icon><Connection /></el-icon>
      <template #title>连接管理</template>
    </el-menu-item>
    <el-menu-item index="/home/graphs" @click="go('/home/graphs')">
      <el-icon><Grid /></el-icon>
      <template #title>图列表</template>
    </el-menu-item>
    <el-menu-item index="/home/modeling" @click="go('/home/modeling')">
      <el-icon><DataLine /></el-icon>
      <template #title>图建模</template>
    </el-menu-item>
    <el-menu-item index="/home/data" @click="goWithGraph('/home/data')">
      <el-icon><List /></el-icon>
      <template #title>图数据</template>
    </el-menu-item>
    <el-menu-item index="/home/visualization" @click="goWithGraph('/home/visualization')">
      <el-icon><Share /></el-icon>
      <template #title>图查询</template>
    </el-menu-item>
    <el-menu-item index="/home/analysis" @click="goWithGraph('/home/analysis')">
      <el-icon><DataAnalysis /></el-icon>
      <template #title>图分析</template>
    </el-menu-item>
    <el-menu-item index="/home/summary" @click="goWithGraph('/home/summary')">
      <el-icon><PieChart /></el-icon>
      <template #title>图统计</template>
    </el-menu-item>
    <el-menu-item index="/home/user" @click="go('/home/user')">
      <el-icon><User /></el-icon>
      <template #title>用户管理</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useGraphsStore } from '@/views/graphs/stores/useGraphsStore'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const graphsStore = useGraphsStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const activeMenu = computed(() => route.path)

function go(path) {
  router.push(path)
}

function goWithGraph(basePath) {
  const graphId = graphsStore.currentGraphId
  if (!graphId) {
    ElMessage.warning('请先选择图')
    router.push('/home/graphs')
    return
  }
  router.push(basePath)
}
</script>
