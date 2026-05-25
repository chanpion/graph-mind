/**
 * 日期格式化
 */
export function formatDate(date, fmt = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  const d = new Date(date)
  const o = {
    'YYYY': d.getFullYear(),
    'MM': String(d.getMonth() + 1).padStart(2, '0'),
    'DD': String(d.getDate()).padStart(2, '0'),
    'HH': String(d.getHours()).padStart(2, '0'),
    'mm': String(d.getMinutes()).padStart(2, '0'),
    'ss': String(d.getSeconds()).padStart(2, '0')
  }
  let result = fmt
  for (const [key, val] of Object.entries(o)) {
    result = result.replace(key, val)
  }
  return result
}

/**
 * 文件大小格式化
 */
export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(2)} ${units[i]}`
}

/**
 * 数字格式化
 */
export function formatNumber(num, decimals = 2) {
  if (num === undefined || num === null) return '0'
  return Number(num).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: decimals
  })
}
