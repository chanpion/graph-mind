import request from '@/api/request'

export const connectionApi = {
  list(params) {
    return request.get('/api/connections', { params })
  },

  create(data) {
    return request.post('/api/connections', data)
  },

  update(id, data) {
    return request.put(`/api/connections/${id}`, data)
  },

  delete(id) {
    return request.delete(`/api/connections/${id}`)
  },

  testConnection(id) {
    return request.post(`/api/connections/${id}/test`)
  },

  connect(id) {
    return request.post(`/api/connections/${id}/connect`)
  },

  disconnect(id) {
    return request.post(`/api/connections/${id}/disconnect`)
  }
}

export default connectionApi
