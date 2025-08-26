<!-- src/components/common/Breadcrumb.vue -->
<template>
  <!-- 动态生成面包屑项 -->
  <el-breadcrumb separator="/">

    <!--  面包屑组件-->
    <!-- :to="item.path"    绑定跳转路径，如截图中可点击的导航 -->
    <el-breadcrumb-item
        v-for="(item, index) in breadcrumbItems"
        :key="index"
        :to="item.path"
    >
      <!-- 显示标题，如图中的"人员管理" -->
      {{ item.title }}

    </el-breadcrumb-item>


  </el-breadcrumb>
</template>

<script setup>
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import {menuData} from '@/utils/commonData' // 导入菜单数据（如截图左侧导航数据）

const route = useRoute() // 获取当前路由信息

// 核心计算属性：生成面包屑导航数据
const breadcrumbItems = computed(() => {
  const matched = route.matched
      .filter(item => !item.meta?.hidden)
      .map(item => {
        const titleSource = item.meta?.title ? '路由' : '菜单'
        const finalTitle = item.meta?.title || findMenuTitle(item.path)

        // 调试信息（与截图中的控制台日志格式匹配）
        // console.log(
        //     `[面包屑来源] 路径: ${item.path.padEnd(25)} | 来源: ${titleSource.padEnd(4)} | 标题: ${finalTitle}`
        // )

        return {
          path: item.path,
          title: finalTitle
        }
      })

  return matched.length ? matched : [{ path: '/home', title: '首页' }]
})

// 从菜单数据查找标题,如果路由未设置 则从菜单里查找
const findMenuTitle = (path) => {
  // 扁平化菜单数据结构（包含多级菜单）
  const flatMenu = menuData.data.flatMap(item => [
    item,
    ...(item.children || []) // 展开子菜单（如"用户管理"展开到同级）
  ])

  // 查找匹配路径的菜单项
  return flatMenu.find(menu => menu.path === path)?.authName || path // 默认显示路径
}

</script>