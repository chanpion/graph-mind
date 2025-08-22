import { defineStore } from 'pinia'

export const useGraphStore = defineStore('graph', {
  state: () => ({
    // 当前选中的图
    currentGraph: null,
    // 图列表
    graphList: []
  }),

  actions: {
    // 设置当前图
    setCurrentGraph(graph) {
      this.currentGraph = graph
    },
    
    // 清除当前图
    clearCurrentGraph() {
      this.currentGraph = null
    },
    
    // 设置图列表
    setGraphList(graphList) {
      this.graphList = graphList
    },
    
    // 清除图列表
    clearGraphList() {
      this.graphList = []
    }
  }
})