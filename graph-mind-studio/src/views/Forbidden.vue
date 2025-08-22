<template>
  <div class="forbidden-container">
    <div class="forbidden-content">
      <el-result icon="error" title="403" sub-title="抱歉，您没有权限访问该页面">
        <template #icon>
          <el-icon class="forbidden-icon"><CircleClose /></el-icon>
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
import { CircleClose } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/home')
}
</script>

<style scoped>
.forbidden-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
}

.forbidden-content {
  text-align: center;
}

.forbidden-icon {
  width: 60px;
  height: 60px;
  color: #f56c6c;
}
</style>