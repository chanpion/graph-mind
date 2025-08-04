import request from '@/utils/request'

// 认证API
export const authApi = {
  // 用户登录
  login(data) {
    return request.post('/api/auth/login', data)
  },

  // 用户登出
  logout() {
    return request.post('/api/auth/logout')
  }
}

export default authApi