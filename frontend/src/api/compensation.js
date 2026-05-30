import request from '@/utils/request'

export function listCompensations(params) {
  return request({
    url: '/compensations',
    method: 'get',
    params
  })
}

export function getCompensation(id) {
  return request({
    url: `/compensations/${id}`,
    method: 'get'
  })
}

export function createCompensation(data) {
  return request({
    url: '/compensations',
    method: 'post',
    data
  })
}

export function processCashPayment(id, remark) {
  return request({
    url: `/compensations/${id}/pay/cash`,
    method: 'post',
    params: { remark }
  })
}

export function processCreditPayment(id, creditAmount, remark) {
  return request({
    url: `/compensations/${id}/pay/credit`,
    method: 'post',
    params: { creditAmount, remark }
  })
}

export function processVolunteerPayment(id, hours, remark) {
  return request({
    url: `/compensations/${id}/pay/volunteer`,
    method: 'post',
    params: { hours, remark }
  })
}

export function cancelCompensation(id, reason) {
  return request({
    url: `/compensations/${id}/cancel`,
    method: 'post',
    params: { reason }
  })
}
