import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value.username || '')
  const nickname = computed(() => userInfo.value.nickname || '')
  const avatar = computed(() => userInfo.value.avatar || '')
  const roles = computed(() => userInfo.value.roles || [])

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function login(val, info) {
    setToken(val)
    setUserInfo(info)
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('rememberedUser')
  }

  function hasPermission(permission) {
    return roles.value.includes(permission)
  }

  function hasRole(role) {
    return roles.value.includes(role)
  }

  return {
    token, userInfo, isLoggedIn, username, nickname, avatar, roles,
    setUserInfo, setToken, login, logout, hasPermission, hasRole
  }
}, {
  persist: {
    key: 'authStore',
    storage: localStorage,
    paths: ['token', 'userInfo']
  }
})
