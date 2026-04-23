import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo as fetchUserInfo } from '@/api/auth'
import { setToken, removeToken, getToken, setRefreshToken, clearAuthCookies } from '@/utils/auth'

// FIXED: FE-001 - 所有Token操作从localStorage迁移到Cookie
export const useUserStore = defineStore('user', () => {
  // State - 从Cookie读取Token
  const token = ref(getToken())
  const userInfo = ref(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isLibrarian = computed(() => ['ADMIN', 'LIBRARIAN'].includes(userInfo.value?.role))

  // Actions
  const setUserToken = (newToken, refreshToken) => {
    token.value = newToken
    setToken(newToken)
    if (refreshToken) {
      setRefreshToken(refreshToken)
    }
  }

  const loginAction = async (credentials) => {
    const { data } = await login(credentials)
    setUserToken(data.token, data.refreshToken)
    userInfo.value = data.userInfo
    return data
  }

  const getUserInfo = async () => {
    const { data } = await fetchUserInfo()
    userInfo.value = data
    return data
  }

  const logout = () => {
    token.value = null
    userInfo.value = null
    // FIXED: FE-001 - 使用clearAuthCookies替代localStorage清除
    clearAuthCookies()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userRole,
    isAdmin,
    isLibrarian,
    loginAction,
    getUserInfo,
    logout,
    setUserToken
  }
})
