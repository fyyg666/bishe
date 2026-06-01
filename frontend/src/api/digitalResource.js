import request from '@/utils/request'

export function getDigitalResourceList(params) {
  return request({
    url: '/digital-resources',
    method: 'get',
    params
  })
}

export function getDigitalResourceDetail(id) {
  return request({
    url: `/digital-resources/${id}`,
    method: 'get'
  })
}

export function createDigitalResource(data) {
  return request({
    url: '/digital-resources',
    method: 'post',
    data
  })
}

export function updateDigitalResource(id, data) {
  return request({
    url: `/digital-resources/${id}`,
    method: 'put',
    data
  })
}

export function deleteDigitalResource(id) {
  return request({
    url: `/digital-resources/${id}`,
    method: 'delete'
  })
}

export function searchDigitalResources(keyword) {
  return request({
    url: '/digital-resources/search',
    method: 'get',
    params: { keyword }
  })
}

export function unifiedSearch(params) {
  return request({
    url: '/search',
    method: 'get',
    params
  })
}

export function incrementViewCount(id) {
  return request({
    url: `/digital-resources/${id}/view`,
    method: 'put'
  })
}

export function getDownloadUrl(id) {
  return request({
    url: `/digital-resources/${id}/download`,
    method: 'get'
  })
}
