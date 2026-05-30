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
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: {
      refreshToken: getRefreshToken()
    }
  })
}

export function changePassword(id, data) {
  return request({
    url: '/readers/' + id + '/password',
    method: 'post',
    data
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
