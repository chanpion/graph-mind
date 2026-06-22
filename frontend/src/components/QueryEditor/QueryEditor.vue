<template>
  <div class="query-editor">
    <!-- 查询编辑器 -->
    <div class="section editor-section">
      <h4 v-if="showHeader">查询语句</h4>
      <div class="query-editor-container">
        <div class="code-editor-wrapper" :class="{ 'with-header': showHeader }">
          <textarea
            v-model="queryStatement"
            ref="queryEditorRef"
            class="code-editor"
            @input="onInput"
            @keydown="onKeydown"
          ></textarea>
          <div class="code-highlight" ref="highlightRef"></div>
        </div>
        <div class="editor-actions">
          <slot name="actions-prepend"></slot>
          <el-button v-if="showFormatButton" size="small" @click="formatQuery">
            <el-icon><MagicStick /></el-icon>
            格式化
          </el-button>
          <el-button v-if="showClearButton" size="small" @click="clearQuery">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
          <el-button
            v-if="showExecuteButton"
            type="primary"
            size="small"
            @click="executeQuery"
            :loading="loading"
          >
            <el-icon><VideoPlay /></el-icon>
            查询
          </el-button>
          <slot name="actions-append"></slot>
        </div>

        <!-- 自动补全弹出框 -->
        <div v-if="showAutocomplete" class="autocomplete-popup">
          <div
            v-for="(item, index) in autocompleteItems"
            :key="index"
            :class="['autocomplete-item', { active: index === autocompleteIndex }]"
            @click="applyAutocomplete(item)"
          >
            {{ item }}
          </div>
        </div>
      </div>
    </div>

    <!-- 查询历史 -->
    <div v-if="showHistory && queryHistory.length > 0" class="section history-section">
      <h4>查询历史</h4>
      <div class="history-list">
        <div
          v-for="(item, index) in displayedHistory"
          :key="index"
          class="history-item"
          @click="loadHistoryQuery(item)"
        >
          <div class="history-content">{{ item.statement.substring(0, 60) }}{{ item.statement.length > 60 ? '...' : '' }}</div>
          <div class="history-time">{{ formatTime(item.timestamp) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Delete, VideoPlay } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  progress: {
    type: Number,
    default: 0
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  showFormatButton: {
    type: Boolean,
    default: true
  },
  showClearButton: {
    type: Boolean,
    default: true
  },
  showExecuteButton: {
    type: Boolean,
    default: true
  },
  showHistory: {
    type: Boolean,
    default: true
  },
  placeholder: {
    type: String,
    default: '输入图数据库查询语句，例如：MATCH (n)-[r]->(m) RETURN n, r, m LIMIT 50'
  },
  historyKey: {
    type: String,
    default: 'queryHistory'
  },
  historyLimit: {
    type: Number,
    default: 10
  },
  editorHeight: {
    type: String,
    default: '200px'
  },
  enableAutocomplete: {
    type: Boolean,
    default: false
  },
  autocompleteKeywords: {
    type: Array,
    default: () => [
      'MATCH', 'RETURN', 'WHERE', 'WITH', 'ORDER BY', 'LIMIT', 'CREATE', 'DELETE', 'SET', 'REMOVE',
      'MERGE', 'UNWIND', 'CALL', 'UNION', 'AS', 'AND', 'OR', 'NOT', 'IN', 'IS', 'NULL', 'TRUE', 'FALSE'
    ]
  },
  autocompleteFunctions: {
    type: Array,
    default: () => [
      'COUNT', 'SUM', 'AVG', 'MIN', 'MAX', 'COLLECT', 'DISTINCT', 'STARTS WITH', 'ENDS WITH', 'CONTAINS'
    ]
  }
})

const emit = defineEmits(['update:modelValue', 'execute', 'format', 'clear', 'history-load'])

// 响应式数据
const queryStatement = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const queryEditorRef = ref(null)
const highlightRef = ref(null)

// 查询历史
const queryHistory = ref([])

