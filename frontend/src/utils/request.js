import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, getCsrfToken, setCsrfToken, setToken, clearAuthCookies } from './auth'
import { refreshToken as apiRefreshToken } from '@/api/auth'
import router from '@/router'

// FIXED: FE-001 - 使用Cookie Token + CSRF Token替代localStorage
// FIXED: FE-003 - 使用环境变量替代硬编码baseURL
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 携带Cookie凭证，支持CSRF防护
})

// 请求拦截器 - 注入Authorization和CSRF Token
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // FIXED: FE-001 - 为非GET请求添加CSRF Token头
    const csrfToken = getCsrfToken()
    if (csrfToken && ['post', 'put', 'patch', 'delete'].includes(config.method?.toLowerCase())) {
      config.headers['X-CSRF-Token'] = csrfToken
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理错误和Token过期
let isRefreshing = false
let pendingRequests = []

request.interceptors.response.use(
  (response) => {
    const { data } = response

    // 从响应头提取并保存CSRF Token（如果后端返回）
    const csrfFromHeader = response.headers['x-csrf-token']
    if (csrfFromHeader) {
      setCsrfToken(csrfFromHeader)
    }

    // 标准响应格式: code: 0 = 成功, 200 = 兼容旧版
    if (data.code !== undefined) {
      if (data.code === 0 || data.code === 200) {
        return data
      } else {
        ElMessage.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message))
      }
    }

    return data
  },
  async (error) => {
    const { response, config } = error

    if (response) {
      switch (response.status) {
        case 401: {
          // Token过期 - 尝试刷新
          if (!isRefreshing) {
            isRefreshing = true
            try {
              const res = await apiRefreshToken()
              const newToken = res.data?.token || res.token
              setToken(newToken)

              // 重试所有挂起的请求
              pendingRequests.forEach(cb => cb(newToken))
              pendingRequests = []

              // 重试当前请求
              config.headers.Authorization = `Bearer ${newToken}`
              return request(config)
            } catch (refreshError) {
              // 刷新失败，清除认证信息并跳转登录
              // FIXED: P2-FE-03 - 刷新失败时清空pendingRequests，防止内存泄漏
              pendingRequests = []
              clearAuthCookies()
              ElMessage.error('登录已过期，请重新登录')
              router.push('/login')
              return Promise.reject(refreshError)
            } finally {
              isRefreshing = false
            }
          } else {
            // 正在刷新中，将请求加入队列
            return new Promise((resolve) => {
              pendingRequests.push((token) => {
                config.headers.Authorization = `Bearer ${token}`
                resolve(request(config))
              })
            })
          }
        }
        case 403:
          ElMessage.error('没有权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.message || '网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }

    return Promise.reject(error)
  }
)

export default request
