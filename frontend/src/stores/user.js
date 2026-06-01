import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getUserInfo } from '@/api/auth'
import { getToken, setToken as setAuthToken, clearToken } from '@/utils/auth'

// FIXED: FE-001 - 所有Token操作从localStorage迁移到Cookie
export const useUserStore = defineStore('user', () => {
  // 状态 - 从Cookie读取Token，不再使用localStorage
  const token = ref(getToken() || '')
  const userInfo = ref(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => ['ADMIN', 'LIBRARIAN'].includes(userInfo.value?.role))
  const username = computed(() => userInfo.value?.username || '')
  const userRole = computed(() => userInfo.value?.role)

  // 方法
  // 防止并发fetch的锁
  const fetchingUserInfo = ref(false)

  function initUser() {
    if (token.value) {
      fetchUserInfo()
    }
  }

  async function fetchUserInfo() {
    if (fetchingUserInfo.value) return
    fetchingUserInfo.value = true
    try {
      const res = await getUserInfo()
      userInfo.value = res.data || res
    } catch (error) {
      console.error('获取用户信息失败:', error)
      if (!userInfo.value) {
        logout()
      }
    } finally {
      fetchingUserInfo.value = false
    }
  }

  async function login(loginForm) {
    const res = await apiLogin(loginForm)
    const newToken = res.accessToken || res.token || res.data?.accessToken || res.data?.token
    const refreshTokenValue = res.refreshToken || res.data?.refreshToken

    token.value = newToken
    setAuthToken(newToken, refreshTokenValue || undefined)

    // 获取用户信息
    userInfo.value = res.userInfo || res.data?.userInfo || res.data
    return res
  }

  async function logout() {
    try {
      await apiLogout()
    } catch {
      // 忽略登出API错误（本地状态仍需清除）
    }
    token.value = ''
    userInfo.value = null
    clearToken()
  }

  function setNewToken(newToken) {
    token.value = newToken
    setAuthToken(newToken)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    username,
    userRole,
    initUser,
    fetchUserInfo,
    login,
    logout,
    setToken: setNewToken,
    fetchingUserInfo
  }
})
