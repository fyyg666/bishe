import request from '@/utils/request'

export function listBranches(params) {
  return request({
    url: '/branches',
    method: 'get',
    params
  })
}

export function getBranchTree() {
  return request({
    url: '/branches/tree',
    method: 'get'
  })
}

export function getBranch(id) {
  return request({
    url: `/branches/${id}`,
    method: 'get'
  })
}

export function createBranch(data) {
  return request({
    url: '/branches',
    method: 'post',
    data
  })
}

export function updateBranch(id, data) {
  return request({
    url: `/branches/${id}`,
    method: 'put',
    data
  })
}

export function deleteBranch(id) {
  return request({
    url: `/branches/${id}`,
    method: 'delete'
  })
}