// 自动补全相关
const showAutocomplete = ref(false)
const autocompleteItems = ref([])
const autocompleteIndex = ref(0)

// 计算属性
const progressText = computed(() => {
  if (props.progress > 0) {
    return ` (${Math.round(props.progress * 100)}%)`
  }
  return ''
})

const displayedHistory = computed(() => {
  return queryHistory.value.slice(0, 5)
})

// 监听外部 modelValue 变化，同步更新语法高亮
watch(() => props.modelValue, () => {
  nextTick(() => {
    highlightSyntax()
  })
})

// 生命周期
onMounted(() => {
  loadQueryHistory()
  // 初始加载时执行语法高亮，确保默认查询语句可见
  nextTick(() => {
    highlightSyntax()
  })
})

// 方法
const executeQuery = () => {
  if (!queryStatement.value.trim()) {
    ElMessage.warning('请输入查询语句')
    return
  }
  emit('execute', queryStatement.value)
}

const formatQuery = () => {
  emit('format', queryStatement.value)
}

const clearQuery = () => {
  queryStatement.value = ''
  emit('clear')
}

const loadHistoryQuery = (historyItem) => {
  queryStatement.value = historyItem.statement
  emit('history-load', historyItem)
  nextTick(() => {
    highlightSyntax()
  })
}

const saveToQueryHistory = (queryInfo) => {
  queryHistory.value.unshift(queryInfo)
  if (queryHistory.value.length > props.historyLimit) {
    queryHistory.value = queryHistory.value.slice(0, props.historyLimit)
  }
  try {
    localStorage.setItem(props.historyKey, JSON.stringify(queryHistory.value))
  } catch (error) {
    console.error('保存查询历史失败:', error)
  }
}

const loadQueryHistory = () => {
  try {
    const saved = localStorage.getItem(props.historyKey)
    if (saved) {
      queryHistory.value = JSON.parse(saved)
    }
  } catch (error) {
    console.error('加载查询历史失败:', error)
  }
}

const highlightSyntax = () => {
  if (!highlightRef.value) return

  const text = queryStatement.value
  const keywords = props.autocompleteKeywords
  const functions = props.autocompleteFunctions

  let highlighted = text

  // 高亮关键字（保留原始大小写）
  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
    highlighted = highlighted.replace(regex, match => `<span class="keyword">${match}</span>`)
  })

  // 高亮函数（保留原始大小写）
  functions.forEach(func => {
    const regex = new RegExp(`\\b${func}\\b`, 'gi')
    highlighted = highlighted.replace(regex, match => `<span class="function">${match}</span>`)
  })

  // 高亮字符串
  highlighted = highlighted.replace(/'(.*?)'/g, '<span class="string">\'$1\'</span>')

  // 高亮数字
  highlighted = highlighted.replace(/\b\d+\b/g, '<span class="number">$&</span>')

  // 高亮变量和节点标识
  highlighted = highlighted.replace(/\$[a-zA-Z_][a-zA-Z0-9_]*/g, '<span class="variable">$&</span>')
  highlighted = highlighted.replace(/\([a-zA-Z_][a-zA-Z0-9_]*\)/g, '<span class="vertex">$&</span>')
  highlighted = highlighted.replace(/\[[a-zA-Z_][a-zA-Z0-9_]*\]/g, '<span class="relationship">$&</span>')

  highlightRef.value.innerHTML = highlighted
}

const handleKeydown = (event) => {
  // Tab键处理
  if (event.key === 'Tab') {
    event.preventDefault()
    if (showAutocomplete.value && props.enableAutocomplete) {
      applyAutocomplete(autocompleteItems.value[autocompleteIndex.value])
      return
    }

    const start = event.target.selectionStart
    const end = event.target.selectionEnd
    const value = queryStatement.value

    queryStatement.value = value.substring(0, start) + '  ' + value.substring(end)
    event.target.selectionStart = event.target.selectionEnd = start + 2

    nextTick(() => {
      highlightSyntax()
    })
  }

  // 自动补全导航
  if (showAutocomplete.value && props.enableAutocomplete) {
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      autocompleteIndex.value = Math.max(0, autocompleteIndex.value - 1)
    } else if (event.key === 'ArrowDown') {
      event.preventDefault()
      autocompleteIndex.value = Math.min(autocompleteItems.value.length - 1, autocompleteIndex.value + 1)
    } else if (event.key === 'Enter') {
      event.preventDefault()
      applyAutocomplete(autocompleteItems.value[autocompleteIndex.value])
    } else if (event.key === 'Escape') {
      event.preventDefault()
      showAutocomplete.value = false
    }
  }
}

