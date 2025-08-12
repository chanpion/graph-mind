import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 创建用于文件上传的axios实例
const uploadRequest = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000, // 文件上传可能需要更长时间
  // 不设置Content-Type，让浏览器根据FormData自动设置（包括boundary）
  headers: {}
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 添加token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 文件上传请求拦截器
uploadRequest.interceptors.request.use(
  config => {
    // 添加token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
const responseHandler = (response) => {
  const { data } = response

  // 如果响应成功
  if (data.code === 200) {
    return data
  }

  // 处理业务错误
  ElMessage.error(data.message || '请求失败')
  return Promise.reject(new Error(data.message || '请求失败'))
}

// 错误处理
const errorHandler = (error) => {
  console.error('响应错误:', error)

  // 处理网络错误
  if (error.response) {
    const { status, data } = error.response
    switch (status) {
      case 401:
        ElMessage.error('未授权，请重新登录')
        // 清除token并跳转到登录页
        localStorage.removeItem('token')
        window.location.href = '/login'
        break
      case 403:
        ElMessage.error('拒绝访问')
        break
      case 404:
        ElMessage.error('请求的资源不存在')
        break
      case 500:
        ElMessage.error('服务器内部错误')
        break
      default:
        ElMessage.error(data?.message || '网络错误')
    }
  } else if (error.request) {
    ElMessage.error('网络连接失败')
  } else {
    ElMessage.error('请求配置错误')
  }

  return Promise.reject(error)
}

request.interceptors.response.use(responseHandler, errorHandler)
uploadRequest.interceptors.response.use(responseHandler, errorHandler)

// 导出两个实例
export { request, uploadRequest }

export default request