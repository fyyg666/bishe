/**
 * FIXED: P2-FE-08 - 提取公共状态映射函数到composable
 * 统一借阅、座位、图书等模块的状态映射
 */

// ===== 借阅状态映射 =====
const borrowStatusTypeMap = {
  BORROWING: 'warning',
  BORROWED: 'warning',
  RETURNED: 'success',
  OVERDUE: 'danger',
  CANCELLED: 'info'
}

const borrowStatusTextMap = {
  BORROWING: '借阅中',
  BORROWED: '借阅中',
  RETURNED: '已归还',
  OVERDUE: '已逾期',
  CANCELLED: '已取消'
}

// ===== 座位预约状态映射 =====
const seatStatusTypeMap = {
  RESERVED: 'warning',
  ACTIVE: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  '待签到': 'warning',
  '使用中': 'success',
  '已完成': 'info',
  '已取消': 'danger'
}

const seatStatusTextMap = {
  RESERVED: '已预约',
  ACTIVE: '使用中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

// ===== 图书状态映射 =====
const bookStatusTypeMap = {
  AVAILABLE: 'success',
  BORROWED: 'warning',
  UNAVAILABLE: 'danger'
}

const bookStatusTextMap = {
  AVAILABLE: '可借',
  BORROWED: '已借出',
  UNAVAILABLE: '不可借'
}

// ===== 志愿服务状态映射 =====
const volunteerStatusTypeMap = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  COMPLETED: 'info'
}

const volunteerStatusTextMap = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  COMPLETED: '已完成'
}

/**
 * 获取状态对应的Element Plus Tag类型
 */
export function getStatusType(status, category = 'borrow') {
  const maps = {
    borrow: borrowStatusTypeMap,
    seat: seatStatusTypeMap,
    book: bookStatusTypeMap,
    volunteer: volunteerStatusTypeMap
  }
  return maps[category]?.[status] || 'info'
}

/**
 * 获取状态的中文文本
 */
export function getStatusText(status, category = 'borrow') {
  const maps = {
    borrow: borrowStatusTextMap,
    seat: seatStatusTextMap,
    book: bookStatusTextMap,
    volunteer: volunteerStatusTextMap
  }
  return maps[category]?.[status] || status
}

/**
 * useStatusMap composable
 * @param {string} category - 状态类别: borrow, seat, book, volunteer
 */
export function useStatusMap(category = 'borrow') {
  return {
    getStatusType: (status) => getStatusType(status, category),
    getStatusText: (status) => getStatusText(status, category)
  }
}

export default useStatusMap
