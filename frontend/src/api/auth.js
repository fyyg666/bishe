import { getRefreshToken } from '@/utils/auth'
import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

export function refreshToken() {
  const refresh = getRefreshToken()
  if (!refresh) {
    return Promise.reject(new Error('Refresh token not available'))
  }
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: {
      refreshToken: refresh
    }
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function getCaptcha() {
  return request({
    url: '/captcha',
    method: 'get'
  })
}
