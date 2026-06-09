<template>
  <el-card class="graph-card" @click="$emit('click', graph)">
    <div class="card-header">
      <div class="icon-wrapper" :class="`icon-${dbTypeClass}`">
        <el-icon :size="22" color="#fff">
          <Share />
        </el-icon>
      </div>
      <div class="graph-info">
        <h4 class="graph-name">{{ graph.name || graph.graphName || graph.code || graph.graphCode }}</h4>
        <p class="graph-code">标识: {{ graph.code || graph.graphCode }}</p>
        <p v-if="graph.description" class="graph-description">{{ graph.description }}</p>
        <div class="graph-tags">
          <el-tag size="small" :type="statusTagType">{{ statusText }}</el-tag>
          <el-tag v-if="graph.sourceType" size="small" :type="sourceTagType">{{ sourceText }}</el-tag>
          <el-tag size="small" :type="dbTagType" class="db-tag">{{ dbTypeLabel }}</el-tag>
        </div>
      </div>
    </div>

    <div class="card-stats">
      <div class="stat-item">
        <div class="stat-value">{{ graph.vertexTypeCount != null ? graph.vertexTypeCount : '--' }}</div>
        <div class="stat-label">节点类型</div>
      </div>
      <el-divider direction="vertical" />
      <div class="stat-item">
        <div class="stat-value">{{ graph.edgeTypeCount != null ? graph.edgeTypeCount : '--' }}</div>
        <div class="stat-label">边类型</div>
      </div>
    </div>

    <div class="card-footer">
      <el-tooltip content="打开" placement="top">
        <button class="icon-btn primary" @click.stop="$emit('open', graph)">
          <el-icon><Position /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip content="编辑" placement="top">
        <button class="icon-btn secondary" @click.stop="$emit('edit', graph)">
          <el-icon><Edit /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip content="详情" placement="top">
        <button class="icon-btn secondary" @click.stop="$emit('detail', graph)">
          <el-icon><Document /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip content="浏览数据" placement="top">
        <button class="icon-btn secondary" @click.stop="$emit('browse', graph)">
          <el-icon><DataLine /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip content="删除" placement="top">
        <button class="icon-btn danger" @click.stop="$emit('delete', graph)">
          <el-icon><Delete /></el-icon>
        </button>
      </el-tooltip>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { Share, DataLine, Position, Edit, Document, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  graph: { type: Object, required: true }
})

defineEmits(['click', 'open', 'edit', 'detail', 'delete', 'browse'])

const dbType = computed(() => props.graph.graphType || props.graph.type || '')
const dbTypeClass = computed(() => {
  const t = dbType.value.toLowerCase()
  if (t.includes('neo4j')) return 'neo4j'
  if (t.includes('nebula')) return 'nebula'
  if (t.includes('janus')) return 'janus'
  return 'default'
})

const statusTagType = computed(() => {
  const s = props.graph.status
  if (s === 'NORMAL' || s === 0) return 'success'
  if (s === 'ABNORMAL' || s === 1) return 'danger'
  return 'info'
})

const statusText = computed(() => {
  const s = props.graph.status
  if (s === 'NORMAL' || s === 0) return '正常'
  if (s === 'ABNORMAL' || s === 1) return '异常'
  return '未知'
})

const sourceTagType = computed(() => {
  return props.graph.sourceType === 'PLATFORM' ? 'success' : 'info'
})

const sourceText = computed(() => {
  return props.graph.sourceType === 'PLATFORM' ? '平台创建' : '已有'
})

const dbTypeLabel = computed(() => {
  const labels = { neo4j: 'Neo4j', nebula: 'Nebula', janus: 'Janus' }
  return labels[dbType.value.toLowerCase()] || dbType.value || '未知'
})

const dbTagType = computed(() => {
  const types = { neo4j: 'warning', nebula: 'success', janus: 'danger' }
  return types[dbType.value.toLowerCase()] || 'info'
})

function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return String(num)
}
</script>

<style scoped>
.graph-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--el-border-color);
  height: 260px;
  display: flex;
  flex-direction: column;
}
.graph-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: var(--el-color-primary);
}

.card-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.icon-wrapper {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}
.graph-card:hover .icon-wrapper {
  transform: scale(1.1) rotate(5deg);
}
.icon-neo4j { background: linear-gradient(135deg, #FFB347, #F5A623); }
.icon-nebula { background: linear-gradient(135deg, #00F5A9, #00D6A9); }
.icon-janus { background: linear-gradient(135deg, #A29BFE, #6C5CE7); }
.icon-default { background: linear-gradient(135deg, #667eea, #764ba2); }

.graph-info { flex: 1; min-width: 0; }
.graph-name {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.graph-description {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.graph-code {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.graph-tags { display: flex; gap: 4px; flex-wrap: wrap; }

.card-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 10px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 8px;
  flex-shrink: 0;
}
.stat-item { display: flex; flex-direction: column; align-items: center; flex: 1; }
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.2;
}
.stat-label { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }

.card-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-light);
  margin-top: auto;
  flex-shrink: 0;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
  outline: none;
}
.icon-btn.primary {
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}
.icon-btn.primary:hover {
  transform: translateY(-2px) scale(1.1);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
}
.icon-btn.secondary {
  background: var(--el-fill-color);
  color: var(--el-text-color-regular);
  border: 1px solid var(--el-border-color);
}
.icon-btn.secondary:hover {
  background: var(--el-fill-color-dark);
  transform: translateY(-2px) scale(1.1);
  color: var(--el-color-primary);
}
.icon-btn.danger {
  background: linear-gradient(135deg, #EF4444, #DC2626);
  color: white;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}
.icon-btn.danger:hover {
  transform: translateY(-2px) scale(1.1);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
}
</style>
