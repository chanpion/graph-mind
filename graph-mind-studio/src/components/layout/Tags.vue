<template>
  <!-- Element Plus 的标签页组件 -->
  <!--
   绑定当前激活的标签页
   监听关闭标签页的事件
   允许手动关闭标签
   -->

  <el-tabs
      v-model="activeTab"
  type="card"
  @tab-remove="handleTabRemove"
  closable
  >
  <!--
    遍历所有标签页
    设定唯一的 key，提升渲染性能
    设定标签页的唯一标识符
    显示的标签名称
    控制是否可关闭（首页不可关闭）

  -->
  <el-tab-pane
      v-for="tab in tabs"
      :key="tab.path"
  :name="tab.path"
  :label="tab.title"
  :closable="tab.closable"
  >
  </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

const router = useRouter()  // 获取 Vue Router 实例
const tabsStore = useTabsStore()  // 获取标签页的状态管理

// 计算属性：获取当前所有的标签页数据
const tabs = computed(() => tabsStore.tabs)

console.log(tabs)

// 计算属性：当前激活的标签页，支持双向绑定
const activeTab = computed({
  get: () => tabsStore.activeTab,  // 获取当前激活的标签页路径
  set: (val) => {
    tabsStore.setActiveTab(val)  // 更新 Pinia 中的状态
    router.push(val)  // 切换路由
  }
})

// 处理标签页的关闭事件
const handleTabRemove = (targetName) => {
  tabsStore.removeTab(targetName)  // 调用 store 方法删除标签
  router.push(tabsStore.activeTab)  // 切换到删除后的激活标签
}
</script>

<style scoped>
/* 整体 Tabs 容器样式 */
.el-tabs {
  background-color: #fff;               /* 背景颜色：白色 */
  border: 1px solid #ebeef5;            /* 浅灰色边框 */
  border-radius: 4px;                   /* 圆角边框 */
  margin: 1px 0;                        /* 上下外边距 */
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); /* 添加柔和阴影 */
}

/* 标签页头部样式 */
.el-tabs__header {
  background-color: #fff;               /* 头部背景保持白色 */
  border-bottom: 1px solid #ebeef5;      /* 添加底部边框 */
  padding: 0 10px;                      /* 左右内边距 */
}

/* 单个标签页内容区域样式 */
.el-tab-pane {
  font-size: 14px;                      /* 统一字体大小 */
  padding: 2px;                         /* 内边距调整 */
}
</style>
