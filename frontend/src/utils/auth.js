// FIXED: SEC-01 - 从Cookie存储改为内存变量存储，防止XSS窃取Token
// 历史：此前使用Cookie存储（js-cookie），Token可被JS读取易受XSS攻击
// 当前方案：内存变量存储，页面刷新后需重新登录
// 后续迭代：后端通过Set-Cookie HttpOnly头自动管理Token

const TOKEN_KEY = 'library_token'
const REFRESH_TOKEN_KEY = 'library_refresh_token'
const CSRF_TOKEN_KEY = 'library_csrf_token'

// 内存变量 —— XSS无法从模块作用域外部读取
let inMemoryToken = null
let inMemoryRefreshToken = null
let inMemoryCsrfToken = null

/**
 * 获取访问Token
 * @returns {string|null}
 */
export function getToken() {
  return inMemoryToken
}

/**
 * 设置访问Token
 * @param {string} token
 */
export function setToken(token) {
  inMemoryToken = token
}

/**
 * 移除访问Token
 */
export function removeToken() {
  inMemoryToken = null
}

/**
 * 获取刷新Token
 * @returns {string|null}
 */
export function getRefreshToken() {
  return inMemoryRefreshToken
}

/**
 * 设置刷新Token
 * @param {string} token
 */
export function setRefreshToken(token) {
  inMemoryRefreshToken = token
}

/**
 * 移除刷新Token
 */
export function removeRefreshToken() {
  inMemoryRefreshToken = null
}

/**
 * 获取CSRF Token
 * @returns {string|null}
 */
export function getCsrfToken() {
  return inMemoryCsrfToken
}

/**
 * 设置CSRF Token
 * @param {string} token
 */
export function setCsrfToken(token) {
  inMemoryCsrfToken = token
}

/**
 * 移除CSRF Token
 */
export function removeCsrfToken() {
  inMemoryCsrfToken = null
}

/**
 * 清除所有认证Token
 */
export function clearAuthCookies() {
  removeToken()
  removeRefreshToken()
  removeCsrfToken()
}
