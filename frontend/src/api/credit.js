import request from '@/utils/request'

/**
 * 获取信用积分信息
 * 后端: GET /credits
 */
export function getCreditInfo() {
  return request({
    url: '/credits',
    method: 'get'
  })
}

/**
 * 获取积分记录
 * 后端: GET /credits/logs
 * @param {Object} params - 查询参数 { current, size }
 */
export function getCreditRecords(params) {
  return request({
    url: '/credits/logs',
    method: 'get',
    params
  })
}

/**
 * 获取积分规则
 * 后端: GET /credits/rules
 */
export function getCreditRules() {
  return request({
    url: '/credits/rules',
    method: 'get'
  })
}

export function getCreditLevel(score) {
  return request({ url: '/credits/level', method: 'get', params: { score } })
}

export function getUserCredit(userId) {
  return request({ url: `/credits/${userId}`, method: 'get' })
}

export function getUserCreditLogs(userId, params) {
  return request({ url: `/credits/${userId}/logs`, method: 'get', params })
}
