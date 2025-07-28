import { defineStore } from 'pinia'

// 定义 Pinia Store
export const useTabsStore = defineStore('tabs', {
    // state：存储数据
    state: () => ({
        // 初始化 tabs，默认包含首页，且首页不可关闭
        tabs: [{ title: '首页', path: '/home', closable: false }],

        // 当前激活的标签路径，默认激活首页
        activeTab: '/home'
    }),

    // actions：定义修改 state 的方法
    actions: {
        // 添加标签
        addTab(tab) {
            // 先检查这个标签是否已存在
            const exists = this.tabs.find(t => t.path === tab.path)
            if (!exists) {
                // 设置是否可关闭，首页不能被关闭
                tab.closable = tab.path === '/home' ? false : true
                this.tabs.push(tab) // 添加到 tabs 列表
            }
            // 设置当前激活的标签
            this.activeTab = tab.path
        },

        // 删除标签
        removeTab(path) {
            if (path === '/home') return // 首页不能被删除

            const index = this.tabs.findIndex(t => t.path === path)
            if (index > -1) {
                this.tabs.splice(index, 1) // 从 tabs 中移除

                // 如果删除的是当前激活的标签，调整 activeTab
                if (this.activeTab === path) {
                    // 如果被删除的标签后面还有标签，则激活下一个标签
                    if (index < this.tabs.length) {
                        this.activeTab = this.tabs[index].path
                    }
                    // 否则激活前一个标签
                    else if (index - 1 >= 0) {
                        this.activeTab = this.tabs[index - 1].path
                    }
                    // 如果删除后 tabs 为空，则回到首页
                    else {
                        this.activeTab = '/home'
                    }
                }
            }
        },

        // 设置当前激活的标签
        setActiveTab(path) {
            this.activeTab = path
        }
    },

    // Pinia 持久化存储，保证刷新页面后状态不会丢失
    persist: {
        key: 'tabsStore', // 存储的 key
        storage: localStorage, // 存储方式（localStorage 或 sessionStorage）
        paths: ['tabs', 'activeTab'] // 需要持久化的字段
    }
}) 