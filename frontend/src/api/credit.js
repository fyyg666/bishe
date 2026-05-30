import request from '@/utils/request'

/**
 * 获取信用积分信息
 * 后端: GET /credits
 */
export function getCreditInfo() {
  return request({
    url: '/credits',
    method: 'get'
  })
}

/**
 * 获取积分记录
 * 后端: GET /credits/logs
 * @param {Object} params - 查询参数 { current, size }
 */
export function getCreditRecords(params) {
  return request({
    url: '/credits/logs',
    method: 'get',
    params
  })
}

/**
 * 获取积分规则（前端硬编码，无后端接口）
 * 规则来源：论文§3.2(4)信用积分
 * 
 * 积分规则：
 * - 借书 +5分
 * - 按时还 +1分
 * - 提前还 +2分
 * - 签到 +1分
 * - 逾期 -5分/天
 * - 损坏 -50分
 * - 丢失 -100分
 * - 志愿服务 +10分/小时（上限50分）
 * 
 * 等级规则：
 * - 0-59: 普通
 * - 60-119: 铜牌
 * - 120-179: 银牌  
 * - 180-239: 金牌
 * - 240-300: 白金
 */
export function getCreditRules() {
  return Promise.resolve({
    code: 0,
    data: [
      { action: '借阅图书', score: 5, description: '每次借阅成功获得5积分' },
      { action: '按时归还', score: 1, description: '按时归还获得1积分' },
      { action: '提前归还', score: 2, description: '提前归还额外加1分，共2分' },
      { action: '逾期归还', score: -5, description: '每逾期一天扣5积分（单日扣分上限）' },
      { action: '损坏图书', score: -50, description: '损坏图书视情况扣50分' },
      { action: '丢失图书', score: -100, description: '丢失图书扣100积分' },
      { action: '签到奖励', score: 1, description: '座位签到奖励1积分' },
      { action: '志愿服务', score: 10, description: '每小时志愿服务奖励10积分（单次上限50）' }
    ]
  })
}
