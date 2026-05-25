<template>
  <div class="empty-state">
    <el-empty :description="description" :image-size="imageSize">
      <template v-if="$slots.default">
        <slot />
      </template>
    </el-empty>
  </div>
</template>

<script setup>
defineProps({
  description: {
    type: String,
    default: '暂无数据'
  },
  imageSize: {
    type: Number,
    default: 200
  }
})
</script>

<style scoped>
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}
</style>
