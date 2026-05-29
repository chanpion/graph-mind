import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore(
  'app',
  () => {
  const sidebarCollapsed = ref(false)
  const theme = ref('light') // 'light' | 'dark'

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(val) {
    sidebarCollapsed.value = val
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    applyTheme(theme.value)
  }

  function setTheme(val) {
    theme.value = val
    applyTheme(val)
  }

  function applyTheme(val) {
    if (val === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark')
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.setAttribute('data-theme', 'light')
      document.documentElement.classList.remove('dark')
    }
  }

  // 初始化主题
  applyTheme(theme.value)

  return {
    sidebarCollapsed,
    theme,
    toggleSidebar,
    setSidebarCollapsed,
    toggleTheme,
    setTheme
  }
},
{
  persist: true
})
