<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <el-result icon="warning" title="404" sub-title="抱歉，您访问的页面不存在">
        <template #icon>
          <el-icon class="not-found-icon"><Warning /></el-icon>
        </template>
        <template #extra>
          <el-button type="primary" @click="goHome">回到首页</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/home')
}
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
}

.not-found-content {
  text-align: center;
}

.not-found-icon {
  width: 60px;
  height: 60px;
  color: #f56c6c;
}
</style>