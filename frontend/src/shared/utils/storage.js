/**
 * localStorage 封装
 * 支持自动 JSON 序列化/反序列化
 */
const PREFIX = 'gm_'

export function getStorage(key) {
  try {
    const raw = localStorage.getItem(PREFIX + key)
    if (raw === null) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setStorage(key, value) {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value))
  } catch { /* ignore */ }
}

export function removeStorage(key) {
  try {
    localStorage.removeItem(PREFIX + key)
  } catch { /* ignore */ }
}

export function clearStorage() {
  try {
    const keys = Object.keys(localStorage).filter(k => k.startsWith(PREFIX))
    keys.forEach(k => localStorage.removeItem(k))
  } catch { /* ignore */ }
}
