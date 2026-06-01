import request from '@/utils/request'

export function getSubscriptionList(params) {
  return request({
    url: '/serial/subscriptions',
    method: 'get',
    params
  })
}

export function getSubscriptionDetail(id) {
  return request({
    url: `/serial/subscriptions/${id}`,
    method: 'get'
  })
}

export function createSubscription(data) {
  return request({
    url: '/serial/subscriptions',
    method: 'post',
    data
  })
}

export function updateSubscription(id, data) {
  return request({
    url: `/serial/subscriptions/${id}`,
    method: 'put',
    data
  })
}

export function deleteSubscription(id) {
  return request({
    url: `/serial/subscriptions/${id}`,
    method: 'delete'
  })
}

export function generateExpectedIssues(id) {
  return request({
    url: `/serial/subscriptions/${id}/generate-issues`,
    method: 'post'
  })
}

export function getIssueList(params) {
  return request({
    url: '/serial/issues',
    method: 'get',
    params
  })
}

export function receiveIssue(id) {
  return request({
    url: `/serial/issues/${id}/receive`,
    method: 'post'
  })
}

export function markIssueMissing(id) {
  return request({
    url: `/serial/issues/${id}/missing`,
    method: 'post'
  })
}

export function checkOverdueIssues() {
  return request({
    url: '/serial/issues/check-overdue',
    method: 'post'
  })
}

export function listRoutings(params) {
  return request({
    url: '/serial-routings',
    method: 'get',
    params
  })
}

export function getRouting(id) {
  return request({
    url: `/serial-routings/${id}`,
    method: 'get'
  })
}

export function createRouting(data) {
  return request({
    url: '/serial-routings',
    method: 'post',
    data
  })
}

export function batchCreateRoutings(data) {
  return request({
    url: '/serial-routings/batch',
    method: 'post',
    data
  })
}

export function sendRouting(id) {
  return request({
    url: `/serial-routings/${id}/send`,
    method: 'put'
  })
}

export function deliverRouting(id, data) {
  return request({
    url: `/serial-routings/${id}/deliver`,
    method: 'put',
    data
  })
}

export function deleteRouting(id) {
  return request({
    url: `/serial-routings/${id}`,
    method: 'delete'
  })
}

export function listRoutingTemplates(params) {
  return request({
    url: '/serial-routings/templates',
    method: 'get',
    params
  })
}

export function createRoutingTemplate(data) {
  return request({
    url: '/serial-routings/templates',
    method: 'post',
    data
  })
}

export function deleteRoutingTemplate(id) {
  return request({
    url: `/serial-routings/templates/${id}`,
    method: 'delete'
  })
}

export function listClaims(params) {
  return request({
    url: '/serial/claims',
    method: 'get',
    params
  })
}

export function getClaim(id) {
  return request({
    url: `/serial/claims/${id}`,
    method: 'get'
  })
}

export function createClaim(data) {
  return request({
    url: '/serial/claims',
    method: 'post',
    data
  })
}

export function updateClaim(id, data) {
  return request({
    url: `/serial/claims/${id}`,
    method: 'put',
    data
  })
}

export function resolveClaim(id, data) {
  return request({
    url: `/serial/claims/${id}/resolve`,
    method: 'put',
    data
  })
}

export function closeClaim(id) {
  return request({
    url: `/serial/claims/${id}/close`,
    method: 'put'
  })
}
