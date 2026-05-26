<template>
  <div class="pagination-container">
    <el-pagination
      v-bind="$attrs"
      :background="background"
      :layout="layout"
      :total="total"
      :page-sizes="pageSizes"
      :default-page-size="defaultLimit"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  limit: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
  background: { type: Boolean, default: true },
  autoScroll: { type: Boolean, default: true },
  hidden: { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

const defaultLimit = computed(() => props.limit)

function handleSizeChange(val) {
  emit('update:limit', val)
  emit('pagination', { page: 1, limit: val })
}

function handleCurrentChange(val) {
  emit('update:page', val)
  emit('pagination', { page: val, limit: props.limit })
}
</script>

<style scoped>
.pagination-container {
  text-align: center;
  margin-top: 20px;
}
</style>
