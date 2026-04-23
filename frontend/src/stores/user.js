import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getUserInfo } from '@/api/auth'
import { getToken, setToken as setCookieToken, removeToken, setRefreshToken, removeRefreshToken, clearAuthCookies } from '@/utils/auth'

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
      logout()
    }
  }

  async function login(loginForm) {
    try {
      const res = await apiLogin(loginForm)
      const newToken = res.token || res.data?.token
      const refreshToken = res.refreshToken || res.data?.refreshToken

      token.value = newToken
      setCookieToken(newToken)

      // 保存刷新Token到Cookie
      if (refreshToken) {
        setRefreshToken(refreshToken)
      }

      // 获取用户信息
      userInfo.value = res.data?.user || res.data
      return res
    } catch (error) {
      throw error
    }
  }

  function logout() {
    try {
      apiLogout()
    } catch (e) {
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
