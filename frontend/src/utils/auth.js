import Cookies from 'js-cookie'

// FIXED: FE-001 - Token存储从localStorage改用Cookie，增强安全性
const TOKEN_KEY = 'library_token'
const REFRESH_TOKEN_KEY = 'library_refresh_token'
const CSRF_TOKEN_KEY = 'library_csrf_token'

// FIXED: P2-FE-04 - Cookie过期时间与后端JWT一致
// Access Token 后端2小时，Cookie设置2小时过期
const COOKIE_OPTIONS = {
  expires: 2 / 24, // 2小时（以天为单位）
  secure: window.location.protocol === 'https:',
  sameSite: 'Lax',
  path: '/'
}

// FIXED: P2-FE-04 - Refresh Token 后端7天，Cookie设置7天过期
const REFRESH_COOKIE_OPTIONS = {
  expires: 7, // 7天
  secure: window.location.protocol === 'https:',
  sameSite: 'Lax',
  path: '/'
}

/**
 * 获取访问Token
 * @returns {string|undefined}
 */
export function getToken() {
  return Cookies.get(TOKEN_KEY)
}

/**
 * 设置访问Token
 * @param {string} token
 */
export function setToken(token) {
  Cookies.set(TOKEN_KEY, token, COOKIE_OPTIONS)
}

/**
 * 移除访问Token
 */
export function removeToken() {
  // FIXED: P3-FE-03 - 删除Cookie时传入secure选项确保HTTPS下正确删除
  Cookies.remove(TOKEN_KEY, { 
    secure: window.location.protocol === 'https:',
    sameSite: 'Lax',
    path: '/' 
  })
}

/**
 * 获取刷新Token
 * @returns {string|undefined}
 */
export function getRefreshToken() {
  return Cookies.get(REFRESH_TOKEN_KEY)
}

/**
 * 设置刷新Token
 * @param {string} token
 */
export function setRefreshToken(token) {
  Cookies.set(REFRESH_TOKEN_KEY, token, REFRESH_COOKIE_OPTIONS)
}

/**
 * 移除刷新Token
 */
export function removeRefreshToken() {
  // FIXED: P3-FE-03 - 删除Cookie时传入secure选项
  Cookies.remove(REFRESH_TOKEN_KEY, { 
    secure: window.location.protocol === 'https:',
    sameSite: 'Lax',
    path: '/' 
  })
}

/**
 * 获取CSRF Token
 * FIXED: FE-001 - 添加CSRF Token支持
 * @returns {string|undefined}
 */
export function getCsrfToken() {
  return Cookies.get(CSRF_TOKEN_KEY)
}

/**
 * 设置CSRF Token
 * @param {string} token
 */
export function setCsrfToken(token) {
  Cookies.set(CSRF_TOKEN_KEY, token, { 
    secure: window.location.protocol === 'https:',
    sameSite: 'Lax',
    path: '/'
  })
}

/**
 * 移除CSRF Token
 */
export function removeCsrfToken() {
  // FIXED: P3-FE-03 - 删除Cookie时传入secure选项
  Cookies.remove(CSRF_TOKEN_KEY, { 
    secure: window.location.protocol === 'https:',
    sameSite: 'Lax',
    path: '/' 
  })
}

/**
 * 清除所有认证相关的Cookie
 */
export function clearAuthCookies() {
  removeToken()
  removeRefreshToken()
  removeCsrfToken()
}
