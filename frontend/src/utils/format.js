/**
 * 格式化日期工具函数
 */
export function formatDate(date, fmt = 'YYYY-MM-DD') {
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  const map = {
    'YYYY': d.getFullYear(),
    'MM': String(d.getMonth() + 1).padStart(2, '0'),
    'DD': String(d.getDate()).padStart(2, '0'),
    'HH': String(d.getHours()).padStart(2, '0'),
    'mm': String(d.getMinutes()).padStart(2, '0'),
    'ss': String(d.getSeconds()).padStart(2, '0'),
  }
  let result = fmt
  Object.entries(map).forEach(([key, val]) => {
    result = result.replace(key, val)
  })
  return result
}

/**
 * 获取分页参数
 */
export function getPaginationParams(page, size) {
  return {
    current: page || 1,
    size: size || 10,
  }
}
