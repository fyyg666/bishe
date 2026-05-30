import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getUserInfo } from '@/api/auth'
import { getToken, setToken as setCookieToken, setRefreshToken, clearAuthCookies } from '@/utils/auth'

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
  function initUser() {
    if (token.value) {
      fetchUserInfo()
    }
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data || res
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 如果userInfo已经在登录时设置，不强制登出
      if (!userInfo.value) {
        logout()
      }
    }
  }

  async function login(loginForm) {
    try {
      const res = await apiLogin(loginForm)
      const newToken = res.accessToken || res.token || res.data?.accessToken || res.data?.token
      const refreshTokenValue = res.refreshToken || res.data?.refreshToken

      token.value = newToken
      setCookieToken(newToken)

      // 保存刷新Token到Cookie
      if (refreshTokenValue) {
        setRefreshToken(refreshTokenValue)
      }

      // 获取用户信息
      userInfo.value = res.userInfo || res.data?.userInfo || res.data
      return res
    } catch (error) {
      throw error
    }
  }

  function logout() {
    try {
      apiLogout()
    } catch {
      // 忽略错误
    }
    token.value = ''
    userInfo.value = null
    // FIXED: FE-001 - 使用clearAuthCookies替代localStorage.removeItem
    clearAuthCookies()
  }

  function setNewToken(newToken) {
    token.value = newToken
    setCookieToken(newToken)
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
    setToken: setNewToken
  }
})
