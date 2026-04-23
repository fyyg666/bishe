import request from '@/utils/request'

/**
 * 获取座位地图
 * @param {String} area - 区域
 */
export function getSeatMap(area) {
  return request({
    url: '/seats/map',
    method: 'get',
    params: { area }
  })
}

/**
 * 获取座位详情
 * @param {Number|String} id - 座位ID
 */
export function getSeatDetail(id) {
  return request({
    url: `/seats/${id}`,
    method: 'get'
  })
}

/**
 * 预约座位
 * @param {Object} data - { seatId, date, startTime, endTime }
 */
export function reserveSeat(data) {
  return request({
    url: '/seats/reserve',
    method: 'post',
    data
  })
}

/**
 * 取消预约
 * @param {Number|String} id - 预约ID
 */
export function cancelReserve(id) {
  return request({
    url: `/seats/reserve/${id}`,
    method: 'delete'
  })
}

/**
 * 获取我的预约
 * @param {Object} params - 查询参数
 */
export function getMyReservations(params) {
  return request({
    url: '/seats/my-reservations',
    method: 'get',
    params
  })
}

/**
 * 签到
 * @param {Number|String} id - 预约ID
 */
export function checkIn(id) {
  return request({
    url: `/seats/reserve/${id}/check-in`,
    method: 'put'
  })
}

/**
 * 签退
 * @param {Number|String} id - 预约ID
 */
export function checkOut(id) {
  return request({
    url: `/seats/reserve/${id}/check-out`,
    method: 'put'
  })
}

/**
 * 获取座位区域列表
 */
export function getAreas() {
  return request({
    url: '/seats/areas',
    method: 'get'
  })
}
