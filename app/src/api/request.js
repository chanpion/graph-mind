import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getApiConfig } from '@/config/apiConfig'

// Mock handler - 延迟加载，避免循环依赖
let mockHandler = null

const config = getApiConfig()

// 创建真实请求实例（API 文件已在 URL 中包含 /api 前缀）
const axiosRequest = axios.create({
  timeout: config.realApiTimeout || 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
axiosRequest.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
axiosRequest.interceptors.response.use(
  response => {
    const { data } = response
    if (data.code === 200) {
      return data
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
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
)

// 响应处理器（Mock 模式使用）
const responseHandler = (response) => {
  const result = response.data || response
  if (result && typeof result === 'object' && 'code' in result) {
    if (result.code === 200) {
      return result.data
    }
    const message = result.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
  return result
}

// 动态加载 mock handler
async function getMockHandler() {
  if (!mockHandler) {
    try {
      const module = await import('@/mock')
      mockHandler = module.default || module.mockHandler
    } catch {
      mockHandler = () => Promise.reject(new Error('[Mock] handler not available'))
    }
  }
  return mockHandler
}

// 请求取消相关
const pendingRequests = new Map()

/**
 * 生成请求唯一key
 * @param {Object} config - axios config
 * @returns {string} 请求key
 */
function generateRequestKey(config) {
  const { method, url, params, data } = config
  return `${method || 'get'}:${url}:${JSON.stringify(params)}:${JSON.stringify(data)}`
}

/**
 * 取消指定key的待处理请求
 * @param {string} key - 请求key
 * @param {string} reason - 取消原因
 */
function cancelPendingRequest(key, reason = '重复的请求已取消') {
  if (pendingRequests.has(key)) {
    const controller = pendingRequests.get(key)
    controller.abort(reason)
    pendingRequests.delete(key)
  }
}

/**
 * 统一请求函数
 * 根据配置决定使用 Mock 还是真实 API
 */
const request = async (config) => {
  const currentConfig = getApiConfig()

  if (currentConfig.useMock) {
    const handler = await getMockHandler()
    const mockResponse = await handler(config)
    // 保持与真实API一致的 {code, data, message} 格式
    return mockResponse
  }

  // 真实API请求 - 添加请求取消机制
  const requestKey = generateRequestKey(config)
  
  // 取消重复的待处理请求
  cancelPendingRequest(requestKey, `重复的请求已取消: ${config.url}`)
  
  // 创建AbortController用于取消请求
  const controller = new AbortController()
  config.signal = controller.signal
  
  // 存储到pending map
  pendingRequests.set(requestKey, controller)
  
  try {
    const response = await axiosRequest(config)
    // 请求成功后从pending map中移除
    pendingRequests.delete(requestKey)
    return response
  } catch (error) {
    // 请求失败后也从pending map中移除
    pendingRequests.delete(requestKey)
    // 如果是取消请求的错误，不抛出（已经是重复请求，静默处理）
    if (error.name === 'AbortError' || error.code === 'ABORT_ERR') {
      console.log(`请求已取消: ${error.message}`)
      // 返回一个永不resolve的promise，让调用方不处理（或抛出特定错误）
      return new Promise(() => {}) // 静默处理，不触发catch
    }
    throw error
  }
}

// 绑定 HTTP 方法，方便调用
request.get = (url, config) => request({ ...config, method: 'get', url })
request.post = (url, data, config) => request({ ...config, method: 'post', url, data })
request.put = (url, data, config) => request({ ...config, method: 'put', url, data })
request.delete = (url, config) => request({ ...config, method: 'delete', url })

// 导出取消请求相关的工具函数（供外部使用）
export function cancelAllPendingRequests() {
  pendingRequests.forEach((controller, key) => {
    controller.abort('手动取消所有待处理请求')
  })
  pendingRequests.clear()
}

export function getPendingRequestCount() {
  return pendingRequests.size
}

export default request
export { axiosRequest }
