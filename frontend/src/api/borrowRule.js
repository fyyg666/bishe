import request from '@/utils/request'

export function getBorrowRules() {
  return request({ url: '/borrow-rules', method: 'get' })
}

export function getBorrowRuleByType(readerType, bookType) {
  return request({ url: '/borrow-rules/query', method: 'get', params: { readerType, bookType } })
}

export function createBorrowRule(data) {
  return request({ url: '/borrow-rules', method: 'post', data })
}

export function updateBorrowRule(id, data) {
  return request({ url: `/borrow-rules/${id}`, method: 'put', data })
}

export function deleteBorrowRule(id) {
  return request({ url: `/borrow-rules/${id}`, method: 'delete' })
}
