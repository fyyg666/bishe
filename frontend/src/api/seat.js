import request from '@/utils/request'

/**
 * 获取座位列表（后端 GET /seats）
 * @param {String} area - 区域（可选）
 * @param {String} date - 日期（可选，默认今天）
 */
export function getSeatMap(area, date) {
  const params = {}
  if (area) params.area = area
  if (date) params.date = date
  return request({
    url: '/seats',
    method: 'get',
    params
  })
}

/**
 * 获取座位详情
 * 后端: GET /seats/{id}
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
 * 后端: POST /seats/reserve
 * @param {Object} data - { seatNumber, area, reservationDate, startTime, endTime }
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
 * 后端: POST /seats/cancel/{reservationId}
 * @param {Number|String} id - 预约ID
 */
export function cancelReserve(id) {
  return request({
    url: `/seats/cancel/${id}`,
    method: 'post'
  })
}

/**
 * 获取我的预约
 * 后端: GET /seats/my
 * @param {Object} params - 查询参数 { current, size }
 */
export function getMyReservations(params) {
  return request({
    url: '/seats/my',
    method: 'get',
    params
  })
}

/**
 * 签到
 * 后端: POST /seats/checkin/{reservationId}
 * @param {Number|String} id - 预约ID
 */
export function checkIn(id) {
  return request({
    url: `/seats/checkin/${id}`,
    method: 'post'
  })
}

/**
 * 签退
 * 后端: POST /seats/checkout/{reservationId}
 * @param {Number|String} id - 预约ID
 */
export function checkOut(id) {
  return request({
    url: `/seats/checkout/${id}`,
    method: 'post'
  })
}

/**
 * 检查时间段是否可用
 * 后端: GET /seats/check-availability
 * @param {String} seatNumber - 座位编号
 * @param {String} date - 日期
 * @param {String} startTime - 开始时间
 * @param {String} endTime - 结束时间
 */
export function checkAvailability(seatNumber, date, startTime, endTime) {
  return request({
    url: '/seats/check-availability',
    method: 'get',
    params: { seatNumber, date, startTime, endTime }
  })
}

export function getReadingRooms() {
  return request({ url: '/seats/reading-rooms', method: 'get' })
}
