import axios from 'axios'
import { ElMessage } from 'element-plus'

const uploadRequest = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {}
})

uploadRequest.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

uploadRequest.interceptors.response.use(
  response => {
    const { data } = response
    if (data.code === 200) {
      return data
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  error => {
    ElMessage.error(error.response?.data?.message || '文件上传失败')
    return Promise.reject(error)
  }
)

export default uploadRequest
