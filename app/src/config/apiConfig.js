/**
 * API 配置管理
 * 用于控制 Mock 模式和真实 API 模式的切换
 */
const CONFIG_KEY = 'graph_mind_api_config'

const defaultConfig = {
  useMock: false,
  mockDelay: 300,
  realApiUrl: 'http://localhost:18080',
  realApiTimeout: 10000
}

export function getApiConfig() {
  try {
    const savedConfig = localStorage.getItem(CONFIG_KEY)
    if (savedConfig) {
      return { ...defaultConfig, ...JSON.parse(savedConfig) }
    }
  } catch {
    console.warn('读取 API 配置失败，使用默认配置')
  }
  return { ...defaultConfig }
}

export function setApiConfig(config) {
  const newConfig = { ...getApiConfig(), ...config }
  localStorage.setItem(CONFIG_KEY, JSON.stringify(newConfig))
  return newConfig
}

export function toggleApiMode(useMock) {
  return setApiConfig({ useMock })
}

export function resetApiConfig() {
  localStorage.removeItem(CONFIG_KEY)
}

export function isMockMode() {
  return getApiConfig().useMock
}

export { defaultConfig }
