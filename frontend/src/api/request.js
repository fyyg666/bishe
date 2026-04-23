import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, getCsrfToken, clearAuthCookies } from '@/utils/auth'
import router from '@/router'

// FIXED: FE-001 - 使用Cookie Token + CSRF Token替代localStorage
// FIXED: FE-003 - 使用环境变量替代硬编码baseURL
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 携带Cookie凭证，支持CSRF防护
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // FIXED: FE-001 - 从Cookie获取Token，不再使用localStorage
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // FIXED: FE-001 - 为非GET请求添加CSRF Token
    const csrfToken = getCsrfToken()
    if (csrfToken && ['post', 'put', 'patch', 'delete'].includes(config.method?.toLowerCase())) {
      config.headers['X-CSRF-Token'] = csrfToken
    }

    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
// FIXED: FE-P2-01 - 统一code=0为成功，其他为失败
service.interceptors.response.use(
  response => {
    const res = response.data
    // 统一成功判断：code === 0 表示成功
    if (res.code === 0) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // FIXED: FE-001 - 使用clearAuthCookies替代localStorage.removeItem
          ElMessage.error('登录已过期，请重新登录')
          clearAuthCookies()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