const applyAutocomplete = (item) => {
  if (!queryEditorRef.value) return

  const start = queryEditorRef.value.selectionStart
  const value = queryStatement.value

  // 找到当前单词的起始位置
  let wordStart = start - 1
  while (wordStart >= 0 && /[a-zA-Z_]/.test(value[wordStart])) {
    wordStart--
  }
  wordStart = Math.max(0, wordStart + 1)

  // 替换单词
  queryStatement.value = value.substring(0, wordStart) + item + value.substring(start)

  // 设置光标位置
  nextTick(() => {
    queryEditorRef.value.selectionStart = queryEditorRef.value.selectionEnd = wordStart + item.length
    queryEditorRef.value.focus()
    showAutocomplete.value = false
    highlightSyntax()
  })
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  // 如果是今天内，显示时间
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  // 否则显示日期
  return date.toLocaleDateString()
}

const onInput = () => {
  highlightSyntax()
  // 触发自动补全
  if (props.enableAutocomplete) {
    emit('input', queryStatement.value)
  }
}

const onKeydown = (event) => {
  handleKeydown(event)
}

// 暴露方法给父组件
defineExpose({
  saveToQueryHistory,
  loadQueryHistory,
  highlightSyntax,
  queryEditorRef
})
</script>

<style scoped>
.query-editor {
  width: 100%;
}

.section {
  margin-bottom: 24px;
}

.section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.editor-section {
  flex: 1;
}

.query-editor-container {
  position: relative;
}

.code-editor-wrapper {
  position: relative;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-bg-color);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.code-editor-wrapper:focus-within {
  border-color: var(--el-color-primary);
  outline: 0;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.code-editor-wrapper.with-header {
  border-radius: 4px;
}

.code-editor {
  width: 100%;
  height: v-bind(editorHeight);
  padding: 8px 12px;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: transparent;
  caret-color: var(--el-text-color-primary);
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  z-index: 2;
  position: relative;
}

.code-highlight {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 8px 12px;
  white-space: pre-wrap;
  word-wrap: break-word;
  pointer-events: none;
  z-index: 1;
  color: var(--el-text-color-primary);
}

/* 语法高亮样式 */
:deep(.keyword) {
  color: #d73a49;
  font-weight: bold;
}

:deep(.function) {
  color: #6f42c1;
  font-weight: bold;
}

:deep(.string) {
  color: #032f62;
}

:deep(.number) {
  color: #005cc5;
}

:deep(.variable) {
  color: #e36209;
  font-weight: bold;
}

:deep(.node) {
  color: #22863a;
}

:deep(.relationship) {
  color: #735c0f;
}

/* 自动补全样式 */
.autocomplete-popup {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  max-height: 200px;
  overflow-y: auto;
}

.autocomplete-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

.autocomplete-item:hover,
.autocomplete-item.active {
  background: var(--el-fill-color-light);
}

.autocomplete-item:last-child {
  border-bottom: none;
}

.editor-actions {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.editor-actions .el-button {
  flex: 1;
}

.history-list {
  background: var(--el-bg-color-page);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  overflow: hidden;
}

.history-item {
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.history-item:hover {
  background-color: var(--el-fill-color-light);
}

.history-item:last-child {
  border-bottom: none;
}

.history-content {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
  line-height: 1.4;
  word-break: break-word;
}

.history-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
}

.history-section {
  margin-top: 16px;
}
</style>
