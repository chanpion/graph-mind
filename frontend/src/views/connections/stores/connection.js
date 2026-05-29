import { defineStore } from 'pinia'
import { ref } from 'vue'
import { connectionApi } from '../api/connection'

export const useConnectionStore = defineStore('connection', () => {
  const connections = ref([])
  const currentConnection = ref(null)

  async function fetchConnections() {
    try {
      const res = await connectionApi.list()
      connections.value = Array.isArray(res) ? res : (res?.data || [])
      return connections.value
    } catch (error) {
      console.error('获取连接列表失败:', error)
      throw error
    }
  }

  function setCurrentConnection(connection) {
    currentConnection.value = connection
  }

  function removeConnection(connectionId) {
    const index = connections.value.findIndex(c => c.id === connectionId)
    if (index !== -1) {
      connections.value.splice(index, 1)
    }
  }

  return {
    connections, currentConnection,
    fetchConnections, setCurrentConnection, removeConnection
  }
})
