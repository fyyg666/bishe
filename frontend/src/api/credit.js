import request from '@/utils/request'

/**
 * 获取信用积分信息
 */
export function getCreditInfo() {
  return request({
    url: '/credit/info',
    method: 'get'
  })
}

/**
 * 获取积分记录
 * @param {Object} params - 查询参数
 */
export function getCreditRecords(params) {
  return request({
    url: '/credit/records',
    method: 'get',
    params
  })
}

/**
 * 获取积分规则
 */
export function getCreditRules() {
  return request({
    url: '/credit/rules',
    method: 'get'
  })
}

/**
 * 获取积分统计
 */
export function getCreditStats() {
  return request({
    url: '/credit/stats',
    method: 'get'
  })
}

/**
 * 申诉积分
 * @param {Object} data - { recordId, reason }
 */
export function appealCredit(data) {
  return request({
    url: '/credit/appeal',
    method: 'post',
    data
  })
}
