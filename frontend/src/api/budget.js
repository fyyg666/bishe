import request from '@/utils/request'

export function listFunds(params) {
  return request({
    url: '/budget-funds',
    method: 'get',
    params
  })
}

export function getFund(id) {
  return request({
    url: `/budget-funds/${id}`,
    method: 'get'
  })
}

export function createFund(data) {
  return request({
    url: '/budget-funds',
    method: 'post',
    data
  })
}

export function updateFund(id, data) {
  return request({
    url: `/budget-funds/${id}`,
    method: 'put',
    data
  })
}

export function deleteFund(id) {
  return request({
    url: `/budget-funds/${id}`,
    method: 'delete'
  })
}

export function allocateToOrder(fundId, orderId, amount) {
  return request({
    url: `/budget-funds/${fundId}/allocate`,
    method: 'post',
    params: { orderId, amount }
  })
}
