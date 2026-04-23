import request from '@/utils/request'

/**
 * 获取综合统计概览
 */
export function getStatisticsOverview() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

/**
 * 获取借阅统计
 */
export function getBorrowStatistics() {
  return request({
    url: '/statistics/borrows',
    method: 'get'
  })
}

/**
 * 获取图书统计
 */
export function getBookStatistics() {
  return request({
    url: '/statistics/books',
    method: 'get'
  })
}

/**
 * 获取读者统计
 */
export function getReaderStatistics() {
  return request({
    url: '/statistics/readers',
    method: 'get'
  })
}

/**
 * 获取座位统计
 */
export function getSeatStatistics() {
  return request({
    url: '/statistics/seats',
    method: 'get'
  })
}

/**
 * 获取借阅趋势
 * @param {Number} days - 天数
 */
export function getBorrowTrend(days = 30) {
  return request({
    url: '/statistics/borrow-trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取热门图书排行
 * @param {Number} limit - 数量限制
 */
export function getHotBooks(limit = 10) {
  return request({
    url: '/statistics/hot-books',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取图书分类分布
 */
export function getCategoryDistribution() {
  return request({
    url: '/statistics/category-distribution',
    method: 'get'
  })
}

/**
 * 获取月度统计数据
 * @param {Number} months - 月份数
 */
export function getMonthlyStats(months = 12) {
  return request({
    url: '/statistics/monthly',
    method: 'get',
    params: { months }
  })
}
