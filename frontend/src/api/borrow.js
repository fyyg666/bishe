import request from '@/utils/request'

/**
 * 获取借阅记录列表
 * @param {Object} params - 查询参数
 */
export function getBorrowList(params) {
  return request({
    url: '/borrows',
    method: 'get',
    params
  })
}

/**
 * 获取借阅详情
 * @param {Number|String} id - 借阅记录ID
 */
export function getBorrowDetail(id) {
  return request({
    url: `/borrows/${id}`,
    method: 'get'
  })
}

/**
 * 借书
 * @param {Object} data - { bookId, readerId, ... }
 */
export function borrowBook(data) {
  return request({
    url: '/borrows/borrow',
    method: 'post',
    data
  })
}

/**
 * 还书
 * @param {Number|String} id - 借阅记录ID
 */
export function returnBook(id) {
  return request({
    url: `/borrows/${id}/return`,
    method: 'put'
  })
}

/**
 * 续借
 * @param {Number|String} id - 借阅记录ID
 * @param {Object} data - { extendDays }
 */
export function renewBook(id, data) {
  return request({
    url: `/borrows/${id}/renew`,
    method: 'put',
    data
  })
}

/**
 * 获取我的借阅记录
 * @param {Object} params - 查询参数
 */
export function getMyBorrows(params) {
  return request({
    url: '/borrows/my',
    method: 'get',
    params
  })
}

/**
 * 获取借阅统计
 */
export function getBorrowStats() {
  return request({
    url: '/borrows/stats',
    method: 'get'
  })
}

/**
 * 取消借阅预约
 * @param {Number|String} id - 预约ID
 */
export function cancelReservation(id) {
  return request({
    url: `/borrows/reservation/${id}`,
    method: 'delete'
  })
}
