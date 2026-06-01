import request from '@/utils/request'

/**
 * 获取志愿服务列表
 * @param {Object} params - 查询参数
 */
export function getVolunteerList(params) {
  return request({
    url: '/volunteers',
    method: 'get',
    params
  })
}

/**
 * 获取我的志愿服务记录
 * @param {Object} params - 查询参数
 */
export function getMyVolunteers(params) {
  return request({
    url: '/volunteers/my',
    method: 'get',
    params
  })
}

/**
 * 获取志愿服务详情
 * @param {Number} id - 记录ID
 */
export function getVolunteerDetail(id) {
  return request({
    url: `/volunteers/${id}`,
    method: 'get'
  })
}

/**
 * 申请志愿服务
 * @param {Object} data - 志愿服务数据
 */
export function createVolunteer(data) {
  return request({
    url: '/volunteers',
    method: 'post',
    data
  })
}

/**
 * 更新志愿服务记录
 * @param {Number} id - 记录ID
 * @param {Object} data - 志愿服务数据
 */
export function updateVolunteer(id, data) {
  return request({
    url: `/volunteers/${id}`,
    method: 'put',
    data
  })
}

/**
 * 取消志愿服务申请
 * @param {Number} id - 记录ID
 */
export function cancelVolunteer(id) {
  return request({
    url: `/volunteers/${id}/cancel`,
    method: 'post'
  })
}

/**
 * 审核志愿服务
 * @param {Number} id - 记录ID
 * @param {Boolean} approved - 是否通过
 * @param {String} remark - 审核备注
 */
export function reviewVolunteer(id, approved, remark) {
  return request({
    url: `/volunteers/${id}/review`,
    method: 'post',
    data: { approved, remark }
  })
}

/**
 * 获取待审核志愿服务列表
 * @param {Object} params - 查询参数
 */
export function getPendingVolunteers(params) {
  return request({
    url: '/volunteers/pending',
    method: 'get',
    params
  })
}

/**
 * 删除志愿服务记录
 * @param {Number} id - 记录ID
 */
export function deleteVolunteer(id) {
  return request({
    url: `/volunteers/${id}`,
    method: 'delete'
  })
}

/**
 * 获取志愿服务统计
 */
export function getVolunteerStats() {
  return request({
    url: '/volunteers/stats',
    method: 'get'
  })
}
