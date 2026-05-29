import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 多标签导航 Store
 * 从 frontend TabSystem 提取，移除特定视图的硬编码引用
 */
export const useTabStore = defineStore('tab', () => {
  const router = useRouter()

  const tabs = ref([])
  const activeTabId = ref('')
  const fixedTabIds = ref([])
  const maxTabs = ref(10)

  const allTabs = computed(() => tabs.value)
  const activeTab = computed(() => tabs.value.find(tab => tab.id === activeTabId.value))
  const closableTabs = computed(() => tabs.value.filter(tab => !fixedTabIds.value.includes(tab.id)))

  function addOrActivateTab(tabInfo) {
    const { id, title, path, icon = '', closable = true } = tabInfo
    const existingTab = tabs.value.find(tab => tab.id === id)

    if (existingTab) {
      activeTabId.value = id
      if (title !== existingTab.title || path !== existingTab.path) {
        existingTab.title = title
        existingTab.path = path
        existingTab.icon = icon
      }
      if (router.currentRoute.value.fullPath !== path) {
        router.push(path)
      }
    } else {
      const newTab = {
        id, title, path, icon,
        closable,
        createTime: Date.now(),
        lastActiveTime: Date.now()
      }
      tabs.value.push(newTab)

      // 超过最大数量，移除最早的可关闭标签页
      if (tabs.value.length > maxTabs.value) {
        const closable = tabs.value.filter(t =>
          t.closable && !fixedTabIds.value.includes(t.id)
        )
        if (closable.length > 0) {
          closable.sort((a, b) => a.createTime - b.createTime)
          const idx = tabs.value.findIndex(t => t.id === closable[0].id)
          if (idx !== -1) tabs.value.splice(idx, 1)
        }
      }

      activeTabId.value = id
      if (router.currentRoute.value.fullPath !== path) {
        router.push(path)
      }
    }
    saveTabs()
  }

  function closeTab(tabId) {
    if (fixedTabIds.value.includes(tabId)) return false
    const tabIndex = tabs.value.findIndex(tab => tab.id === tabId)
    if (tabIndex === -1) return false

    if (activeTabId.value === tabId) {
      if (tabIndex > 0) {
        activeTabId.value = tabs.value[tabIndex - 1].id
        router.push(tabs.value[tabIndex - 1].path)
      } else if (tabs.value.length > 1) {
        activeTabId.value = tabs.value[1].id
        router.push(tabs.value[1].path)
      } else {
        activeTabId.value = ''
        router.push('/home/dashboard')
      }
    }

    tabs.value.splice(tabIndex, 1)
    saveTabs()
    return true
  }

  function closeOtherTabs(tabId) {
    tabs.value = tabs.value.filter(t =>
      fixedTabIds.value.includes(t.id) || t.id === tabId
    )
    const tab = tabs.value.find(t => t.id === tabId)
    if (tab) {
      activeTabId.value = tabId
      router.push(tab.path)
    }
    saveTabs()
  }

  function closeAllTabs() {
    tabs.value = tabs.value.filter(t => fixedTabIds.value.includes(t.id))
    if (tabs.value.length > 0) {
      activeTabId.value = tabs.value[0].id
      router.push(tabs.value[0].path)
    } else {
      activeTabId.value = ''
      router.push('/home/dashboard')
    }
    saveTabs()
  }

  function activateTab(tabId) {
    const tab = tabs.value.find(tab => tab.id === tabId)
    if (tab) {
      activeTabId.value = tabId
      tab.lastActiveTime = Date.now()
      router.push(tab.path)
    }
  }

  function refreshTab(tabId) {
    const tab = tabs.value.find(tab => tab.id === tabId)
    if (tab) {
      router.push(tab.path)
    }
  }

  function updateTabTitle(tabId, newTitle) {
    const tab = tabs.value.find(tab => tab.id === tabId)
    if (tab) {
      tab.title = newTitle
      saveTabs()
    }
  }

  function saveTabs() {
    try {
      sessionStorage.setItem('tab_store', JSON.stringify({
        tabs: tabs.value,
        activeTabId: activeTabId.value,
        fixedTabIds: fixedTabIds.value
      }))
    } catch { /* ignore */ }
  }

  function loadTabs() {
    try {
      const stored = sessionStorage.getItem('tab_store')
      if (stored) {
        const { tabs: t, activeTabId: a, fixedTabIds: f } = JSON.parse(stored)
        if (Array.isArray(t)) tabs.value = t
        if (a) activeTabId.value = a
        if (Array.isArray(f)) fixedTabIds.value = f
      }
    } catch { /* ignore */ }
  }

  function initTabs() {
    loadTabs()
    const hasHome = tabs.value.some(t => t.id === 'home')
    if (!hasHome) {
      tabs.value.push({
        id: 'home',
        title: '首页',
        path: '/home/dashboard',
        icon: 'House',
        closable: false,
        createTime: Date.now(),
        lastActiveTime: Date.now()
      })
      if (!fixedTabIds.value.includes('home')) {
        fixedTabIds.value.push('home')
      }
    }
    if (!activeTabId.value && tabs.value.length > 0) {
      activeTabId.value = tabs.value[0].id
    }
  }

  initTabs()

  return {
    tabs, activeTabId, fixedTabIds,
    allTabs, activeTab, closableTabs,
    addOrActivateTab, closeTab, closeOtherTabs, closeAllTabs,
    activateTab, refreshTab, updateTabTitle,
    saveTabs, loadTabs, initTabs
  }
})
