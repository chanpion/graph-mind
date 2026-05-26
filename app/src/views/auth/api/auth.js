import request from '@/api/request'

export const authApi = {
  login(data) {
    return request.post('/api/auth/login', data)
  },

  logout() {
    return request.post('/api/auth/logout')
  }
}

export default authApi
