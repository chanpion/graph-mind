<template>
  <el-aside
      :width="isCollapse ? '64px' : '240px'"
      class="nav-sidebar"
  >
    <div class="system-logo" :class="{ 'logo-collapse': isCollapse }">
      <span v-if="!isCollapse" class="logo-text">图数据库管理系统</span>
      <span v-else class="logo-text">易</span>
    </div>

    <el-menu
        :default-active="activeMenuIndex"
        class="sidebar-menu"
        background-color="#304156"
        text-color="#b0bac5"
        active-text-color="#fff"
        :collapse="isCollapse"
        :collapse-transition="false"
        :router="true"
    >
      <!-- 首页作为顶级菜单项 -->
      <el-menu-item 
        index="/home"
        @click="handleHomeClick"
      >
        <el-icon class="menu-icon">
          <component :is="getIcon('Odometer')"/>
        </el-icon>
        <template #title>
          <span>首页</span>
        </template>
      </el-menu-item>

      <!-- 其他菜单项 -->
      <el-sub-menu
          v-for="item in getNonHomeMenuItems()"
          :key="item.id"
          :index="String(item.id)"
      >
        <template #title>
          <el-icon class="menu-icon">
            <component :is="getIcon(item.icon)"/>
          </el-icon>
          <span>{{ item.authName }}</span>
        </template>

        <!-- 二级菜单 -->
        <el-menu-item
            v-for="child in item.children"
            :key="child.id"
            :index="child.path"
        >
          <el-icon>
            <component :is="getIcon(child.icon)"/>
          </el-icon>
          <template #title>
            <span>{{ child.authName }}</span>
          </template>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getIcon } from '@/utils/icons'
import { useRouter } from 'vue-router'

const props = defineProps({
  isCollapse: Boolean,
  menuData: Array,
  activeMenuIndex: String
})

const router = useRouter()
const route = useRoute()

// 获取首页菜单项的索引
const getIndexForHomeMenuItem = () => {
  return '/home'
}

// 获取非首页菜单项
const getNonHomeMenuItems = () => {
  return props.menuData.filter(item => item.id !== 100)
}

// 处理首页点击事件
const handleHomeClick = () => {
  router.push('/home')
}
</script>

<style scoped>
@import '@/assets/home.css';
</style>