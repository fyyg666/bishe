import request from '@/utils/request'

export function createSuggestion(data) {
  return request({
    url: '/purchase-suggestions',
    method: 'post',
    data
  })
}

export function listSuggestions(params) {
  return request({
    url: '/purchase-suggestions',
    method: 'get',
    params
  })
}

export function getMySuggestions(params) {
  return request({
    url: '/purchase-suggestions/my',
    method: 'get',
    params
  })
}

export function approveSuggestion(id, remark) {
  return request({
    url: `/purchase-suggestions/${id}/approve`,
    method: 'put',
    data: { remark }
  })
}

export function rejectSuggestion(id, remark) {
  return request({
    url: `/purchase-suggestions/${id}/reject`,
    method: 'put',
    data: { remark }
  })
}
