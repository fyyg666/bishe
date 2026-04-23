import request from '@/utils/request'

/**
 * 获取读者列表
 * @param {Object} params - 查询参数
 */
export function getReaderList(params) {
  return request({
    url: '/readers',
    method: 'get',
    params
  })
}

/**
 * 获取读者详情
 * @param {Number} id - 读者ID
 */
export function getReaderDetail(id) {
  return request({
    url: `/readers/${id}`,
    method: 'get'
  })
}

/**
 * 获取当前登录读者信息
 */
export function getCurrentReader() {
  return request({
    url: '/readers/me',
    method: 'get'
  })
}

/**
 * 注册新读者
 * @param {Object} data - 注册数据
 */
export function registerReader(data) {
  return request({
    url: '/readers',
    method: 'post',
    data
  })
}

/**
 * 更新读者信息
 * @param {Number} id - 读者ID
 * @param {Object} data - 更新数据
 */
export function updateReader(id, data) {
  return request({
    url: `/readers/${id}`,
    method: 'put',
    data
  })
}

/**
 * 修改密码
 * @param {Number} id - 读者ID
 * @param {Object} data - 包含旧密码和新密码
 */
export function changePassword(id, data) {
  return request({
    url: `/readers/${id}/password`,
    method: 'post',
    data
  })
}

/**
 * 删除读者
 * @param {Number} id - 读者ID
 */
export function deleteReader(id) {
  return request({
    url: `/readers/${id}`,
    method: 'delete'
  })
}

/**
 * 重置读者密码
 * @param {Number} id - 读者ID
 */
export function resetPassword(id) {
  return request({
    url: `/readers/${id}/reset-password`,
    method: 'post'
  })
}

/**
 * 禁用/启用读者账户
 * @param {Number} id - 读者ID
 * @param {Boolean} disabled - 是否禁用
 */
export function updateReaderStatus(id, disabled) {
  return request({
    url: `/readers/${id}/status`,
    method: 'post',
    params: { disabled }
  })
}
