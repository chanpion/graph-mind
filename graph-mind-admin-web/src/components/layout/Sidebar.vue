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

      <!-- 一级菜单 -->
      <el-sub-menu
          v-for="item in menuData"
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
            :index="child.path.startsWith('/') ? child.path : (item.path + '/' + child.path)"
        >
          <el-icon>
            <component :is="getIcon(child.icon)"/>
          </el-icon>
          <span>{{ child.authName }}</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getIcon } from '@/utils/icons'

const props = defineProps({
  isCollapse: Boolean,
  menuData: Array,
  activeMenuIndex: String
})
</script>

<style scoped>
@import '@/assets/home.css';
</style>