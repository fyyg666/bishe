import request from '@/utils/request'

export function listTemplates(category) {
  return request({
    url: '/reports/templates',
    method: 'get',
    params: category ? { category } : {}
  })
}

export function getTemplate(id) {
  return request({
    url: `/reports/templates/${id}`,
    method: 'get'
  })
}

export function createTemplate(data) {
  return request({
    url: '/reports/templates',
    method: 'post',
    data
  })
}

export function updateTemplate(id, data) {
  return request({
    url: `/reports/templates/${id}`,
    method: 'put',
    data
  })
}

export function deleteTemplate(id) {
  return request({
    url: `/reports/templates/${id}`,
    method: 'delete'
  })
}

export function executeTemplate(id, params = {}) {
  return request({
    url: `/reports/templates/${id}/execute`,
    method: 'post',
    data: params
  })
}

export function getExportUrl(id, format = 'excel') {
  return `/reports/templates/${id}/export?format=${format}`
}
