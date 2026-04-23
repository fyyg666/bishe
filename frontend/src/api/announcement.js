import request from '@/utils/request'

/**
 * 获取公告列表
 * @param {Object} params - 查询参数
 */
export function getAnnouncementList(params) {
  return request({
    url: '/announcements',
    method: 'get',
    params
  })
}

/**
 * 获取公告详情
 * @param {Number} id - 公告ID
 */
export function getAnnouncementDetail(id) {
  return request({
    url: `/announcements/${id}`,
    method: 'get'
  })
}

/**
 * 获取最新公告
 * @param {Number} limit - 数量限制
 */
export function getLatestAnnouncements(limit = 5) {
  return request({
    url: '/announcements/latest',
    method: 'get',
    params: { limit }
  })
}

/**
 * 创建公告
 * @param {Object} data - 公告数据
 */
export function createAnnouncement(data) {
  return request({
    url: '/announcements',
    method: 'post',
    data
  })
}

/**
 * 更新公告
 * @param {Number} id - 公告ID
 * @param {Object} data - 公告数据
 */
export function updateAnnouncement(id, data) {
  return request({
    url: `/announcements/${id}`,
    method: 'put',
    data
  })
}

/**
 * 发布公告
 * @param {Number} id - 公告ID
 */
export function publishAnnouncement(id) {
  return request({
    url: `/announcements/${id}/publish`,
    method: 'post'
  })
}

/**
 * 删除公告
 * @param {Number} id - 公告ID
 */
export function deleteAnnouncement(id) {
  return request({
    url: `/announcements/${id}`,
    method: 'delete'
  })
}
