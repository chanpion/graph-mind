import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    isLoggedIn: !!localStorage.getItem('token')
  }),

  getters: {
    // 获取用户名
    username: (state) => state.userInfo.username || '',
    
    // 获取昵称
    nickname: (state) => state.userInfo.nickname || '',
    
    // 获取头像
    avatar: (state) => state.userInfo.avatar || '',
    
    // 获取用户角色
    roles: (state) => state.userInfo.roles || []
  },

  actions: {
    // 设置用户信息
    setUserInfo(userInfo) {
      console.log('等了设置用户信息:', userInfo)
      this.userInfo = userInfo
      this.isLoggedIn = true
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    // 设置token
    setToken(token) {
      this.token = token
      this.isLoggedIn = true
      localStorage.setItem('token', token)
    },

    // 登录
    login(token, userInfo) {
      this.setToken(token)
      this.setUserInfo(userInfo)
    },

    // 退出登录
    logout() {
      this.token = ''
      this.userInfo = {}
      this.isLoggedIn = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('rememberedUser')
    },

    // 检查是否有权限
    hasPermission(permission) {
      return this.roles.includes(permission)
    },

    // 检查是否有角色
    hasRole(role) {
      return this.roles.includes(role)
    }
  },

  persist: {
    key: 'userStore',
    storage: localStorage,
    paths: ['token', 'userInfo', 'isLoggedIn']
  }
})