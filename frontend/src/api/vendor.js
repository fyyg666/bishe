import request from '@/utils/request'

export function getVendorList(params) {
  return request({
    url: '/vendors',
    method: 'get',
    params
  })
}

export function getVendorDetail(id) {
  return request({
    url: `/vendors/${id}`,
    method: 'get'
  })
}

export function createVendor(data) {
  return request({
    url: '/vendors',
    method: 'post',
    data
  })
}

export function updateVendor(id, data) {
  return request({
    url: `/vendors/${id}`,
    method: 'put',
    data
  })
}

export function deleteVendor(id) {
  return request({
    url: `/vendors/${id}`,
    method: 'delete'
  })
}
